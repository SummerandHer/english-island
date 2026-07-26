package com.island.module.learn;

import com.island.module.reading.ReadingSubmissionService;
import com.island.module.translation.TranslationService;
import com.island.module.vocabulary.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnService {

	private final VocabularyService vocabularyService;
	private final ReadingSubmissionService readingSubmissionService;
	private final TranslationService translationService;

	public LearnSummary getSummary(Long userId) {
		VocabularyService.VocabStats vocabStats = vocabularyService.getStats(userId);
		VocabularyService.CheckinStats checkinStats = vocabularyService.getCheckinStats(userId);

		ReadingSubmissionService.SubmissionSummary readingLatest = readingSubmissionService.getLatest(userId);
		int readingWeekCount = readingSubmissionService.countThisWeek(userId);
		long readingTotalCount = readingSubmissionService.countTotal(userId);

		List<TranslationService.SubmissionSummary> translationSubs = translationService.listMySubmissions(userId, 1);
		TranslationService.SubmissionSummary translationLatest = translationSubs.isEmpty() ? null : translationSubs.get(0);
		int translationWeekCount = translationService.countThisWeek(userId);

		List<SuggestedAction> suggestions = buildSuggestions(
				vocabStats, readingTotalCount, translationWeekCount);

		return new LearnSummary(
				VocabSection.from(vocabStats),
				CheckinSection.from(checkinStats),
				ReadingSection.from(readingLatest, readingWeekCount),
				TranslationSection.from(translationLatest, translationWeekCount),
				suggestions);
	}

	private List<SuggestedAction> buildSuggestions(
			VocabularyService.VocabStats vocabStats,
			long readingTotalCount,
			int translationWeekCount) {
		List<SuggestedAction> actions = new ArrayList<>();
		if (vocabStats.todayRemaining() > 0) {
			actions.add(new SuggestedAction(
					"vocab",
					"今日还有 " + vocabStats.todayRemaining() + " 个词待复习",
					"/vocabulary"));
		}
		if (readingTotalCount == 0) {
			actions.add(new SuggestedAction(
					"reading",
					"开始第一篇阅读模拟",
					"/reading"));
		}
		if (translationWeekCount == 0) {
			actions.add(new SuggestedAction(
					"translation",
					"本周尚未完成翻译练习",
					"/translation"));
		}
		return actions;
	}

	public record LearnSummary(
			VocabSection vocabulary,
			CheckinSection checkin,
			ReadingSection reading,
			TranslationSection translation,
			List<SuggestedAction> suggestedActions) {
	}

	public record VocabSection(
			int masteredCount,
			int reviewTotal,
			int notebookCount,
			int todayDone,
			int dailyLimit,
			int todayRemaining,
			int streakDays,
			String examLevel) {
		static VocabSection from(VocabularyService.VocabStats stats) {
			return new VocabSection(
					stats.masteredCount(),
					stats.reviewTotal(),
					stats.notebookCount(),
					stats.todayDone(),
					stats.dailyLimit(),
					stats.todayRemaining(),
					stats.streakDays(),
					stats.examLevel());
		}
	}

	public record CheckinSection(
			int streakDays,
			List<VocabularyService.WeekDayCheckin> week) {
		static CheckinSection from(VocabularyService.CheckinStats stats) {
			return new CheckinSection(stats.streakDays(), stats.week());
		}
	}

	public record ReadingSection(
			ReadingSubmissionService.SubmissionSummary latest,
			int weekCount) {
		static ReadingSection from(ReadingSubmissionService.SubmissionSummary latest, int weekCount) {
			return new ReadingSection(latest, weekCount);
		}
	}

	public record TranslationSection(
			TranslationLatest latest,
			int weekCount) {
		static TranslationSection from(TranslationService.SubmissionSummary latest, int weekCount) {
			return new TranslationSection(
					latest != null ? TranslationLatest.from(latest) : null,
					weekCount);
		}
	}

	public record TranslationLatest(
			Long submissionId,
			Long questionId,
			String promptPreview,
			String direction,
			int score,
			int cetScore,
			String band,
			String bandLabel,
			LocalDateTime createdAt) {
		static TranslationLatest from(TranslationService.SubmissionSummary sub) {
			return new TranslationLatest(
					sub.submissionId(),
					sub.questionId(),
					sub.promptPreview(),
					sub.direction(),
					sub.score(),
					sub.cetScore(),
					sub.band(),
					sub.bandLabel(),
					sub.createdAt());
		}
	}

	public record SuggestedAction(String type, String label, String path) {
	}
}
