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

import java.util.List;

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
		var grading = gradingService.grade(prompt, q.getReferenceAnswer(), request.getUserAnswer());

		TranslationSubmission sub = new TranslationSubmission();
		sub.setUserId(userId);
		sub.setQuestionId(q.getId());
		sub.setUserAnswer(request.getUserAnswer());
		sub.setScore(grading.score());
		sub.setOverallComment(grading.overallComment());
		sub.setErrorsJson(grading.errors());
		sub.setReferenceHint(grading.referenceHint());
		sub.setAiModel("fallback");
		sub.setAiRawResponse(grading.rawResponse());
		submissionMapper.insert(sub);

		return new SubmissionResult(sub.getId(), grading.score(), grading.overallComment(),
				grading.errors(), grading.referenceHint());
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

	public record SubmissionResult(Long submissionId, int score, String overallComment,
			List<java.util.Map<String, String>> errors, String referenceHint) {}
}
