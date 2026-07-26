package com.island.module.reading;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.reading.dto.SubmitReadingRequest;
import com.island.module.reading.mapper.ReadingPassageMapper;
import com.island.module.reading.mapper.ReadingQuestionMapper;
import com.island.module.reading.mapper.ReadingQuestionOptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingPassageService {

	private final ReadingPassageMapper passageMapper;
	private final ReadingQuestionMapper questionMapper;
	private final ReadingQuestionOptionMapper optionMapper;
	private final ObjectMapper objectMapper;

	public List<PassageSummary> listByChapterId(Long chapterId) {
		return passageMapper.selectList(new LambdaQueryWrapper<ReadingPassage>()
						.eq(ReadingPassage::getChapterId, chapterId)
						.eq(ReadingPassage::getStatus, 1)
						.orderByAsc(ReadingPassage::getSortOrder))
				.stream()
				.map(p -> new PassageSummary(
						p.getId(),
						p.getChapterId(),
						p.getTitle(),
						p.getDifficulty(),
						p.getIsMock() == 1,
						p.getWordCount(),
						countQuestions(p.getId())))
				.toList();
	}

	public PassagePracticeDetail getPracticeDetail(Long passageId) {
		ReadingPassage passage = requirePassage(passageId);
		List<ReadingQuestion> questions = loadQuestions(passageId);
		if (questions.isEmpty()) {
			throw new BusinessException(404, "该篇章暂无练习题");
		}
		List<Long> questionIds = questions.stream().map(ReadingQuestion::getId).toList();
		Map<Long, List<OptionView>> optionsByQuestion = loadOptions(questionIds).stream()
				.collect(Collectors.groupingBy(OptionView::questionId));

		List<QuestionView> questionViews = questions.stream()
				.map(q -> new QuestionView(
						q.getId(),
						q.getQuestionType(),
						q.getStem(),
						q.getSortOrder(),
						optionsByQuestion.getOrDefault(q.getId(), List.of())))
				.toList();

		return new PassagePracticeDetail(
				passage.getId(),
				passage.getChapterId(),
				passage.getTitle(),
				passage.getContentEn(),
				parseLongSentences(passage.getLongSentencesJson()),
				passage.getWordCount(),
				passage.getDifficulty(),
				passage.getIsMock() == 1,
				questionViews);
	}

	public SubmitResult submit(Long passageId, SubmitReadingRequest request) {
		ReadingPassage passage = requirePassage(passageId);
		List<ReadingQuestion> questions = loadQuestions(passageId);
		if (questions.isEmpty()) {
			throw new BusinessException(404, "该篇章暂无练习题");
		}

		Map<Long, ReadingQuestion> questionMap = questions.stream()
				.collect(Collectors.toMap(ReadingQuestion::getId, q -> q));
		List<Long> questionIds = questions.stream().map(ReadingQuestion::getId).toList();
		Map<Long, List<ReadingQuestionOption>> optionsByQuestion = optionMapper.selectList(
						new LambdaQueryWrapper<ReadingQuestionOption>()
								.in(ReadingQuestionOption::getQuestionId, questionIds))
				.stream()
				.collect(Collectors.groupingBy(ReadingQuestionOption::getQuestionId));

		int correct = 0;
		List<QuestionResult> results = request.getAnswers().stream()
				.map(item -> gradeOne(item, questionMap, optionsByQuestion))
				.toList();

		for (QuestionResult r : results) {
			if (r.correct()) {
				correct++;
			}
		}

		int total = questions.size();
		int answered = results.size();
		return new SubmitResult(
				passage.getId(),
				passage.getTitle(),
				correct,
				total,
				answered,
				results);
	}

	private QuestionResult gradeOne(
			SubmitReadingRequest.AnswerItem item,
			Map<Long, ReadingQuestion> questionMap,
			Map<Long, List<ReadingQuestionOption>> optionsByQuestion) {
		ReadingQuestion question = questionMap.get(item.getQuestionId());
		if (question == null) {
			throw new BusinessException(400, "题目不属于本篇章: " + item.getQuestionId());
		}
		String label = normalizeLabel(item.getLabel());
		List<ReadingQuestionOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());
		String correctLabel = options.stream()
				.filter(o -> o.getIsCorrect() == 1)
				.map(ReadingQuestionOption::getLabel)
				.findFirst()
				.orElse(null);
		boolean isCorrect = label != null && label.equals(correctLabel);
		return new QuestionResult(
				question.getId(),
				question.getStem(),
				label,
				correctLabel,
				isCorrect,
				question.getExplanation());
	}

	private static String normalizeLabel(String label) {
		if (label == null) {
			return null;
		}
		String t = label.trim().toUpperCase();
		return t.isEmpty() ? null : t.substring(0, 1);
	}

	private ReadingPassage requirePassage(Long passageId) {
		ReadingPassage passage = passageMapper.selectById(passageId);
		if (passage == null || passage.getStatus() != 1) {
			throw new BusinessException(404, "篇章不存在或已下线");
		}
		return passage;
	}

	public ReadingPassage getPassageEntity(Long passageId) {
		return requirePassage(passageId);
	}

	private List<ReadingQuestion> loadQuestions(Long passageId) {
		return questionMapper.selectList(new LambdaQueryWrapper<ReadingQuestion>()
				.eq(ReadingQuestion::getPassageId, passageId)
				.orderByAsc(ReadingQuestion::getSortOrder));
	}

	private List<OptionView> loadOptions(List<Long> questionIds) {
		if (questionIds.isEmpty()) {
			return List.of();
		}
		return optionMapper.selectList(new LambdaQueryWrapper<ReadingQuestionOption>()
						.in(ReadingQuestionOption::getQuestionId, questionIds))
				.stream()
				.sorted(Comparator.comparing(ReadingQuestionOption::getLabel))
				.map(o -> new OptionView(o.getQuestionId(), o.getLabel(), o.getContent()))
				.toList();
	}

	private int countQuestions(Long passageId) {
		return Math.toIntExact(questionMapper.selectCount(new LambdaQueryWrapper<ReadingQuestion>()
				.eq(ReadingQuestion::getPassageId, passageId)));
	}

	private List<LongSentenceItem> parseLongSentences(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<LongSentenceItem>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	public record LongSentenceItem(String en, String zh, String hint) {
	}

	public record PassageSummary(
			Long id,
			Long chapterId,
			String title,
			String difficulty,
			boolean mock,
			Integer wordCount,
			int questionCount) {
	}

	public record PassagePracticeDetail(
			Long id,
			Long chapterId,
			String title,
			String contentEn,
			List<LongSentenceItem> longSentences,
			Integer wordCount,
			String difficulty,
			boolean mock,
			List<QuestionView> questions) {
	}

	public record QuestionView(
			Long id,
			String questionType,
			String stem,
			int sortOrder,
			List<OptionView> options) {
	}

	public record OptionView(Long questionId, String label, String content) {
	}

	public record SubmitResult(
			Long passageId,
			String passageTitle,
			int correctCount,
			int totalQuestions,
			int answeredCount,
			List<QuestionResult> questions) {
	}

	public record QuestionResult(
			Long questionId,
			String stem,
			String userLabel,
			String correctLabel,
			boolean correct,
			String explanation) {
	}
}
