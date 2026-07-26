package com.island.module.simexam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.ai.DeepSeekChatService;
import com.island.ai.LlmException;
import com.island.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimExamAiGenerateService {

	public static final String AI_VERSION_SHORT = "sim-short-v1";
	public static final String AI_VERSION_LONG = "sim-long-v1";

	private static final String SYSTEM_SHORT = """
			You are the CET reading exam writer for ISLAND (四六级岛).
			Given a PAST exam passage + questions (source paper, admin-only), rewrite into a HIGH-FIDELITY SIMULATION:
			- section_type: short_careful.
			- Rewrite the passage in fresh wording; keep topic, difficulty, paragraph structure similar.
			- DO NOT copy long phrases from the source.
			- Create NEW questions (usually 5) with 4 options A-D, exactly one correct each.
			- skillTag from: main_idea, detail, inference, vocab_in_context, attitude, structure.
			- Each question: locateEn (verbatim from YOUR passage), locateZh, explainCorrect,
			  explainDistractors (A/B/C/D), explainTip (Chinese CET tip).
			- contentZh full translation; vocab 8-14 items; recommendedMinutes 8-15.
			Output pure JSON:
			{"title":"","contentEn":"","contentZh":"","recommendedMinutes":10,
			 "vocab":[{"word":"","zh":"","pos":"","note":""}],
			 "questions":[{"stem":"","skillTag":"detail",
			   "options":[{"label":"A","content":"","correct":false}],
			   "locateEn":"","locateZh":"","explainCorrect":"",
			   "explainDistractors":{"A":"","B":"","C":"","D":""},"explainTip":""}]}
			""";

	private static final String SYSTEM_LONG = """
			You are the CET Section B (long reading / information matching) writer for ISLAND.
			Rewrite the source into a simulation with labeled paragraphs and matching statements.
			Hard rules:
			1. contentEn MUST use labels like [A] [B] [C]... each on its own paragraph block (8-12 paragraphs typical).
			2. Create about 8-10 matching statements (questions). Each has answerKey = one paragraph letter.
			3. Multiple statements MAY map to the same paragraph (as in real CET).
			4. Do NOT copy long source phrases. Keep topic and difficulty.
			5. skillTag usually detail or inference. locateEn must be a substring from the matched paragraph.
			6. explainDistractors may be brief notes about nearby confusing paragraphs (keys = letters).
			7. contentZh: translate with same [A][B] labels. vocab 8-14. recommendedMinutes 12-15.
			Output pure JSON:
			{"title":"","contentEn":"[A] ...\\n\\n[B] ...","contentZh":"...","recommendedMinutes":15,
			 "vocab":[{"word":"","zh":"","pos":""}],
			 "questions":[{"stem":"statement...","skillTag":"detail","answerKey":"C",
			   "locateEn":"","locateZh":"","explainCorrect":"",
			   "explainDistractors":{"A":"","B":""},"explainTip":""}]}
			""";

	private final DeepSeekChatService deepSeekChatService;
	private final ObjectMapper objectMapper;

	public GeneratedSim generate(SimSourcePaper source) {
		if (SimExamConstants.SECTION_LONG.equals(source.getSectionType())) {
			return generateLong(source);
		}
		if (!SimExamConstants.SECTION_SHORT.equals(source.getSectionType())) {
			throw new BusinessException(400, "不支持的 sectionType: " + source.getSectionType());
		}
		return generateShort(source);
	}

	public GeneratedSim generateShort(SimSourcePaper source) {
		JsonNode root = callLlm(SYSTEM_SHORT, source, "short_careful");
		return parseShortResult(source, root);
	}

	public GeneratedSim generateLong(SimSourcePaper source) {
		JsonNode root = callLlm(SYSTEM_LONG, source, "long_match");
		return parseLongResult(source, root);
	}

	private JsonNode callLlm(String system, SimSourcePaper source, String sectionType) {
		if (!deepSeekChatService.isConfigured()) {
			throw new BusinessException("AI 未配置：请设置 island.ai.enabled=true 并填写 api-key");
		}
		String userPrompt = """
				examLevel: %s
				sectionType: %s
				sourceTitle: %s
				sourcePassage:
				%s

				sourceQuestionsJson:
				%s

				officialExplains (optional):
				%s
				""".formatted(
				source.getExamLevel(),
				sectionType,
				source.getTitle(),
				source.getPassageEn(),
				source.getQuestionsJson(),
				source.getOfficialExplains() == null ? "" : source.getOfficialExplains());
		try {
			String raw = deepSeekChatService.chat(system, userPrompt, 0.35).content();
			return objectMapper.readTree(stripFence(raw));
		} catch (LlmException e) {
			throw new BusinessException(502, "AI 调用失败: " + e.getMessage());
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessException(502, "AI 返回非 JSON，请重试");
		}
	}

	private GeneratedSim parseShortResult(SimSourcePaper source, JsonNode root) {
		List<String> warnings = new ArrayList<>();
		String title = text(root, "title");
		String contentEn = text(root, "contentEn");
		String contentZh = text(root, "contentZh");
		if (!StringUtils.hasText(title) || !StringUtils.hasText(contentEn)) {
			throw new BusinessException(502, "AI 缺少 title/contentEn");
		}
		int wordCount = countWords(contentEn);
		double similarity = sentenceOverlapRatio(source.getPassageEn(), contentEn);
		if (similarity >= SimExamConstants.SIMILARITY_REVIEW_THRESHOLD) {
			warnings.add("与底稿相似度偏高(" + formatScore(similarity) + ")，须人工抽检后发布");
		}

		JsonNode questions = root.get("questions");
		if (questions == null || !questions.isArray() || questions.isEmpty()) {
			throw new BusinessException(502, "AI 未返回 questions");
		}

		List<GeneratedQuestion> qs = new ArrayList<>();
		int qi = 0;
		for (JsonNode q : questions) {
			qi++;
			String stem = text(q, "stem");
			String locateEn = text(q, "locateEn");
			if (!StringUtils.hasText(stem)) {
				throw new BusinessException(502, "第 " + qi + " 题缺少 stem");
			}
			if (StringUtils.hasText(locateEn) && !containsIgnoreCase(contentEn, locateEn)) {
				warnings.add("第 " + qi + " 题定位句未能在仿真正文中精确匹配");
			}
			JsonNode options = q.get("options");
			if (options == null || !options.isArray() || options.size() < 2) {
				throw new BusinessException(502, "第 " + qi + " 题选项不足");
			}
			List<GeneratedOption> opts = new ArrayList<>();
			int correctCount = 0;
			String answerKey = null;
			for (JsonNode o : options) {
				boolean correct = o.path("correct").asBoolean(false);
				String label = text(o, "label").toUpperCase(Locale.ROOT).substring(0, 1);
				if (correct) {
					correctCount++;
					answerKey = label;
				}
				opts.add(new GeneratedOption(label, text(o, "content"), correct));
			}
			if (correctCount != 1) {
				throw new BusinessException(502, "第 " + qi + " 题必须恰好 1 个正确选项");
			}
			qs.add(new GeneratedQuestion(
					"single",
					stem,
					defaultSkill(text(q, "skillTag")),
					answerKey,
					opts,
					locateEn,
					text(q, "locateZh"),
					text(q, "explainCorrect"),
					distractorsJson(q),
					text(q, "explainTip")));
		}
		return finish(source, root, title, contentEn, contentZh, wordCount, similarity, warnings, qs, AI_VERSION_SHORT);
	}

	private GeneratedSim parseLongResult(SimSourcePaper source, JsonNode root) {
		List<String> warnings = new ArrayList<>();
		String title = text(root, "title");
		String contentEn = text(root, "contentEn");
		String contentZh = text(root, "contentZh");
		if (!StringUtils.hasText(title) || !StringUtils.hasText(contentEn)) {
			throw new BusinessException(502, "AI 缺少 title/contentEn");
		}
		List<SimExamParagraphs.Paragraph> paras = SimExamParagraphs.parse(contentEn);
		if (paras.size() < 6) {
			throw new BusinessException(502, "长篇仿写段落不足（需 [A][B]… 至少 6 段，当前 " + paras.size() + "）");
		}
		Set<String> labels = new HashSet<>();
		for (SimExamParagraphs.Paragraph p : paras) {
			labels.add(p.label());
		}

		int wordCount = countWords(contentEn);
		double similarity = sentenceOverlapRatio(source.getPassageEn(), contentEn);
		if (similarity >= SimExamConstants.SIMILARITY_REVIEW_THRESHOLD) {
			warnings.add("与底稿相似度偏高(" + formatScore(similarity) + ")，须人工抽检后发布");
		}

		JsonNode questions = root.get("questions");
		if (questions == null || !questions.isArray() || questions.size() < 5) {
			throw new BusinessException(502, "长篇匹配题数量不足（建议 8–10）");
		}

		List<GeneratedQuestion> qs = new ArrayList<>();
		int qi = 0;
		for (JsonNode q : questions) {
			qi++;
			String stem = text(q, "stem");
			String answerKey = text(q, "answerKey").toUpperCase(Locale.ROOT);
			if (!StringUtils.hasText(stem)) {
				throw new BusinessException(502, "第 " + qi + " 题缺少 stem");
			}
			if (answerKey.isEmpty()) {
				throw new BusinessException(502, "第 " + qi + " 题缺少 answerKey");
			}
			answerKey = answerKey.substring(0, 1);
			if (!labels.contains(answerKey)) {
				throw new BusinessException(502, "第 " + qi + " 题 answerKey=" + answerKey + " 不在正文段落标号中");
			}
			String locateEn = text(q, "locateEn");
			if (StringUtils.hasText(locateEn) && !containsIgnoreCase(contentEn, locateEn)) {
				warnings.add("第 " + qi + " 题定位句未能在仿真正文中精确匹配");
			}
			List<GeneratedOption> opts = new ArrayList<>();
			for (String lab : labels.stream().sorted().toList()) {
				opts.add(new GeneratedOption(lab, "Paragraph " + lab, lab.equals(answerKey)));
			}
			qs.add(new GeneratedQuestion(
					"match",
					stem,
					defaultSkill(text(q, "skillTag")),
					answerKey,
					opts,
					locateEn,
					text(q, "locateZh"),
					text(q, "explainCorrect"),
					distractorsJson(q),
					text(q, "explainTip")));
		}
		return finish(source, root, title, contentEn, contentZh, wordCount, similarity, warnings, qs, AI_VERSION_LONG);
	}

	private GeneratedSim finish(
			SimSourcePaper source,
			JsonNode root,
			String title,
			String contentEn,
			String contentZh,
			int wordCount,
			double similarity,
			List<String> warnings,
			List<GeneratedQuestion> qs,
			String aiVersion) {
		List<Map<String, String>> vocab = parseVocab(root.get("vocab"));
		int recommended = root.path("recommendedMinutes").asInt(0);
		if (recommended <= 0) {
			recommended = recommendMinutes(wordCount, qs.size(), source.getExamLevel(),
					SimExamConstants.SECTION_LONG.equals(source.getSectionType()));
		}
		String gate = warnings.isEmpty() && similarity < SimExamConstants.SIMILARITY_REVIEW_THRESHOLD
				? "ok"
				: "needs_review";
		String vocabJson;
		String rawJson;
		try {
			vocabJson = objectMapper.writeValueAsString(vocab);
			rawJson = objectMapper.writeValueAsString(root);
		} catch (Exception e) {
			vocabJson = "[]";
			rawJson = "{}";
		}
		return new GeneratedSim(
				title.trim(),
				contentEn.trim(),
				contentZh,
				wordCount,
				vocab.size(),
				recommended,
				vocabJson,
				qs,
				BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP),
				gate,
				warnings,
				rawJson,
				aiVersion);
	}

	private List<Map<String, String>> parseVocab(JsonNode vocabNode) {
		List<Map<String, String>> vocab = new ArrayList<>();
		if (vocabNode == null || !vocabNode.isArray()) {
			return vocab;
		}
		for (JsonNode v : vocabNode) {
			String w = text(v, "word");
			if (!StringUtils.hasText(w)) {
				continue;
			}
			Map<String, String> item = new LinkedHashMap<>();
			item.put("word", w);
			item.put("zh", text(v, "zh"));
			item.put("pos", text(v, "pos"));
			String note = text(v, "note");
			if (StringUtils.hasText(note)) {
				item.put("note", note);
			}
			vocab.add(item);
		}
		return vocab;
	}

	private String distractorsJson(JsonNode q) {
		try {
			return objectMapper.writeValueAsString(q.get("explainDistractors"));
		} catch (Exception e) {
			return "{}";
		}
	}

	private static String defaultSkill(String skill) {
		return StringUtils.hasText(skill) ? skill : "detail";
	}

	public static int recommendMinutes(int wordCount, int questionCount, String examLevel, boolean longMatch) {
		double base = longMatch
				? wordCount / 50.0 + questionCount * 0.6
				: wordCount / 35.0 + questionCount * 0.8;
		if ("cet6".equalsIgnoreCase(examLevel)) {
			base *= 1.15;
		}
		int[] slots = {5, 8, 10, 12, 15};
		int best = longMatch ? 15 : 10;
		double bestDiff = Double.MAX_VALUE;
		for (int s : slots) {
			double d = Math.abs(s - base);
			if (d < bestDiff) {
				bestDiff = d;
				best = s;
			}
		}
		return best;
	}

	/** @deprecated use overload with longMatch */
	public static int recommendMinutes(int wordCount, int questionCount, String examLevel) {
		return recommendMinutes(wordCount, questionCount, examLevel, false);
	}

	public static int countWords(String en) {
		if (!StringUtils.hasText(en)) {
			return 0;
		}
		String[] parts = en.trim().split("\\s+");
		int n = 0;
		for (String p : parts) {
			if (!p.isBlank()) {
				n++;
			}
		}
		return n;
	}

	static double sentenceOverlapRatio(String source, String generated) {
		Set<String> src = normalizeSentences(source);
		Set<String> gen = normalizeSentences(generated);
		if (src.isEmpty() || gen.isEmpty()) {
			return 0;
		}
		int hit = 0;
		for (String g : gen) {
			for (String s : src) {
				if (g.length() >= 40 && s.length() >= 40) {
					if (g.contains(s) || s.contains(g)
							|| longestCommonSubstring(g, s) >= Math.min(40, (int) (Math.min(g.length(), s.length()) * 0.6))) {
						hit++;
						break;
					}
				} else if (g.equals(s)) {
					hit++;
					break;
				}
			}
		}
		return (double) hit / gen.size();
	}

	private static Set<String> normalizeSentences(String text) {
		Set<String> out = new HashSet<>();
		if (!StringUtils.hasText(text)) {
			return out;
		}
		for (String part : text.split("[.!?\\n]+")) {
			String n = part.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
			if (n.length() >= 20) {
				out.add(n);
			}
		}
		return out;
	}

	private static int longestCommonSubstring(String a, String b) {
		int max = 0;
		int[][] dp = new int[a.length() + 1][b.length() + 1];
		for (int i = 1; i <= a.length(); i++) {
			for (int j = 1; j <= b.length(); j++) {
				if (a.charAt(i - 1) == b.charAt(j - 1)) {
					dp[i][j] = dp[i - 1][j - 1] + 1;
					max = Math.max(max, dp[i][j]);
				}
			}
		}
		return max;
	}

	private static boolean containsIgnoreCase(String hay, String needle) {
		return hay.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT).trim());
	}

	private static String stripFence(String raw) {
		String t = raw.trim();
		if (t.startsWith("```")) {
			int firstNl = t.indexOf('\n');
			int last = t.lastIndexOf("```");
			if (firstNl > 0 && last > firstNl) {
				t = t.substring(firstNl + 1, last).trim();
			}
		}
		return t;
	}

	private static String text(JsonNode node, String field) {
		if (node == null || node.get(field) == null || node.get(field).isNull()) {
			return "";
		}
		return node.get(field).asText("").trim();
	}

	private static String formatScore(double v) {
		return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	public record GeneratedOption(String label, String content, boolean correct) {
	}

	public record GeneratedQuestion(
			String questionType,
			String stem,
			String skillTag,
			String answerKey,
			List<GeneratedOption> options,
			String locateEn,
			String locateZh,
			String explainCorrect,
			String explainDistractorsJson,
			String explainTip) {
	}

	public record GeneratedSim(
			String title,
			String contentEn,
			String contentZh,
			int wordCount,
			int vocabCount,
			int recommendedMinutes,
			String vocabJson,
			List<GeneratedQuestion> questions,
			BigDecimal similarityScore,
			String gate,
			List<String> warnings,
			String aiRawJson,
			String aiVersion) {
	}
}
