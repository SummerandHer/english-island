package com.island.module.daily;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.ai.DeepSeekChatService;
import com.island.ai.LlmException;
import com.island.common.BusinessException;
import com.island.module.daily.dto.DailyAiEnrichmentResult;
import com.island.module.vocabulary.Vocabulary;
import com.island.module.vocabulary.mapper.VocabularyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAiParseService {

	public static final String AI_VERSION = "daily-enrich-v2";

	private static final Set<String> STOP_WORDS = Set.of(
			"the", "a", "an", "and", "or", "but", "if", "of", "to", "in", "on", "for", "with",
			"is", "are", "was", "were", "be", "been", "being", "it", "its", "this", "that",
			"these", "those", "as", "at", "by", "from", "not", "no", "yes", "do", "does",
			"did", "have", "has", "had", "will", "would", "can", "could", "should", "may",
			"might", "very", "just", "also", "than", "then", "so", "such", "into", "over",
			"about", "after", "before", "between", "through", "during", "without", "within",
			"their", "them", "they", "he", "she", "we", "you", "i", "my", "our", "your",
			"his", "her", "who", "which", "what", "when", "where", "how", "why", "there",
			"here", "more", "most", "some", "any", "all", "each", "other", "only", "own"
	);

	/**
	 * 按篇幅给出词汇配额：约 400 词 → 高频约 10；≥800 词 → 高频约 15+。
	 * 难词随篇幅略增。允许单词与原文中的词组/搭配。
	 */
	record VocabQuota(int cetMin, int cetTarget, int cetMax, int hardMin, int hardTarget, int hardMax) {
		static VocabQuota forWordCount(int wc) {
			if (wc >= 800) {
				return new VocabQuota(14, 16, 22, 7, 10, 14);
			}
			if (wc >= 550) {
				return new VocabQuota(12, 14, 18, 6, 8, 12);
			}
			if (wc >= 350) {
				return new VocabQuota(9, 11, 15, 5, 7, 10);
			}
			return new VocabQuota(8, 10, 14, 4, 6, 9);
		}

		String promptLine() {
			return "cetVocab: aim for about %d items (acceptable %d-%d); hardVocab: aim for about %d (acceptable %d-%d). Include both single words AND useful multi-word phrases/collocations that appear verbatim in the text."
					.formatted(cetTarget, cetMin, cetMax, hardTarget, hardMin, hardMax);
		}
	}

	private static final String SYSTEM_PROMPT = """
			You are the close-reading editor for ISLAND (四六级岛), a CET-4/CET-6 learning product for Chinese college students.
			Your job is NOT literary criticism. Extract exam-useful vocabulary and sentence patterns from the given rewritten article, and classify topic + difficulty.
			Hard rules:
			1. Use ONLY the given text. Never invent words, phrases, or sentences that do not appear.
			2. structures[].en MUST be copied verbatim from the article (punctuation allowed).
			3. cetVocab: high-frequency CET content words OR useful phrases/collocations (n./v./adj./adv./phrase). Avoid bare function words like the/is/very. Scale quantity with article length (see user message). Prefer items learners can reuse in exams and daily study.
			4. hardVocab: harder/academic words or low-frequency phrases from the text; note explains collocation or confusion in Chinese. Scale with length.
			5. Each cetVocab/hardVocab "word" field may be a single word OR a short phrase (2-4 words) that appears in the article.
			6. topic MUST be exactly one of: education, technology, environment, society_culture, economy_business, health, psychology, science.
			7. difficulty MUST be cet4 or cet6 (prefer cet6 if clause density / academic words are high).
			8. summaryZh: 80-120 Chinese characters, objective; no pep talk.
			9. Output pure JSON only, no markdown fences.
			""";

	private final DeepSeekChatService deepSeekChatService;
	private final ObjectMapper objectMapper;
	private final VocabularyMapper vocabularyMapper;

	/** content hash → last enrich epoch ms (simple debounce) */
	private final ConcurrentHashMap<String, Long> enrichDebounce = new ConcurrentHashMap<>();

	/**
	 * @deprecated use {@link #enrich(String, String)}
	 */
	@Deprecated
	public Map<String, Object> parse(String title, String contentEn, String topicHint, String difficulty) {
		DailyAiEnrichmentResult r = enrich(title, contentEn);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("gate", r.gate());
		out.put("warnings", r.warnings());
		out.put("error", r.error());
		out.put("aiVersion", r.aiVersion());
		out.put("summaryZh", r.summaryZh());
		out.put("topic", r.topic());
		out.put("difficulty", r.difficulty());
		out.put("slugSuggestion", r.slugSuggestion());
		out.put("wordCount", r.wordCount());
		out.put("coverHint", r.coverHint());
		out.put("cetVocabJson", r.cetVocabJson());
		out.put("hardVocabJson", r.hardVocabJson());
		out.put("structuresJson", r.structuresJson());
		out.put("aiRawJson", r.aiRawJson());
		out.put("cetVocab", r.cetVocab());
		out.put("hardVocab", r.hardVocab());
		out.put("structures", r.structures());
		return out;
	}

	public DailyAiEnrichmentResult enrich(String title, String contentEn) {
		if (!StringUtils.hasText(contentEn)) {
			throw new BusinessException(400, "正文不能为空");
		}
		String content = contentEn.trim();
		int wordCount = countWords(content);

		if (wordCount < 200) {
			throw new BusinessException(400, "正文过短（当前 " + wordCount + " 词），建议至少 200 词后再增强");
		}
		if (wordCount > 1000) {
			throw new BusinessException(400, "正文过长（当前 " + wordCount + " 词），请压缩到 1000 词以内");
		}

		String debounceKey = Integer.toHexString((title == null ? "" : title).hashCode() * 31 + content.hashCode());
		long now = System.currentTimeMillis();
		Long prev = enrichDebounce.get(debounceKey);
		if (prev != null && now - prev < 10_000) {
			throw new BusinessException(429, "请稍后再试（10 秒内请勿重复增强）");
		}

		if (!deepSeekChatService.isConfigured()) {
			throw new BusinessException("AI 未配置：请设置 island.ai.enabled=true 并填写 api-key");
		}

		List<String> warnings = new ArrayList<>();
		if (wordCount < 280) {
			warnings.add("词数 " + wordCount + " 低于 CET4 建议下限 280，发布前请确认篇幅");
		} else if (wordCount > 900) {
			warnings.add("词数 " + wordCount + " 接近上限 1000，注意用户阅读负担");
		}

		VocabQuota quota = VocabQuota.forWordCount(wordCount);
		String userPrompt = """
				Analyze the article (approx. %d English words) and return ONLY valid JSON with this shape:
				{
				  "summaryZh": "80-120 Chinese chars",
				  "topic": "one of the 8 allowed slugs",
				  "difficulty": "cet4 or cet6",
				  "slugSuggestion": "optional-kebab-case",
				  "coverHint": "English prompt for a warm nature photo matching the theme",
				  "cetVocab": [{"word":"word or phrase","zh":"","pos":"n.|v.|adj.|adv.|phrase"}],
				  "hardVocab": [{"word":"word or phrase","zh":"","pos":"","note":""}],
				  "structures": [{"en":"","zh":"","hint":""}]
				}
				Vocabulary quota for THIS article: %s
				Guide: ~400-word articles → about 10 cetVocab; ≥800-word articles → about 15+ cetVocab.
				Extract as many exam-useful items as the text truly supports—do not pad with weak words, but do not undershoot if richer vocabulary is present.
				structures: exactly 3 verbatim sentences from the text.

				Title: %s

				Article:
				%s
				""".formatted(wordCount, quota.promptLine(), title == null ? "" : title.trim(), content);

		try {
			DeepSeekChatService.ChatResult result = deepSeekChatService.chat(SYSTEM_PROMPT, userPrompt, 0.25);
			JsonNode root = deepSeekChatService.parseJsonContent(result.content());
			DailyAiEnrichmentResult enrichment = normalizeAndGate(root, content, wordCount, quota, warnings);
			if (!enrichment.isHardFail()) {
				enrichDebounce.put(debounceKey, now);
			}
			return enrichment;
		} catch (LlmException e) {
			log.warn("Daily AI enrich LLM error: {}", e.getMessage());
			return failed(wordCount, warnings, e.getMessage());
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.warn("Daily AI enrich failed: {}", e.getMessage());
			return failed(wordCount, warnings, "AI 解析失败：" + e.getMessage());
		}
	}

	private DailyAiEnrichmentResult normalizeAndGate(
			JsonNode root,
			String content,
			int wordCount,
			VocabQuota quota,
			List<String> warnings) throws Exception {

		String contentNorm = normalizeSpace(content).toLowerCase(Locale.ROOT);
		String summaryZh = text(root, "summaryZh");
		String topic = text(root, "topic").toLowerCase(Locale.ROOT);
		String difficulty = text(root, "difficulty").toLowerCase(Locale.ROOT);
		String slugSuggestion = text(root, "slugSuggestion");
		String coverHint = text(root, "coverHint");

		List<String> hardErrors = new ArrayList<>();
		if (!StringUtils.hasText(summaryZh)) {
			hardErrors.add("摘要为空");
		} else {
			int zhLen = summaryZh.codePointCount(0, summaryZh.length());
			if (zhLen < 60 || zhLen > 150) {
				warnings.add("摘要长度 " + zhLen + " 字，建议 60–150 汉字");
			}
		}
		if (!DailyTopics.isValid(topic)) {
			hardErrors.add("主题非法：" + topic);
			topic = "";
		}
		if (!"cet4".equals(difficulty) && !"cet6".equals(difficulty)) {
			hardErrors.add("难度非法：" + difficulty);
			difficulty = "";
		}

		if ("cet6".equals(difficulty) && wordCount < 260) {
			warnings.add("难度标为 cet6 但词数偏少，请人工确认");
		}
		if ("cet4".equals(difficulty) && wordCount > 380) {
			warnings.add("难度标为 cet4 但词数偏多，请人工确认");
		}

		List<Map<String, Object>> cetVocab = normalizeVocabList(root.path("cetVocab"), false, contentNorm, warnings);
		List<Map<String, Object>> hardVocab = normalizeVocabList(root.path("hardVocab"), true, contentNorm, warnings);
		dedupeAcrossLists(cetVocab, hardVocab, warnings);

		if (cetVocab.isEmpty()) {
			hardErrors.add("高频词为空");
		} else if (cetVocab.size() < quota.cetMin()) {
			warnings.add("高频词/词组 " + cetVocab.size() + " 个，本篇幅建议至少约 "
					+ quota.cetTarget() + "（下限 " + quota.cetMin() + "）");
		} else if (cetVocab.size() > quota.cetMax()) {
			warnings.add("高频词/词组 " + cetVocab.size() + " 个偏多（建议 ≤" + quota.cetMax() + "），已保留，可人工精简");
		}

		if (hardVocab.isEmpty()) {
			hardErrors.add("难词为空");
		} else if (hardVocab.size() < quota.hardMin()) {
			warnings.add("难词/词组 " + hardVocab.size() + " 个，本篇幅建议至少约 "
					+ quota.hardTarget() + "（下限 " + quota.hardMin() + "）");
		} else if (hardVocab.size() > quota.hardMax()) {
			warnings.add("难词/词组 " + hardVocab.size() + " 个偏多（建议 ≤" + quota.hardMax() + "），已保留，可人工精简");
		}

		List<Map<String, Object>> structures = normalizeStructures(root.path("structures"), content, contentNorm, hardErrors);
		if (structures.size() != 3) {
			hardErrors.add("句式须恰好 3 条，当前 " + structures.size());
		}

		alignWithGlossary(cetVocab, hardVocab, warnings);

		String cetJson = objectMapper.writeValueAsString(cetVocab);
		String hardJson = objectMapper.writeValueAsString(hardVocab);
		String structJson = objectMapper.writeValueAsString(structures);
		String aiRaw = objectMapper.writeValueAsString(root);

		if (!hardErrors.isEmpty()) {
			String err = String.join("；", hardErrors);
			return new DailyAiEnrichmentResult(
					DailyAiEnrichmentResult.GATE_FAILED,
					List.copyOf(warnings),
					err,
					AI_VERSION,
					null, null, null, null,
					wordCount,
					null, null, null, null,
					aiRaw,
					List.of(), List.of(), List.of()
			);
		}

		String gate = warnings.isEmpty()
				? DailyAiEnrichmentResult.GATE_OK
				: DailyAiEnrichmentResult.GATE_NEEDS_REVIEW;

		return new DailyAiEnrichmentResult(
				gate,
				List.copyOf(warnings),
				null,
				AI_VERSION,
				summaryZh,
				topic,
				difficulty,
				slugSuggestion,
				wordCount,
				coverHint,
				cetJson,
				hardJson,
				structJson,
				aiRaw,
				cetVocab,
				hardVocab,
				structures
		);
	}

	private List<Map<String, Object>> normalizeVocabList(
			JsonNode arr,
			boolean hard,
			String contentNorm,
			List<String> warnings) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (!arr.isArray()) {
			return out;
		}
		Set<String> seen = new HashSet<>();
		for (JsonNode n : arr) {
			String word = normalizeVocabKey(text(n, "word"));
			if (!StringUtils.hasText(word)) {
				continue;
			}
			boolean phrase = word.contains(" ");
			if (!phrase && STOP_WORDS.contains(word)) {
				continue;
			}
			if (!seen.add(word)) {
				continue;
			}
			if (!wordAppearsInText(word, contentNorm)) {
				warnings.add((hard ? "难词" : "高频词") + "「" + word + "」未在正文中定位到，已丢弃");
				continue;
			}
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("word", word);
			item.put("zh", text(n, "zh"));
			String pos = text(n, "pos");
			if (!StringUtils.hasText(pos) && phrase) {
				pos = "phrase";
			}
			item.put("pos", pos);
			if (hard) {
				item.put("note", text(n, "note"));
			}
			item.put("inGlossary", false);
			out.add(item);
		}
		return out;
	}

	/** 保留空格以支持词组；去掉其它标点。 */
	private static String normalizeVocabKey(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z'\\-\\s]+", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private List<Map<String, Object>> normalizeStructures(
			JsonNode arr,
			String content,
			String contentNorm,
			List<String> hardErrors) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (!arr.isArray()) {
			return out;
		}
		for (JsonNode n : arr) {
			String en = text(n, "en");
			if (!StringUtils.hasText(en)) {
				continue;
			}
			String enNorm = normalizeSpace(en);
			if (!sentenceInText(enNorm, content, contentNorm)) {
				hardErrors.add("句式不在原文中：" + truncate(enNorm, 48));
				continue;
			}
			// Prefer the exact span from original content when possible
			String canonical = findCanonicalSentence(enNorm, content);
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("en", canonical != null ? canonical : enNorm);
			item.put("zh", text(n, "zh"));
			item.put("hint", text(n, "hint"));
			out.add(item);
		}
		return out;
	}

	private void dedupeAcrossLists(
			List<Map<String, Object>> cet,
			List<Map<String, Object>> hard,
			List<String> warnings) {
		Set<String> cetWords = new HashSet<>();
		for (Map<String, Object> m : cet) {
			cetWords.add(String.valueOf(m.get("word")));
		}
		hard.removeIf(m -> {
			String w = String.valueOf(m.get("word"));
			if (cetWords.contains(w)) {
				warnings.add("词「" + w + "」同时出现在高频与难词，已从难词移除");
				return true;
			}
			return false;
		});
	}

	private void alignWithGlossary(
			List<Map<String, Object>> cetVocab,
			List<Map<String, Object>> hardVocab,
			List<String> warnings) {
		Set<String> words = new HashSet<>();
		for (Map<String, Object> m : cetVocab) {
			words.add(String.valueOf(m.get("word")));
		}
		for (Map<String, Object> m : hardVocab) {
			words.add(String.valueOf(m.get("word")));
		}
		if (words.isEmpty()) {
			return;
		}
		Map<String, Vocabulary> glossary = new LinkedHashMap<>();
		for (String w : words) {
			Vocabulary v = vocabularyMapper.selectOne(new LambdaQueryWrapper<Vocabulary>()
					.apply("LOWER(word) = {0}", w)
					.last("LIMIT 1"));
			if (v != null) {
				glossary.put(w, v);
			}
		}
		int miss = 0;
		for (Map<String, Object> m : cetVocab) {
			applyGlossary(m, glossary);
			if (!Boolean.TRUE.equals(m.get("inGlossary"))) {
				miss++;
			}
		}
		for (Map<String, Object> m : hardVocab) {
			applyGlossary(m, glossary);
			if (!Boolean.TRUE.equals(m.get("inGlossary"))) {
				miss++;
			}
		}
		int total = cetVocab.size() + hardVocab.size();
		if (total > 0 && miss * 100 / total > 30) {
			warnings.add("超过 30% 词汇未命中站内词库（" + miss + "/" + total + "），建议人工扫一眼释义");
		}
	}

	private void applyGlossary(Map<String, Object> m, Map<String, Vocabulary> glossary) {
		String word = String.valueOf(m.get("word"));
		Vocabulary v = glossary.get(word);
		if (v == null) {
			m.put("inGlossary", false);
			return;
		}
		m.put("inGlossary", true);
		m.put("vocabularyId", v.getId());
		if (StringUtils.hasText(v.getMeaningZh())) {
			m.put("zh", v.getMeaningZh());
		}
		if (StringUtils.hasText(v.getPartOfSpeech())) {
			m.put("pos", v.getPartOfSpeech());
		}
	}

	private static boolean wordAppearsInText(String word, String contentNorm) {
		if (!StringUtils.hasText(word) || !StringUtils.hasText(contentNorm)) {
			return false;
		}
		if (word.contains(" ")) {
			return contentNorm.contains(word);
		}
		if (contentNorm.contains(word)) {
			return true;
		}
		// light morphology for single words
		if (word.endsWith("ies") && word.length() > 4) {
			String stem = word.substring(0, word.length() - 3) + "y";
			if (contentNorm.contains(stem)) {
				return true;
			}
		}
		if (word.endsWith("s") && word.length() > 3) {
			if (contentNorm.contains(word.substring(0, word.length() - 1))) {
				return true;
			}
		}
		if (word.endsWith("ed") && word.length() > 4) {
			if (contentNorm.contains(word.substring(0, word.length() - 2))) {
				return true;
			}
		}
		if (word.endsWith("ing") && word.length() > 5) {
			if (contentNorm.contains(word.substring(0, word.length() - 3))) {
				return true;
			}
		}
		return false;
	}

	private static boolean sentenceInText(String enNorm, String content, String contentNorm) {
		String enLower = enNorm.toLowerCase(Locale.ROOT);
		if (contentNorm.contains(enLower)) {
			return true;
		}
		// allow minor whitespace differences already normalized; try without trailing period
		String stripped = enLower.replaceAll("[.!?]$", "").trim();
		return contentNorm.contains(stripped);
	}

	private static String findCanonicalSentence(String enNorm, String content) {
		String lowerContent = content.toLowerCase(Locale.ROOT);
		String needle = enNorm.toLowerCase(Locale.ROOT);
		int idx = lowerContent.indexOf(needle);
		if (idx >= 0) {
			return content.substring(idx, idx + enNorm.length());
		}
		String stripped = needle.replaceAll("[.!?]$", "").trim();
		idx = lowerContent.indexOf(stripped);
		if (idx >= 0) {
			int end = Math.min(content.length(), idx + stripped.length() + 1);
			return content.substring(idx, end).trim();
		}
		return null;
	}

	private DailyAiEnrichmentResult failed(int wordCount, List<String> warnings, String error) {
		return new DailyAiEnrichmentResult(
				DailyAiEnrichmentResult.GATE_FAILED,
				List.copyOf(warnings),
				error,
				AI_VERSION,
				null, null, null, null,
				wordCount,
				null, null, null, null,
				null,
				List.of(), List.of(), List.of()
		);
	}

	private static String text(JsonNode root, String field) {
		String v = root.path(field).asText("");
		return v == null ? "" : v.trim();
	}

	private static String normalizeSpace(String s) {
		return s == null ? "" : s.replaceAll("\\s+", " ").trim();
	}

	private static String truncate(String s, int max) {
		if (s == null || s.length() <= max) {
			return s;
		}
		return s.substring(0, max) + "…";
	}

	public static int countWords(String content) {
		if (!StringUtils.hasText(content)) {
			return 0;
		}
		String[] parts = content.trim().split("\\s+");
		return parts.length;
	}

	/** Expose for tests / admin word-count preview without AI */
	public boolean isConfigured() {
		return deepSeekChatService.isConfigured();
	}
}
