package com.island.module.reading;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.module.reading.mapper.ReadingChapterMapper;
import com.island.module.reading.mapper.ReadingPassageMapper;
import com.island.module.reading.mapper.ReadingSubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingSubmissionService {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;

	private final ReadingSubmissionMapper submissionMapper;
	private final ReadingPassageMapper passageMapper;
	private final ReadingChapterMapper chapterMapper;

	public void saveSubmission(Long userId, ReadingPassage passage, ReadingPassageService.SubmitResult result) {
		ReadingSubmission sub = new ReadingSubmission();
		sub.setUserId(userId);
		sub.setPassageId(passage.getId());
		sub.setChapterId(passage.getChapterId());
		sub.setCorrectCount(result.correctCount());
		sub.setTotalQuestions(result.totalQuestions());
		sub.setAnswersJson(result.questions().stream().map(q -> {
			Map<String, Object> m = new HashMap<>();
			m.put("questionId", q.questionId());
			m.put("userLabel", q.userLabel());
			m.put("correctLabel", q.correctLabel());
			m.put("correct", q.correct());
			return m;
		}).toList());
		submissionMapper.insert(sub);
	}

	public SubmissionSummary getLatest(Long userId) {
		List<SubmissionSummary> rows = listMine(userId, 1);
		return rows.isEmpty() ? null : rows.get(0);
	}

	public int countThisWeek(Long userId) {
		LocalDateTime since = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
		return submissionMapper.selectCount(new LambdaQueryWrapper<ReadingSubmission>()
				.eq(ReadingSubmission::getUserId, userId)
				.ge(ReadingSubmission::getCreatedAt, since)).intValue();
	}

	public long countTotal(Long userId) {
		return submissionMapper.selectCount(new LambdaQueryWrapper<ReadingSubmission>()
				.eq(ReadingSubmission::getUserId, userId));
	}

	public List<SubmissionSummary> listMine(Long userId, int limit) {
		int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
		List<ReadingSubmission> rows = submissionMapper.selectList(new LambdaQueryWrapper<ReadingSubmission>()
				.eq(ReadingSubmission::getUserId, userId)
				.orderByDesc(ReadingSubmission::getCreatedAt)
				.last("LIMIT " + safeLimit));
		if (rows.isEmpty()) {
			return List.of();
		}

		Map<Long, ReadingPassage> passageMap = passageMapper.selectBatchIds(
				rows.stream().map(ReadingSubmission::getPassageId).distinct().toList())
				.stream()
				.collect(java.util.stream.Collectors.toMap(ReadingPassage::getId, p -> p));

		Map<Long, String> chapterSlugById = chapterMapper.selectBatchIds(
				rows.stream()
						.map(ReadingSubmission::getChapterId)
						.filter(id -> id != null)
						.distinct()
						.toList())
				.stream()
				.collect(java.util.stream.Collectors.toMap(ReadingChapter::getId, ReadingChapter::getSlug));

		return rows.stream().map(r -> {
			ReadingPassage p = passageMap.get(r.getPassageId());
			String title = p != null ? p.getTitle() : "阅读模拟";
			String slug = r.getChapterId() != null ? chapterSlugById.get(r.getChapterId()) : null;
			return new SubmissionSummary(
					r.getId(),
					r.getPassageId(),
					title,
					slug,
					r.getCorrectCount(),
					r.getTotalQuestions(),
					r.getCreatedAt());
		}).toList();
	}

	public Map<Long, PassageLastScore> lastScoresByPassage(Long userId, List<Long> passageIds) {
		if (passageIds == null || passageIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, PassageLastScore> result = new HashMap<>();
		for (Long passageId : passageIds) {
			ReadingSubmission latest = submissionMapper.selectOne(new LambdaQueryWrapper<ReadingSubmission>()
					.eq(ReadingSubmission::getUserId, userId)
					.eq(ReadingSubmission::getPassageId, passageId)
					.orderByDesc(ReadingSubmission::getCreatedAt)
					.last("LIMIT 1"));
			if (latest != null) {
				result.put(passageId, new PassageLastScore(latest.getCorrectCount(), latest.getTotalQuestions(), latest.getCreatedAt()));
			}
		}
		return result;
	}

	public record SubmissionSummary(
			Long submissionId,
			Long passageId,
			String passageTitle,
			String chapterSlug,
			int correctCount,
			int totalQuestions,
			java.time.LocalDateTime createdAt) {
	}

	public record PassageLastScore(int correctCount, int totalQuestions, java.time.LocalDateTime createdAt) {
	}
}
