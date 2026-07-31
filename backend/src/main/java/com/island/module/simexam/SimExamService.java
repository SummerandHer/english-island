package com.island.module.simexam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.common.BusinessException;
import com.island.module.simexam.dto.AdminSimSourceRequest;
import com.island.module.simexam.dto.SubmitSimExamRequest;
import com.island.module.simexam.mapper.SimOptionMapper;
import com.island.module.simexam.mapper.SimPassageMapper;
import com.island.module.simexam.mapper.SimQuestionMapper;
import com.island.module.simexam.mapper.SimSourcePaperMapper;
import com.island.module.simexam.mapper.SimSubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimExamService {

	private final SimSourcePaperMapper sourceMapper;
	private final SimPassageMapper passageMapper;
	private final SimQuestionMapper questionMapper;
	private final SimOptionMapper optionMapper;
	private final SimSubmissionMapper submissionMapper;
	private final SimExamAiGenerateService aiGenerateService;
	private final ObjectMapper objectMapper;

	// —— Admin sources ——

	public List<SourceSummary> adminListSources() {
		return sourceMapper.selectList(new LambdaQueryWrapper<SimSourcePaper>()
						.orderByDesc(SimSourcePaper::getId))
				.stream()
				.map(s -> new SourceSummary(
						s.getId(), s.getExamLevel(), s.getSectionType(), s.getTitle(),
						s.getSourceMeta(), s.getStatus(), s.getCreatedAt() == null ? null : s.getCreatedAt().toString()))
				.toList();
	}

	public SourceDetail adminGetSource(Long id) {
		SimSourcePaper s = requireSource(id);
		return toSourceDetail(s);
	}

	@Transactional
	public Long adminCreateSource(AdminSimSourceRequest req) {
		validateSourceRequest(req);
		SimSourcePaper row = new SimSourcePaper();
		applySource(row, req);
		row.setStatus("draft");
		sourceMapper.insert(row);
		return row.getId();
	}

	@Transactional
	public void adminUpdateSource(Long id, AdminSimSourceRequest req) {
		validateSourceRequest(req);
		SimSourcePaper row = requireSource(id);
		applySource(row, req);
		sourceMapper.updateById(row);
	}

	@Transactional
	public GenerateResult adminGenerate(Long sourceId) {
		SimSourcePaper source = requireSource(sourceId);
		SimPassage running = findLatestPassageForSource(sourceId);
		if (running != null && "running".equals(running.getAiStatus())) {
			throw new BusinessException(429, "正在生成中，请稍候");
		}

		SimPassage passage = running != null && !"published".equals(running.getStatus())
				? running
				: newDraftPassage(source);

		passage.setAiStatus("running");
		passage.setAiError(null);
		if (passage.getId() == null) {
			passageMapper.insert(passage);
		} else {
			passageMapper.updateById(passage);
		}

		try {
			SimExamAiGenerateService.GeneratedSim gen = aiGenerateService.generate(source);
			passage.setTitle(gen.title());
			passage.setContentEn(gen.contentEn());
			passage.setContentZh(gen.contentZh());
			passage.setWordCount(gen.wordCount());
			passage.setVocabCount(gen.vocabCount());
			passage.setRecommendedMinutes(gen.recommendedMinutes());
			passage.setVocabJson(gen.vocabJson());
			passage.setSimilarityScore(gen.similarityScore());
			passage.setAiRawJson(gen.aiRawJson());
			passage.setAiVersion(gen.aiVersion());
			passage.setAiStatus(gen.gate());
			passage.setAiError(gen.warnings().isEmpty() ? null : String.join("; ", gen.warnings()));
			passage.setStatus("needs_review".equals(gen.gate()) ? "draft" : "ready");
			passageMapper.updateById(passage);

			replaceQuestions(passage.getId(), gen.questions());

			return new GenerateResult(
					passage.getId(),
					passage.getAiStatus(),
					passage.getAiError(),
					passage.getStatus(),
					gen.warnings(),
					passage.getSimilarityScore() == null ? null : passage.getSimilarityScore().doubleValue());
		} catch (BusinessException e) {
			passage.setAiStatus("failed");
			passage.setAiError(truncate(e.getMessage(), 490));
			passageMapper.updateById(passage);
			throw e;
		} catch (Exception e) {
			passage.setAiStatus("failed");
			passage.setAiError(truncate(e.getMessage(), 490));
			passageMapper.updateById(passage);
			throw new BusinessException(502, "生成失败: " + e.getMessage());
		}
	}

	public List<AdminPassageSummary> adminListPassages(String status, String sectionType) {
		LambdaQueryWrapper<SimPassage> qw = new LambdaQueryWrapper<SimPassage>()
				.orderByDesc(SimPassage::getId);
		if (StringUtils.hasText(status)) {
			qw.eq(SimPassage::getStatus, status);
		}
		if (StringUtils.hasText(sectionType)) {
			qw.eq(SimPassage::getSectionType, sectionType);
		}
		return passageMapper.selectList(qw).stream()
				.map(p -> new AdminPassageSummary(
						p.getId(), p.getTitle(), p.getExamLevel(), p.getSectionType(),
						p.getStatus(), p.getAiStatus(), p.getWordCount(), p.getVocabCount(),
						p.getRecommendedMinutes(), p.getDerivedFromSourceId()))
				.toList();
	}

	public AdminPassageDetail adminGetPassage(Long id) {
		SimPassage p = requirePassageEntity(id);
		return toAdminDetail(p, true);
	}

	@Transactional
	public void adminPublish(Long id) {
		SimPassage p = requirePassageEntity(id);
		if ("failed".equals(p.getAiStatus()) && p.getDerivedFromSourceId() != null) {
			throw new BusinessException(400, "AI 生成失败的卷不可发布；请重试生成（禁止手工冒充仿真卷）");
		}
		List<SimQuestion> qs = loadQuestions(id);
		if (qs.isEmpty()) {
			throw new BusinessException(400, "无题目，无法发布");
		}
		if (!StringUtils.hasText(p.getContentEn())) {
			throw new BusinessException(400, "正文为空，无法发布");
		}
		p.setStatus("published");
		passageMapper.updateById(p);
	}

	public SubmitResult adminTrialSubmit(Long id, SubmitSimExamRequest request) {
		return grade(id, request, null, false);
	}

	// —— User ——

	public HubPayload hub() {
		long shortCount = passageMapper.selectCount(publishedQw().eq(SimPassage::getSectionType, SimExamConstants.SECTION_SHORT));
		long longCount = passageMapper.selectCount(publishedQw().eq(SimPassage::getSectionType, SimExamConstants.SECTION_LONG));
		return new HubPayload(shortCount, longCount,
				"本站题目为按四六级考点生成的仿真练习，非考试院历年原题；原题请以中国教育考试网为准。");
	}

	public List<PassageCard> listPublished(String sectionType, String examLevel) {
		LambdaQueryWrapper<SimPassage> qw = publishedQw().orderByAsc(SimPassage::getSortOrder).orderByDesc(SimPassage::getId);
		if (StringUtils.hasText(sectionType)) {
			qw.eq(SimPassage::getSectionType, sectionType);
		}
		if (StringUtils.hasText(examLevel)) {
			qw.eq(SimPassage::getExamLevel, examLevel);
		}
		return passageMapper.selectList(qw).stream()
				.map(p -> new PassageCard(
						p.getId(), p.getTitle(), p.getExamLevel(), p.getSectionType(),
						p.getWordCount(), p.getVocabCount(), p.getRecommendedMinutes(),
						countQuestions(p.getId())))
				.toList();
	}

	/** 考试态：无答案、无解析、无中译 */
	public PracticeDetail getPractice(Long id) {
		SimPassage p = requirePublished(id);
		List<SimQuestion> questions = loadQuestions(id);
		Map<Long, List<OptionView>> options = loadOptionsGrouped(
				questions.stream().map(SimQuestion::getId).toList());
		List<ParagraphView> paragraphs = SimExamParagraphs.parse(p.getContentEn()).stream()
				.map(para -> new ParagraphView(para.label(), para.text()))
				.toList();
		// 匹配题若无选项，用段落标号合成
		List<QuestionPractice> qViews = questions.stream()
				.map(q -> {
					List<OptionView> opts = options.getOrDefault(q.getId(), List.of());
					if (opts.isEmpty() && "match".equals(q.getQuestionType()) && !paragraphs.isEmpty()) {
						opts = paragraphs.stream()
								.map(para -> new OptionView(q.getId(), para.label(), "Paragraph " + para.label()))
								.toList();
					}
					return new QuestionPractice(
							q.getId(), q.getQuestionType(), q.getStem(), q.getSortOrder(), opts);
				})
				.toList();
		return new PracticeDetail(
				p.getId(), p.getTitle(), p.getExamLevel(), p.getSectionType(),
				p.getContentEn(), p.getWordCount(), p.getVocabCount(),
				p.getRecommendedMinutes(), paragraphs, qViews);
	}

	@Transactional
	public SubmitResult submit(Long passageId, Long userId, SubmitSimExamRequest request) {
		return grade(passageId, request, userId, true);
	}

	public List<SubmissionSummary> listMine(Long userId, int limit) {
		int lim = Math.min(Math.max(limit, 1), 50);
		return submissionMapper.selectList(new LambdaQueryWrapper<SimSubmission>()
						.eq(SimSubmission::getUserId, userId)
						.orderByDesc(SimSubmission::getCreatedAt)
						.last("LIMIT " + lim))
				.stream()
				.map(s -> {
					SimPassage p = passageMapper.selectById(s.getPassageId());
					return new SubmissionSummary(
							s.getId(),
							s.getPassageId(),
							p == null ? "" : p.getTitle(),
							s.getCorrectCount(),
							s.getTotalQuestions(),
							s.getElapsedSeconds(),
							s.getCreatedAt() == null ? null : s.getCreatedAt().toString());
				})
				.toList();
	}

	/** 回看某次提交：保留当时选项，解析/题干取当前篇章最新内容 */
	public SubmitResult getSubmission(Long userId, Long submissionId) {
		SimSubmission sub = submissionMapper.selectById(submissionId);
		if (sub == null || !userId.equals(sub.getUserId())) {
			throw new BusinessException(404, "做题记录不存在");
		}
		SimPassage passage = requirePassageEntity(sub.getPassageId());
		List<SimQuestion> questions = loadQuestions(passage.getId());
		Map<Long, SimQuestion> qMap = questions.stream()
				.collect(Collectors.toMap(SimQuestion::getId, q -> q));
		Map<Long, List<SimOption>> optsByQ = Map.of();
		if (!qMap.isEmpty()) {
			optsByQ = optionMapper.selectList(new LambdaQueryWrapper<SimOption>()
							.in(SimOption::getQuestionId, qMap.keySet()))
					.stream()
					.collect(Collectors.groupingBy(SimOption::getQuestionId));
		}

		List<Map<String, Object>> stored = sub.getAnswersJson() == null ? List.of() : sub.getAnswersJson();
		List<QuestionResult> results = new ArrayList<>();
		for (Map<String, Object> row : stored) {
			Long qid = toLong(row.get("questionId"));
			if (qid == null) {
				continue;
			}
			String userLabel = normalizeLabel(asString(row.get("userLabel")));
			String storedCorrectLabel = normalizeLabel(asString(row.get("correctLabel")));
			boolean storedOk = Boolean.TRUE.equals(row.get("correct"))
					|| "true".equalsIgnoreCase(asString(row.get("correct")));

			SimQuestion q = qMap.get(qid);
			if (q == null) {
				results.add(new QuestionResult(
						qid, "（原题已更新，仅保留当时选项）", null,
						userLabel, storedCorrectLabel, storedOk,
						null, null, null, Map.of(), null, List.of()));
				continue;
			}
			List<SimOption> opts = optsByQ.getOrDefault(qid, List.of());
			String liveCorrect = resolveCorrectLabel(q, opts);
			String correctLabel = liveCorrect != null ? liveCorrect : storedCorrectLabel;
			boolean ok = userLabel != null && userLabel.equals(correctLabel);
			results.add(new QuestionResult(
					q.getId(), q.getStem(), q.getSkillTag(), userLabel, correctLabel, ok,
					q.getLocateEn(), q.getLocateZh(), q.getExplainCorrect(),
					parseDistractors(q.getExplainDistractorsJson()), q.getExplainTip(),
					opts.stream()
							.sorted(Comparator.comparing(SimOption::getLabel))
							.map(o -> new OptionView(o.getQuestionId(), o.getLabel(), o.getContent()))
							.toList()));
		}

		int elapsed = sub.getElapsedSeconds() == null ? 0 : sub.getElapsedSeconds();
		int correct = sub.getCorrectCount() == null
				? (int) results.stream().filter(QuestionResult::correct).count()
				: sub.getCorrectCount();
		int total = sub.getTotalQuestions() == null ? results.size() : sub.getTotalQuestions();
		return new SubmitResult(
				passage.getId(),
				passage.getTitle(),
				correct,
				total,
				elapsed,
				passage.getRecommendedMinutes(),
				passage.getContentEn(),
				passage.getContentZh(),
				parseVocab(passage.getVocabJson()),
				results);
	}

	// —— internals ——

	private SubmitResult grade(Long passageId, SubmitSimExamRequest request, Long userId, boolean requirePublished) {
		SimPassage passage = requirePublished
				? requirePublished(passageId)
				: requirePassageEntity(passageId);
		List<SimQuestion> questions = loadQuestions(passageId);
		if (questions.isEmpty()) {
			throw new BusinessException(404, "该篇章暂无题目");
		}
		Map<Long, SimQuestion> qMap = questions.stream().collect(Collectors.toMap(SimQuestion::getId, q -> q));
		Map<Long, List<SimOption>> opts = optionMapper.selectList(new LambdaQueryWrapper<SimOption>()
						.in(SimOption::getQuestionId, qMap.keySet()))
				.stream()
				.collect(Collectors.groupingBy(SimOption::getQuestionId));

		List<QuestionResult> results = new ArrayList<>();
		int correct = 0;
		for (SubmitSimExamRequest.AnswerItem item : request.getAnswers()) {
			SimQuestion q = qMap.get(item.getQuestionId());
			if (q == null) {
				throw new BusinessException(400, "题目不属于本篇章: " + item.getQuestionId());
			}
			String label = normalizeLabel(item.getLabel());
			String correctLabel = resolveCorrectLabel(q, opts.getOrDefault(q.getId(), List.of()));
			boolean ok = label != null && label.equals(correctLabel);
			if (ok) {
				correct++;
			}
			results.add(new QuestionResult(
					q.getId(), q.getStem(), q.getSkillTag(), label, correctLabel, ok,
					q.getLocateEn(), q.getLocateZh(), q.getExplainCorrect(),
					parseDistractors(q.getExplainDistractorsJson()), q.getExplainTip(),
					opts.getOrDefault(q.getId(), List.of()).stream()
							.sorted(Comparator.comparing(SimOption::getLabel))
							.map(o -> new OptionView(o.getQuestionId(), o.getLabel(), o.getContent()))
							.toList()));
		}

		int elapsed = request.getElapsedSeconds() == null ? 0 : Math.max(0, request.getElapsedSeconds());
		if (userId != null) {
			SimSubmission sub = new SimSubmission();
			sub.setUserId(userId);
			sub.setPassageId(passageId);
			sub.setCorrectCount(correct);
			sub.setTotalQuestions(questions.size());
			sub.setElapsedSeconds(elapsed);
			List<Map<String, Object>> answersJson = results.stream().map(r -> {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("questionId", r.questionId());
				m.put("userLabel", r.userLabel());
				m.put("correctLabel", r.correctLabel());
				m.put("correct", r.correct());
				return m;
			}).toList();
			sub.setAnswersJson(answersJson);
			submissionMapper.insert(sub);
		}

		List<Map<String, String>> vocab = parseVocab(passage.getVocabJson());
		return new SubmitResult(
				passage.getId(),
				passage.getTitle(),
				correct,
				questions.size(),
				elapsed,
				passage.getRecommendedMinutes(),
				passage.getContentEn(),
				passage.getContentZh(),
				vocab,
				results);
	}

	private String resolveCorrectLabel(SimQuestion q, List<SimOption> options) {
		if (StringUtils.hasText(q.getAnswerKey())) {
			return normalizeLabel(q.getAnswerKey());
		}
		return options.stream()
				.filter(o -> o.getIsCorrect() != null && o.getIsCorrect() == 1)
				.map(SimOption::getLabel)
				.findFirst()
				.map(this::normalizeLabel)
				.orElse(null);
	}

	private void replaceQuestions(Long passageId, List<SimExamAiGenerateService.GeneratedQuestion> questions) {
		List<SimQuestion> old = loadQuestions(passageId);
		if (!old.isEmpty()) {
			List<Long> ids = old.stream().map(SimQuestion::getId).toList();
			optionMapper.delete(new LambdaQueryWrapper<SimOption>().in(SimOption::getQuestionId, ids));
			questionMapper.delete(new LambdaQueryWrapper<SimQuestion>().eq(SimQuestion::getPassageId, passageId));
		}
		int order = 1;
		for (SimExamAiGenerateService.GeneratedQuestion gq : questions) {
			SimQuestion q = new SimQuestion();
			q.setPassageId(passageId);
			q.setQuestionType(StringUtils.hasText(gq.questionType()) ? gq.questionType() : "single");
			q.setStem(gq.stem());
			q.setSkillTag(gq.skillTag());
			q.setAnswerKey(gq.answerKey());
			q.setLocateEn(gq.locateEn());
			q.setLocateZh(gq.locateZh());
			q.setExplainCorrect(gq.explainCorrect());
			q.setExplainDistractorsJson(gq.explainDistractorsJson());
			q.setExplainTip(gq.explainTip());
			q.setSortOrder(order++);
			questionMapper.insert(q);
			for (SimExamAiGenerateService.GeneratedOption go : gq.options()) {
				SimOption o = new SimOption();
				o.setQuestionId(q.getId());
				o.setLabel(go.label());
				o.setContent(go.content());
				o.setIsCorrect(go.correct() ? 1 : 0);
				optionMapper.insert(o);
			}
		}
	}

	private SimPassage newDraftPassage(SimSourcePaper source) {
		SimPassage p = new SimPassage();
		p.setDerivedFromSourceId(source.getId());
		p.setExamLevel(source.getExamLevel());
		p.setSectionType(source.getSectionType());
		p.setTitle(source.getTitle() + " · 仿真稿");
		p.setContentEn("");
		p.setStatus("draft");
		p.setAiStatus("idle");
		p.setSortOrder(0);
		return p;
	}

	private SimPassage findLatestPassageForSource(Long sourceId) {
		return passageMapper.selectOne(new LambdaQueryWrapper<SimPassage>()
				.eq(SimPassage::getDerivedFromSourceId, sourceId)
				.orderByDesc(SimPassage::getId)
				.last("LIMIT 1"));
	}

	private void validateSourceRequest(AdminSimSourceRequest req) {
		if (!SimExamConstants.EXAM_LEVELS.contains(req.getExamLevel())) {
			throw new BusinessException(400, "examLevel 须为 cet4 或 cet6");
		}
		if (!SimExamConstants.SECTION_TYPES.contains(req.getSectionType())) {
			throw new BusinessException(400, "sectionType 须为 short_careful 或 long_match");
		}
		if (!StringUtils.hasText(req.getPassageEn()) || req.getPassageEn().trim().length() < 100) {
			throw new BusinessException(400, "底稿正文过短");
		}
		String rawQuestions = StringUtils.hasText(req.getQuestionsText())
				? req.getQuestionsText()
				: req.getQuestionsJson();
		if (!StringUtils.hasText(rawQuestions)) {
			throw new BusinessException(400, "请粘贴题目文本（题干 + A/B/C/D 选项）");
		}
		// 解析校验：不合法会抛 BusinessException
		SimExamQuestionsParser.normalizeToJson(rawQuestions, req.getAnswersText(), objectMapper);
	}

	private void applySource(SimSourcePaper row, AdminSimSourceRequest req) {
		row.setExamLevel(req.getExamLevel());
		row.setSectionType(req.getSectionType());
		row.setTitle(req.getTitle().trim());
		row.setPassageEn(req.getPassageEn().trim());
		String rawQuestions = StringUtils.hasText(req.getQuestionsText())
				? req.getQuestionsText().trim()
				: (req.getQuestionsJson() == null ? "" : req.getQuestionsJson().trim());
		String rawAnswers = req.getAnswersText() == null ? "" : req.getAnswersText().trim();
		// 原文原样入库，编辑时原样回显
		row.setQuestionsText(rawQuestions);
		row.setAnswersText(rawAnswers);
		// 同时解析为结构化 JSON，供 AI 仿写使用
		row.setQuestionsJson(SimExamQuestionsParser.normalizeToJson(rawQuestions, rawAnswers, objectMapper));
		row.setSourceMeta(req.getSourceMeta());
		row.setLicenseNote(StringUtils.hasText(req.getLicenseNote())
				? req.getLicenseNote().trim()
				: SimExamConstants.LICENSE_NOTE);
		row.setOfficialExplains(req.getOfficialExplains());
	}

	private SimSourcePaper requireSource(Long id) {
		SimSourcePaper s = sourceMapper.selectById(id);
		if (s == null) {
			throw new BusinessException(404, "底稿不存在");
		}
		return s;
	}

	private SimPassage requirePassageEntity(Long id) {
		SimPassage p = passageMapper.selectById(id);
		if (p == null) {
			throw new BusinessException(404, "仿真篇章不存在");
		}
		return p;
	}

	private SimPassage requirePublished(Long id) {
		SimPassage p = requirePassageEntity(id);
		if (!"published".equals(p.getStatus())) {
			throw new BusinessException(404, "仿真篇章不存在或未发布");
		}
		return p;
	}

	private LambdaQueryWrapper<SimPassage> publishedQw() {
		return new LambdaQueryWrapper<SimPassage>().eq(SimPassage::getStatus, "published");
	}

	private List<SimQuestion> loadQuestions(Long passageId) {
		return questionMapper.selectList(new LambdaQueryWrapper<SimQuestion>()
				.eq(SimQuestion::getPassageId, passageId)
				.orderByAsc(SimQuestion::getSortOrder));
	}

	private Map<Long, List<OptionView>> loadOptionsGrouped(List<Long> questionIds) {
		if (questionIds.isEmpty()) {
			return Map.of();
		}
		return optionMapper.selectList(new LambdaQueryWrapper<SimOption>()
						.in(SimOption::getQuestionId, questionIds))
				.stream()
				.sorted(Comparator.comparing(SimOption::getLabel))
				.map(o -> new OptionView(o.getQuestionId(), o.getLabel(), o.getContent()))
				.collect(Collectors.groupingBy(OptionView::questionId));
	}

	private int countQuestions(Long passageId) {
		return Math.toIntExact(questionMapper.selectCount(new LambdaQueryWrapper<SimQuestion>()
				.eq(SimQuestion::getPassageId, passageId)));
	}

	private SourceDetail toSourceDetail(SimSourcePaper s) {
		String questionsText = StringUtils.hasText(s.getQuestionsText())
				? s.getQuestionsText()
				: SimExamQuestionsParser.toPlainText(s.getQuestionsJson(), objectMapper);
		String answersText = StringUtils.hasText(s.getAnswersText())
				? s.getAnswersText()
				: SimExamQuestionsParser.toAnswersText(s.getQuestionsJson(), objectMapper);
		return new SourceDetail(
				s.getId(), s.getExamLevel(), s.getSectionType(), s.getTitle(),
				s.getPassageEn(), s.getQuestionsJson(), questionsText, answersText,
				s.getSourceMeta(), s.getLicenseNote(), s.getOfficialExplains(), s.getStatus());
	}

	private AdminPassageDetail toAdminDetail(SimPassage p, boolean withQuestions) {
		List<QuestionAdmin> qs = List.of();
		if (withQuestions) {
			List<SimQuestion> questions = loadQuestions(p.getId());
			Map<Long, List<OptionView>> opts = loadOptionsGrouped(
					questions.stream().map(SimQuestion::getId).toList());
			qs = questions.stream()
					.map(q -> new QuestionAdmin(
							q.getId(), q.getStem(), q.getSkillTag(), q.getSortOrder(),
							q.getLocateEn(), q.getExplainCorrect(),
							opts.getOrDefault(q.getId(), List.of())))
					.toList();
		}
		return new AdminPassageDetail(
				p.getId(), p.getDerivedFromSourceId(), p.getExamLevel(), p.getSectionType(),
				p.getTitle(), p.getContentEn(), p.getContentZh(), p.getWordCount(), p.getVocabCount(),
				p.getRecommendedMinutes(), p.getVocabJson(), p.getStatus(), p.getAiStatus(),
				p.getAiError(), p.getAiVersion(),
				p.getSimilarityScore() == null ? null : p.getSimilarityScore().doubleValue(),
				qs);
	}

	private Map<String, String> parseDistractors(String json) {
		if (!StringUtils.hasText(json)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private List<Map<String, String>> parseVocab(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private String normalizeLabel(String label) {
		if (label == null) {
			return null;
		}
		String t = label.trim().toUpperCase(Locale.ROOT);
		return t.isEmpty() ? null : t.substring(0, 1);
	}

	private static Long toLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String asString(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		return s.length() <= max ? s : s.substring(0, max);
	}

	public record SourceSummary(Long id, String examLevel, String sectionType, String title,
								String sourceMeta, String status, String createdAt) {
	}

	public record SourceDetail(Long id, String examLevel, String sectionType, String title,
							   String passageEn, String questionsJson,
							   String questionsText, String answersText,
							   String sourceMeta,
							   String licenseNote, String officialExplains, String status) {
	}

	public record GenerateResult(Long passageId, String aiStatus, String aiError, String status,
								 List<String> warnings, Double similarityScore) {
	}

	public record AdminPassageSummary(Long id, String title, String examLevel, String sectionType,
									  String status, String aiStatus, Integer wordCount, Integer vocabCount,
									  Integer recommendedMinutes, Long derivedFromSourceId) {
	}

	public record AdminPassageDetail(Long id, Long derivedFromSourceId, String examLevel, String sectionType,
									 String title, String contentEn, String contentZh, Integer wordCount,
									 Integer vocabCount, Integer recommendedMinutes, String vocabJson,
									 String status, String aiStatus, String aiError, String aiVersion,
									 Double similarityScore, List<QuestionAdmin> questions) {
	}

	public record QuestionAdmin(Long id, String stem, String skillTag, Integer sortOrder,
								String locateEn, String explainCorrect, List<OptionView> options) {
	}

	public record HubPayload(long shortCount, long longCount, String complianceNote) {
	}

	public record PassageCard(Long id, String title, String examLevel, String sectionType,
							  Integer wordCount, Integer vocabCount, Integer recommendedMinutes,
							  int questionCount) {
	}

	public record OptionView(Long questionId, String label, String content) {
	}

	public record ParagraphView(String label, String text) {
	}

	public record QuestionPractice(Long id, String questionType, String stem, Integer sortOrder,
								   List<OptionView> options) {
	}

	public record PracticeDetail(Long id, String title, String examLevel, String sectionType,
								 String contentEn, Integer wordCount, Integer vocabCount,
								 Integer recommendedMinutes, List<ParagraphView> paragraphs,
								 List<QuestionPractice> questions) {
	}

	public record QuestionResult(Long questionId, String stem, String skillTag, String userLabel,
								 String correctLabel, boolean correct, String locateEn, String locateZh,
								 String explainCorrect, Map<String, String> explainDistractors,
								 String explainTip, List<OptionView> options) {
	}

	public record SubmitResult(Long passageId, String title, int correctCount, int totalQuestions,
							   int elapsedSeconds, Integer recommendedMinutes, String contentEn,
							   String contentZh, List<Map<String, String>> vocab,
							   List<QuestionResult> results) {
	}

	public record SubmissionSummary(Long submissionId, Long passageId, String passageTitle,
									int correctCount, int totalQuestions, int elapsedSeconds,
									String createdAt) {
	}
}
