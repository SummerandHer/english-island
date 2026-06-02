package com.island.module.translation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.translation.dto.SubmitTranslationRequest;
import com.island.module.translation.mapper.TranslationChapterMapper;
import com.island.module.translation.mapper.TranslationQuestionMapper;
import com.island.module.translation.mapper.TranslationSubmissionMapper;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranslationService {

	private final TranslationChapterMapper chapterMapper;
	private final TranslationQuestionMapper questionMapper;
	private final TranslationSubmissionMapper submissionMapper;
	private final TranslationGradingService gradingService;

	public List<ChapterSummary> listChapters() {
		return chapterMapper.selectList(new LambdaQueryWrapper<TranslationChapter>()
						.eq(TranslationChapter::getStatus, 1)
						.orderByAsc(TranslationChapter::getSortOrder))
				.stream()
				.map(ChapterSummary::from)
				.toList();
	}

	public ChapterDetail getChapter(String slug, IslandUserDetails userDetails) {
		TranslationChapter chapter = chapterMapper.selectOne(new LambdaQueryWrapper<TranslationChapter>()
				.eq(TranslationChapter::getSlug, slug)
				.eq(TranslationChapter::getStatus, 1));
		if (chapter == null) {
			throw new BusinessException(404, "章节不存在");
		}
		checkVip(chapter.getIsVip(), userDetails);
		return ChapterDetail.from(chapter);
	}

	public List<QuestionSummary> listQuestions() {
		return questionMapper.selectList(new LambdaQueryWrapper<TranslationQuestion>()
						.eq(TranslationQuestion::getStatus, 1)
						.orderByAsc(TranslationQuestion::getSortOrder))
				.stream()
				.map(QuestionSummary::from)
				.toList();
	}

	public QuestionDetail getQuestion(Long id, IslandUserDetails userDetails) {
		TranslationQuestion q = questionMapper.selectById(id);
		if (q == null || q.getStatus() != 1) {
			throw new BusinessException(404, "题目不存在");
		}
		checkVip(q.getIsVip(), userDetails);
		return QuestionDetail.from(q);
	}

	@Transactional
	public SubmissionResult submit(Long userId, SubmitTranslationRequest request) {
		TranslationQuestion q = questionMapper.selectById(request.getQuestionId());
		if (q == null) {
			throw new BusinessException(404, "题目不存在");
		}
		String prompt = "zh2en".equals(q.getDirection()) ? q.getPromptZh() : q.getPromptEn();
		var grading = gradingService.grade(q.getDirection(), prompt, q.getReferenceAnswer(), request.getUserAnswer());

		TranslationSubmission sub = new TranslationSubmission();
		sub.setUserId(userId);
		sub.setQuestionId(q.getId());
		sub.setUserAnswer(request.getUserAnswer());
		sub.setScore(grading.score());
		sub.setOverallComment(grading.overallComment());
		sub.setErrorsJson(grading.errors());
		sub.setReferenceHint(grading.referenceHint());
		sub.setAiModel(grading.aiModel());
		sub.setAiRawResponse(grading.rawResponse());
		submissionMapper.insert(sub);

		return toSubmissionResult(sub.getId(), grading);
	}

	public List<SubmissionSummary> listMySubmissions(Long userId, int limit) {
		int size = Math.clamp(limit, 1, 50);
		List<TranslationSubmission> subs = submissionMapper.selectList(new LambdaQueryWrapper<TranslationSubmission>()
				.eq(TranslationSubmission::getUserId, userId)
				.orderByDesc(TranslationSubmission::getCreatedAt)
				.last("LIMIT " + size));
		if (subs.isEmpty()) {
			return List.of();
		}

		List<Long> questionIds = subs.stream()
				.map(TranslationSubmission::getQuestionId)
				.distinct()
				.toList();
		Map<Long, TranslationQuestion> questions = questionMapper.selectBatchIds(questionIds).stream()
				.collect(Collectors.toMap(TranslationQuestion::getId, Function.identity()));

		return subs.stream()
				.map(sub -> toSubmissionSummary(sub, questions.get(sub.getQuestionId())))
				.toList();
	}

	private SubmissionResult toSubmissionResult(Long submissionId, TranslationGradingService.GradingResult grading) {
		int score = grading.score();
		var band = TranslationCetScore.bandFromScore100(score);
		return new SubmissionResult(
				submissionId,
				score,
				TranslationCetScore.toCetScore(score),
				band.code(),
				band.label(),
				band.description(),
				grading.overallComment(),
				grading.errors(),
				grading.referenceHint(),
				grading.aiModel()
		);
	}

	private SubmissionSummary toSubmissionSummary(TranslationSubmission sub, TranslationQuestion question) {
		int score = sub.getScore() != null ? sub.getScore() : 0;
		var band = TranslationCetScore.bandFromScore100(score);
		String promptPreview = promptPreview(question);
		List<Map<String, String>> errors = sub.getErrorsJson() != null ? sub.getErrorsJson() : List.of();
		return new SubmissionSummary(
				sub.getId(),
				sub.getQuestionId(),
				promptPreview,
				question != null ? question.getDirection() : null,
				sub.getUserAnswer(),
				score,
				TranslationCetScore.toCetScore(score),
				band.code(),
				band.label(),
				band.description(),
				sub.getOverallComment(),
				errors,
				errors.size(),
				sub.getReferenceHint(),
				sub.getAiModel(),
				sub.getCreatedAt()
		);
	}

	private static String promptPreview(TranslationQuestion question) {
		if (question == null) {
			return "（题目已删除）";
		}
		String text = "zh2en".equals(question.getDirection())
				? question.getPromptZh()
				: question.getPromptEn();
		if (text == null || text.isBlank()) {
			return "（无题干）";
		}
		text = text.trim().replaceAll("\\s+", " ");
		return text.length() > 60 ? text.substring(0, 60) + "…" : text;
	}

	private void checkVip(Integer isVip, IslandUserDetails userDetails) {
		if (isVip != null && isVip == 1) {
			boolean vip = userDetails != null && userDetails.getUser().isVipActive();
			if (!vip) {
				throw new BusinessException(403, "该内容为 VIP 高级技巧");
			}
		}
	}

	public record ChapterSummary(Long id, String title, String slug, String summary, boolean vip) {
		static ChapterSummary from(TranslationChapter c) {
			return new ChapterSummary(c.getId(), c.getTitle(), c.getSlug(), c.getSummary(), c.getIsVip() == 1);
		}
	}

	public record ChapterDetail(Long id, String title, String slug, String summary, String contentHtml, boolean vip) {
		static ChapterDetail from(TranslationChapter c) {
			return new ChapterDetail(c.getId(), c.getTitle(), c.getSlug(), c.getSummary(), c.getContentHtml(), c.getIsVip() == 1);
		}
	}

	public record QuestionSummary(Long id, String promptZh, String promptEn, String direction, boolean mock, boolean vip) {
		static QuestionSummary from(TranslationQuestion q) {
			return new QuestionSummary(q.getId(), q.getPromptZh(), q.getPromptEn(),
					q.getDirection(), q.getIsMock() == 1, q.getIsVip() == 1);
		}
	}

	public record QuestionDetail(Long id, String promptZh, String promptEn, String direction, boolean mock, boolean vip) {
		static QuestionDetail from(TranslationQuestion q) {
			return new QuestionDetail(q.getId(), q.getPromptZh(), q.getPromptEn(),
					q.getDirection(), q.getIsMock() == 1, q.getIsVip() == 1);
		}
	}

	public record SubmissionResult(
			Long submissionId,
			int score,
			int cetScore,
			String band,
			String bandLabel,
			String bandDescription,
			String overallComment,
			List<java.util.Map<String, String>> errors,
			String referenceHint,
			String aiModel
	) {}

	public record SubmissionSummary(
			Long submissionId,
			Long questionId,
			String promptPreview,
			String direction,
			String userAnswer,
			int score,
			int cetScore,
			String band,
			String bandLabel,
			String bandDescription,
			String overallComment,
			List<java.util.Map<String, String>> errors,
			int errorCount,
			String referenceHint,
			String aiModel,
			LocalDateTime createdAt
	) {}
}
