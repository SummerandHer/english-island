package com.island.module.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.admin.dto.AdminReadingPassageRequest;
import com.island.module.admin.dto.AdminReadingQuestionInput;
import com.island.module.admin.dto.AdminTranslationQuestionRequest;
import com.island.module.reading.*;
import com.island.module.reading.mapper.*;
import com.island.module.translation.TranslationChapter;
import com.island.module.translation.TranslationQuestion;
import com.island.module.translation.mapper.TranslationChapterMapper;
import com.island.module.translation.mapper.TranslationQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminContentService {

	private final TranslationQuestionMapper translationQuestionMapper;
	private final TranslationChapterMapper translationChapterMapper;
	private final ReadingPassageMapper passageMapper;
	private final ReadingChapterMapper chapterMapper;
	private final ReadingQuestionMapper questionMapper;
	private final ReadingQuestionOptionMapper optionMapper;

	// --- Translation ---

	public List<AdminTranslationSummary> listTranslationQuestions() {
		return translationQuestionMapper.selectList(new LambdaQueryWrapper<TranslationQuestion>()
						.orderByAsc(TranslationQuestion::getSortOrder)
						.orderByDesc(TranslationQuestion::getId))
				.stream()
				.map(AdminTranslationSummary::from)
				.toList();
	}

	public AdminTranslationDetail getTranslationQuestion(Long id) {
		TranslationQuestion q = requireTranslationQuestion(id);
		return AdminTranslationDetail.from(q);
	}

	@Transactional
	public Long createTranslationQuestion(AdminTranslationQuestionRequest req) {
		validateTranslationChapter(req.getChapterId());
		TranslationQuestion q = toTranslationEntity(req);
		translationQuestionMapper.insert(q);
		return q.getId();
	}

	@Transactional
	public void updateTranslationQuestion(Long id, AdminTranslationQuestionRequest req) {
		TranslationQuestion existing = requireTranslationQuestion(id);
		validateTranslationChapter(req.getChapterId());
		TranslationQuestion q = toTranslationEntity(req);
		q.setId(existing.getId());
		translationQuestionMapper.updateById(q);
	}

	@Transactional
	public void setTranslationQuestionStatus(Long id, int status) {
		TranslationQuestion q = requireTranslationQuestion(id);
		q.setStatus(status);
		translationQuestionMapper.updateById(q);
	}

	// --- Reading ---

	public List<AdminPassageSummary> listReadingPassages() {
		return passageMapper.selectList(new LambdaQueryWrapper<ReadingPassage>()
						.orderByAsc(ReadingPassage::getSortOrder)
						.orderByDesc(ReadingPassage::getId))
				.stream()
				.map(p -> {
					int qc = Math.toIntExact(questionMapper.selectCount(
							new LambdaQueryWrapper<ReadingQuestion>().eq(ReadingQuestion::getPassageId, p.getId())));
					return AdminPassageSummary.from(p, qc);
				})
				.toList();
	}

	public AdminPassageDetail getReadingPassage(Long id) {
		ReadingPassage passage = requirePassage(id);
		List<ReadingQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<ReadingQuestion>()
				.eq(ReadingQuestion::getPassageId, id)
				.orderByAsc(ReadingQuestion::getSortOrder));
		List<Long> qIds = questions.stream().map(ReadingQuestion::getId).toList();
		Map<Long, List<ReadingQuestionOption>> optionsByQ = qIds.isEmpty()
				? Map.of()
				: optionMapper.selectList(new LambdaQueryWrapper<ReadingQuestionOption>()
								.in(ReadingQuestionOption::getQuestionId, qIds))
						.stream()
						.collect(Collectors.groupingBy(ReadingQuestionOption::getQuestionId));

		List<AdminQuestionDetail> qDetails = questions.stream()
				.map(q -> AdminQuestionDetail.from(q, optionsByQ.getOrDefault(q.getId(), List.of())))
				.toList();
		return AdminPassageDetail.from(passage, qDetails);
	}

	@Transactional
	public Long createReadingPassage(AdminReadingPassageRequest req) {
		validateReadingChapter(req.getChapterId());
		validateQuestions(req.getQuestions());
		ReadingPassage passage = new ReadingPassage();
		passage.setChapterId(req.getChapterId());
		passage.setTitle(req.getTitle());
		passage.setContentEn(req.getContentEn());
		passage.setWordCount(countWords(req.getContentEn()));
		passage.setDifficulty(req.getDifficulty());
		passage.setIsMock(req.getIsMock() != null ? req.getIsMock() : 1);
		passage.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
		passage.setStatus(req.getStatus() != null ? req.getStatus() : 1);
		passage.setSourceId(1);
		passageMapper.insert(passage);
		saveQuestions(passage.getId(), req.getQuestions());
		return passage.getId();
	}

	@Transactional
	public void updateReadingPassage(Long id, AdminReadingPassageRequest req) {
		ReadingPassage passage = requirePassage(id);
		validateReadingChapter(req.getChapterId());
		validateQuestions(req.getQuestions());
		passage.setChapterId(req.getChapterId());
		passage.setTitle(req.getTitle());
		passage.setContentEn(req.getContentEn());
		passage.setWordCount(countWords(req.getContentEn()));
		passage.setDifficulty(req.getDifficulty());
		passage.setIsMock(req.getIsMock() != null ? req.getIsMock() : 1);
		passage.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
		passage.setStatus(req.getStatus() != null ? req.getStatus() : 1);
		passageMapper.updateById(passage);

		List<Long> oldQIds = questionMapper.selectList(new LambdaQueryWrapper<ReadingQuestion>()
						.eq(ReadingQuestion::getPassageId, id))
				.stream()
				.map(ReadingQuestion::getId)
				.toList();
		if (!oldQIds.isEmpty()) {
			optionMapper.delete(new LambdaQueryWrapper<ReadingQuestionOption>()
					.in(ReadingQuestionOption::getQuestionId, oldQIds));
			questionMapper.delete(new LambdaQueryWrapper<ReadingQuestion>()
					.eq(ReadingQuestion::getPassageId, id));
		}
		saveQuestions(id, req.getQuestions());
	}

	@Transactional
	public void setReadingPassageStatus(Long id, int status) {
		ReadingPassage passage = requirePassage(id);
		passage.setStatus(status);
		passageMapper.updateById(passage);
	}

	public List<ChapterOption> listReadingChapters() {
		return chapterMapper.selectList(new LambdaQueryWrapper<ReadingChapter>()
						.orderByAsc(ReadingChapter::getSortOrder))
				.stream()
				.map(c -> new ChapterOption(c.getId(), c.getTitle(), c.getSlug()))
				.toList();
	}

	public List<ChapterOption> listTranslationChapters() {
		return translationChapterMapper.selectList(new LambdaQueryWrapper<TranslationChapter>()
						.orderByAsc(TranslationChapter::getSortOrder))
				.stream()
				.map(c -> new ChapterOption(c.getId(), c.getTitle(), c.getSlug()))
				.toList();
	}

	private void saveQuestions(Long passageId, List<AdminReadingQuestionInput> questions) {
		for (AdminReadingQuestionInput qin : questions) {
			ReadingQuestion q = new ReadingQuestion();
			q.setPassageId(passageId);
			q.setQuestionType("single");
			q.setStem(qin.getStem());
			q.setExplanation(qin.getExplanation());
			q.setSortOrder(qin.getSortOrder() != null ? qin.getSortOrder() : 0);
			questionMapper.insert(q);
			for (var opt : qin.getOptions()) {
				ReadingQuestionOption o = new ReadingQuestionOption();
				o.setQuestionId(q.getId());
				o.setLabel(opt.getLabel().trim().toUpperCase().substring(0, 1));
				o.setContent(opt.getContent());
				o.setIsCorrect(opt.getIsCorrect() != null && opt.getIsCorrect() == 1 ? 1 : 0);
				optionMapper.insert(o);
			}
		}
	}

	private static void validateQuestions(List<AdminReadingQuestionInput> questions) {
		for (AdminReadingQuestionInput q : questions) {
			long correct = q.getOptions().stream().filter(o -> o.getIsCorrect() != null && o.getIsCorrect() == 1).count();
			if (correct != 1) {
				throw new BusinessException(400, "每道题须有且仅有 1 个正确答案");
			}
		}
	}

	private static int countWords(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		return text.trim().split("\\s+").length;
	}

	private TranslationQuestion toTranslationEntity(AdminTranslationQuestionRequest req) {
		TranslationQuestion q = new TranslationQuestion();
		q.setChapterId(req.getChapterId());
		q.setDirection("zh2en");
		q.setPromptZh(req.getPromptZh());
		q.setReferenceAnswer(req.getReferenceAnswer());
		q.setDifficulty(req.getDifficulty());
		q.setIsMock(req.getIsMock() != null ? req.getIsMock() : 1);
		q.setIsVip(req.getIsVip() != null ? req.getIsVip() : 0);
		q.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
		q.setStatus(req.getStatus() != null ? req.getStatus() : 1);
		return q;
	}

	private void validateTranslationChapter(Long chapterId) {
		if (chapterId == null) {
			return;
		}
		if (translationChapterMapper.selectById(chapterId) == null) {
			throw new BusinessException(400, "翻译章节不存在");
		}
	}

	private void validateReadingChapter(Long chapterId) {
		if (chapterId == null) {
			return;
		}
		if (chapterMapper.selectById(chapterId) == null) {
			throw new BusinessException(400, "阅读章节不存在");
		}
	}

	private TranslationQuestion requireTranslationQuestion(Long id) {
		TranslationQuestion q = translationQuestionMapper.selectById(id);
		if (q == null) {
			throw new BusinessException(404, "翻译题不存在");
		}
		return q;
	}

	private ReadingPassage requirePassage(Long id) {
		ReadingPassage p = passageMapper.selectById(id);
		if (p == null) {
			throw new BusinessException(404, "阅读篇章不存在");
		}
		return p;
	}

	public record ChapterOption(Long id, String title, String slug) {
	}

	public record AdminTranslationSummary(
			Long id, Long chapterId, String promptZh, String difficulty, boolean vip, boolean mock, int status, int sortOrder) {
		static AdminTranslationSummary from(TranslationQuestion q) {
			String preview = q.getPromptZh();
			if (preview != null && preview.length() > 48) {
				preview = preview.substring(0, 48) + "…";
			}
			return new AdminTranslationSummary(
					q.getId(), q.getChapterId(), preview, q.getDifficulty(), q.getIsVip() == 1, q.getIsMock() == 1,
					q.getStatus(), q.getSortOrder());
		}
	}

	public record AdminTranslationDetail(
			Long id, Long chapterId, String promptZh, String referenceAnswer, String difficulty,
			boolean vip, boolean mock, int status, int sortOrder) {
		static AdminTranslationDetail from(TranslationQuestion q) {
			return new AdminTranslationDetail(
					q.getId(), q.getChapterId(), q.getPromptZh(), q.getReferenceAnswer(), q.getDifficulty(),
					q.getIsVip() == 1, q.getIsMock() == 1, q.getStatus(), q.getSortOrder());
		}
	}

	public record AdminPassageSummary(
			Long id, Long chapterId, String title, String difficulty, boolean mock, int status, int questionCount, int wordCount) {
		static AdminPassageSummary from(ReadingPassage p, int questionCount) {
			return new AdminPassageSummary(
					p.getId(), p.getChapterId(), p.getTitle(), p.getDifficulty(), p.getIsMock() == 1,
					p.getStatus(), questionCount, p.getWordCount());
		}
	}

	public record AdminOptionDetail(String label, String content, boolean correct) {
		static AdminOptionDetail from(ReadingQuestionOption o) {
			return new AdminOptionDetail(o.getLabel(), o.getContent(), o.getIsCorrect() == 1);
		}
	}

	public record AdminQuestionDetail(Long id, String stem, String explanation, int sortOrder, List<AdminOptionDetail> options) {
		static AdminQuestionDetail from(ReadingQuestion q, List<ReadingQuestionOption> options) {
			List<AdminOptionDetail> opts = options.stream()
					.sorted(Comparator.comparing(ReadingQuestionOption::getLabel))
					.map(AdminOptionDetail::from)
					.toList();
			return new AdminQuestionDetail(q.getId(), q.getStem(), q.getExplanation(), q.getSortOrder(), opts);
		}
	}

	public record AdminPassageDetail(
			Long id, Long chapterId, String title, String contentEn, String difficulty,
			boolean mock, int status, int sortOrder, int wordCount, List<AdminQuestionDetail> questions) {
		static AdminPassageDetail from(ReadingPassage p, List<AdminQuestionDetail> questions) {
			return new AdminPassageDetail(
					p.getId(), p.getChapterId(), p.getTitle(), p.getContentEn(), p.getDifficulty(),
					p.getIsMock() == 1, p.getStatus(), p.getSortOrder(), p.getWordCount(), questions);
		}
	}
}
