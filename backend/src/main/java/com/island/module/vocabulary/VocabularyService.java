package com.island.module.vocabulary;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.common.BusinessException;
import com.island.module.vocabulary.dto.AddUserVocabularyRequest;
import com.island.module.vocabulary.dto.VocabReviewRequest;
import com.island.module.vocabulary.dto.NotebookReviewBatchRequest;
import com.island.module.vocabulary.dto.VocabSettingsRequest;
import com.island.module.vocabulary.mapper.UserVocabSettingsMapper;
import com.island.module.vocabulary.mapper.VocabDailyCheckinMapper;
import com.island.module.vocabulary.mapper.UserVocabReviewMapper;
import com.island.module.vocabulary.mapper.UserVocabularyMapper;
import com.island.module.vocabulary.mapper.VocabularyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyService {

	private static final int DAILY_LIMIT = 20;
	private static final int SEARCH_LIMIT = 20;

	private final VocabularyMapper vocabularyMapper;
	private final UserVocabReviewMapper reviewMapper;
	private final UserVocabularyMapper userVocabularyMapper;
	private final UserVocabSettingsMapper settingsMapper;
	private final VocabDailyCheckinMapper checkinMapper;
	private final ObjectMapper objectMapper;

	public VocabSettingsView getSettings(Long userId) {
		UserVocabSettings settings = settingsMapper.selectById(userId);
		return new VocabSettingsView(settings != null ? settings.getExamLevel() : "cet4");
	}

	@Transactional
	public VocabSettingsView updateSettings(Long userId, VocabSettingsRequest request) {
		String level = request.getExamLevel().trim().toLowerCase();
		if (!level.equals("cet4") && !level.equals("cet6")) {
			throw new BusinessException(400, "词书级别须为 cet4 或 cet6");
		}
		UserVocabSettings settings = settingsMapper.selectById(userId);
		if (settings == null) {
			settings = new UserVocabSettings();
			settings.setUserId(userId);
			settings.setExamLevel(level);
			settingsMapper.insert(settings);
		} else {
			settings.setExamLevel(level);
			settingsMapper.updateById(settings);
		}
		return new VocabSettingsView(level);
	}

	public CheckinStats getCheckinStats(Long userId) {
		LocalDate today = LocalDate.now();
		List<VocabDailyCheckin> rows = checkinMapper.selectList(new LambdaQueryWrapper<VocabDailyCheckin>()
				.eq(VocabDailyCheckin::getUserId, userId)
				.ge(VocabDailyCheckin::getCheckDate, today.minusDays(60))
				.orderByDesc(VocabDailyCheckin::getCheckDate));
		Set<LocalDate> checkedDates = rows.stream()
				.map(VocabDailyCheckin::getCheckDate)
				.collect(Collectors.toSet());

		int streak = 0;
		LocalDate cursor = checkedDates.contains(today) ? today : today.minusDays(1);
		while (checkedDates.contains(cursor)) {
			streak++;
			cursor = cursor.minusDays(1);
		}

		List<WeekDayCheckin> week = new ArrayList<>();
		for (int i = 6; i >= 0; i--) {
			LocalDate d = today.minusDays(i);
			week.add(new WeekDayCheckin(d.toString(), checkedDates.contains(d)));
		}
		return new CheckinStats(streak, week);
	}

	@Transactional
	public int addNotebookToReview(Long userId, NotebookReviewBatchRequest request) {
		LocalDateTime now = LocalDateTime.now();
		int added = 0;
		for (Long vocabularyId : request.getVocabularyIds()) {
			if (vocabularyId == null) {
				continue;
			}
			Vocabulary vocab = vocabularyMapper.selectById(vocabularyId);
			if (vocab == null) {
				continue;
			}
			UserVocabReview review = reviewMapper.selectOne(new LambdaQueryWrapper<UserVocabReview>()
					.eq(UserVocabReview::getUserId, userId)
					.eq(UserVocabReview::getVocabularyId, vocabularyId));
			if (review == null) {
				review = new UserVocabReview();
				review.setUserId(userId);
				review.setVocabularyId(vocabularyId);
				review.setFamiliarity(0);
				review.setNextReviewAt(now);
				review.setLastReviewAt(null);
				reviewMapper.insert(review);
				added++;
			} else if (review.getNextReviewAt().isAfter(now)) {
				review.setNextReviewAt(now);
				reviewMapper.updateById(review);
				added++;
			}
		}
		return added;
	}

	public TodayPlan getTodayPlan(Long userId) {
		String examLevel = resolveExamLevel(userId);
		ensureReviewQueue(userId, examLevel);
		LocalDateTime now = LocalDateTime.now();
		List<UserVocabReview> due = reviewMapper.selectList(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId)
				.le(UserVocabReview::getNextReviewAt, now)
				.orderByAsc(UserVocabReview::getNextReviewAt)
				.last("LIMIT " + DAILY_LIMIT));

		if (due.isEmpty()) {
			return new TodayPlan(0, DAILY_LIMIT, examLevel, List.of());
		}

		List<Long> vocabIds = due.stream().map(UserVocabReview::getVocabularyId).toList();
		Map<Long, Vocabulary> vocabMap = vocabularyMapper.selectBatchIds(vocabIds).stream()
				.collect(Collectors.toMap(Vocabulary::getId, Function.identity()));

		List<TodayItem> items = due.stream()
				.map(r -> {
					Vocabulary v = vocabMap.get(r.getVocabularyId());
					if (v == null) {
						return null;
					}
					return new TodayItem(toListItem(v), r.getFamiliarity());
				})
				.filter(item -> item != null)
				.toList();

		long remaining = reviewMapper.selectCount(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId)
				.le(UserVocabReview::getNextReviewAt, now));

		return new TodayPlan(items.size(), DAILY_LIMIT, examLevel, items);
	}

	public VocabListItem getDetail(Long id) {
		Vocabulary v = vocabularyMapper.selectById(id);
		if (v == null) {
			throw new BusinessException(404, "词汇不存在");
		}
		return toListItem(v);
	}

	public VocabDetail lookupByWord(String word) {
		if (word == null || word.isBlank()) {
			throw new BusinessException(400, "单词不能为空");
		}
		String normalized = word.trim().toLowerCase().replaceAll("[^a-z'-]", "");
		if (normalized.isEmpty()) {
			throw new BusinessException(400, "无效的单词");
		}
		Vocabulary v = vocabularyMapper.selectOne(new LambdaQueryWrapper<Vocabulary>()
				.apply("LOWER(word) = {0}", normalized)
				.last("LIMIT 1"));
		if (v == null) {
			throw new BusinessException(404, "词库中未找到该词");
		}
		return toDetail(v);
	}

	public VocabStats getStats(Long userId) {
		long reviewTotal = reviewMapper.selectCount(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId));
		long masteredCount = reviewMapper.selectCount(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId)
				.ge(UserVocabReview::getFamiliarity, 3));
		long notebookCount = userVocabularyMapper.selectCount(new LambdaQueryWrapper<UserVocabulary>()
				.eq(UserVocabulary::getUserId, userId));
		TodayPlan today = getTodayPlan(userId);
		LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
		long todayDone = reviewMapper.selectCount(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId)
				.ge(UserVocabReview::getLastReviewAt, startOfDay));
		return new VocabStats(
				(int) masteredCount,
				(int) reviewTotal,
				(int) notebookCount,
				(int) todayDone,
				DAILY_LIMIT,
				today.dueCount(),
				getCheckinStats(userId).streakDays(),
				resolveExamLevel(userId));
	}

	public VocabDetail getFullDetail(Long id) {
		Vocabulary v = vocabularyMapper.selectById(id);
		if (v == null) {
			throw new BusinessException(404, "词汇不存在");
		}
		return toDetail(v);
	}

	public List<VocabListItem> search(String query) {
		String q = query == null ? "" : query.trim().toLowerCase();
		if (q.length() < 1) {
			return List.of();
		}
		if (q.length() > 32) {
			q = q.substring(0, 32);
		}
		return vocabularyMapper.selectList(new LambdaQueryWrapper<Vocabulary>()
						.likeRight(Vocabulary::getWord, q)
						.orderByAsc(Vocabulary::getFreqRank)
						.last("LIMIT " + SEARCH_LIMIT))
				.stream()
				.map(this::toListItem)
				.toList();
	}

	@Transactional
	public ReviewResult review(Long userId, VocabReviewRequest request) {
		Vocabulary vocab = vocabularyMapper.selectById(request.getVocabularyId());
		if (vocab == null) {
			throw new BusinessException(404, "词汇不存在");
		}

		UserVocabReview review = reviewMapper.selectOne(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId)
				.eq(UserVocabReview::getVocabularyId, request.getVocabularyId()));

		LocalDateTime now = LocalDateTime.now();
		int familiarity;
		LocalDateTime next;

		switch (request.getResult()) {
			case "know" -> {
				familiarity = Math.min(5, (review != null ? review.getFamiliarity() : 0) + 1);
				next = now.plusDays(3);
			}
			case "vague" -> {
				familiarity = Math.max(0, (review != null ? review.getFamiliarity() : 0));
				next = now.plusDays(1);
			}
			case "unknown" -> {
				familiarity = 0;
				next = now;
			}
			default -> throw new BusinessException(400, "无效的复习结果");
		}

		if (review == null) {
			review = new UserVocabReview();
			review.setUserId(userId);
			review.setVocabularyId(request.getVocabularyId());
			review.setFamiliarity(familiarity);
			review.setNextReviewAt(next);
			review.setLastReviewAt(now);
			reviewMapper.insert(review);
		} else {
			review.setFamiliarity(familiarity);
			review.setNextReviewAt(next);
			review.setLastReviewAt(now);
			reviewMapper.updateById(review);
		}

		maybeRecordCheckin(userId);

		long daysUntil = Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), next.toLocalDate()));
		return new ReviewResult(request.getVocabularyId(), request.getResult(), familiarity, next, (int) daysUntil);
	}

	@Transactional
	public void addToNotebook(Long userId, AddUserVocabularyRequest request) {
		Vocabulary vocab = vocabularyMapper.selectById(request.getVocabularyId());
		if (vocab == null) {
			throw new BusinessException(404, "词汇不存在");
		}
		UserVocabulary existing = userVocabularyMapper.selectOne(new LambdaQueryWrapper<UserVocabulary>()
				.eq(UserVocabulary::getUserId, userId)
				.eq(UserVocabulary::getVocabularyId, request.getVocabularyId()));
		if (existing != null) {
			if (request.getNote() != null && !request.getNote().isBlank()) {
				existing.setNote(request.getNote().trim());
			}
			if (request.getSourceType() != null && !request.getSourceType().isBlank()) {
				existing.setSourceType(request.getSourceType().trim());
				existing.setSourceId(request.getSourceId());
				userVocabularyMapper.updateById(existing);
			}
			return;
		}
		UserVocabulary uv = new UserVocabulary();
		uv.setUserId(userId);
		uv.setVocabularyId(request.getVocabularyId());
		String sourceType = request.getSourceType();
		uv.setSourceType(sourceType != null && !sourceType.isBlank() ? sourceType.trim() : "manual");
		uv.setSourceId(request.getSourceId());
		uv.setNote(request.getNote());
		try {
			userVocabularyMapper.insert(uv);
		} catch (DuplicateKeyException e) {
			// concurrent insert
		}
	}

	public List<NotebookItem> listNotebook(Long userId) {
		List<UserVocabulary> rows = userVocabularyMapper.selectList(new LambdaQueryWrapper<UserVocabulary>()
				.eq(UserVocabulary::getUserId, userId)
				.orderByDesc(UserVocabulary::getCreatedAt)
				.last("LIMIT 100"));
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> ids = rows.stream().map(UserVocabulary::getVocabularyId).toList();
		Map<Long, Vocabulary> map = vocabularyMapper.selectBatchIds(ids).stream()
				.collect(Collectors.toMap(Vocabulary::getId, Function.identity()));
		return rows.stream()
				.map(uv -> {
					Vocabulary v = map.get(uv.getVocabularyId());
					if (v == null) {
						return null;
					}
					return new NotebookItem(toListItem(v), uv.getSourceType(), uv.getSourceId(), uv.getNote());
				})
				.filter(item -> item != null)
				.toList();
	}

	private void ensureReviewQueue(Long userId, String examLevel) {
		long total = reviewMapper.selectCount(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId));
		if (total >= DAILY_LIMIT) {
			return;
		}

		Set<Long> existing = reviewMapper.selectList(new LambdaQueryWrapper<UserVocabReview>()
						.eq(UserVocabReview::getUserId, userId))
				.stream()
				.map(UserVocabReview::getVocabularyId)
				.collect(Collectors.toSet());

		int need = DAILY_LIMIT - (int) total;
		List<Vocabulary> candidates = vocabularyMapper.selectList(new LambdaQueryWrapper<Vocabulary>()
				.eq(Vocabulary::getDifficulty, examLevel)
				.orderByAsc(Vocabulary::getFreqRank)
				.last("LIMIT " + (need + existing.size() + 50)));

		LocalDateTime now = LocalDateTime.now();
		int added = 0;
		for (Vocabulary v : candidates) {
			if (existing.contains(v.getId())) {
				continue;
			}
			UserVocabReview r = new UserVocabReview();
			r.setUserId(userId);
			r.setVocabularyId(v.getId());
			r.setFamiliarity(0);
			r.setNextReviewAt(now);
			r.setLastReviewAt(null);
			reviewMapper.insert(r);
			existing.add(v.getId());
			added++;
			if (added >= need) {
				break;
			}
		}
	}

	private String resolveExamLevel(Long userId) {
		UserVocabSettings settings = settingsMapper.selectById(userId);
		if (settings == null || settings.getExamLevel() == null) {
			return "cet4";
		}
		return settings.getExamLevel();
	}

	private void maybeRecordCheckin(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		long due = reviewMapper.selectCount(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId)
				.le(UserVocabReview::getNextReviewAt, now));
		if (due > 0) {
			return;
		}
		LocalDate today = LocalDate.now();
		LocalDateTime startOfDay = today.atStartOfDay();
		long reviewedToday = reviewMapper.selectCount(new LambdaQueryWrapper<UserVocabReview>()
				.eq(UserVocabReview::getUserId, userId)
				.ge(UserVocabReview::getLastReviewAt, startOfDay));
		if (reviewedToday < 1) {
			return;
		}
		VocabDailyCheckin checkin = checkinMapper.selectOne(new LambdaQueryWrapper<VocabDailyCheckin>()
				.eq(VocabDailyCheckin::getUserId, userId)
				.eq(VocabDailyCheckin::getCheckDate, today));
		if (checkin == null) {
			checkin = new VocabDailyCheckin();
			checkin.setUserId(userId);
			checkin.setCheckDate(today);
			checkin.setReviewedCount((int) reviewedToday);
			checkinMapper.insert(checkin);
		} else if (checkin.getReviewedCount() < reviewedToday) {
			checkin.setReviewedCount((int) reviewedToday);
			checkinMapper.updateById(checkin);
		}
	}

	private VocabListItem toListItem(Vocabulary v) {
		String brief = briefMeaning(v.getMeaningZh());
		return new VocabListItem(
				v.getId(),
				v.getWord(),
				v.getPhonetic(),
				v.getPartOfSpeech(),
				brief,
				v.getDifficulty(),
				v.getFreqRank());
	}

	private VocabDetail toDetail(Vocabulary v) {
		return new VocabDetail(
				toListItem(v),
				v.getMeaningZh(),
				v.getExampleEn(),
				v.getExampleZh(),
				v.getCollocation(),
				parsePhrases(v.getPhrasesJson()));
	}

	private static String briefMeaning(String meaning) {
		if (meaning == null || meaning.isBlank()) {
			return "—";
		}
		String s = meaning.trim();
		int sep = s.indexOf('；');
		if (sep < 0) {
			sep = s.indexOf(';');
		}
		if (sep > 0 && sep < 80) {
			return s.substring(0, sep);
		}
		return s.length() > 80 ? s.substring(0, 80) + "…" : s;
	}

	private List<PhraseItem> parsePhrases(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<PhraseItem>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	public record VocabListItem(
			Long id,
			String word,
			String phonetic,
			String partOfSpeech,
			String meaningBrief,
			String examLevel,
			Integer freqRank) {
	}

	public record PhraseItem(String en, String zh) {
	}

	public record VocabDetail(
			VocabListItem summary,
			String meaningZh,
			String exampleEn,
			String exampleZh,
			String collocation,
			List<PhraseItem> phrases) {
	}

	public record TodayItem(VocabListItem vocab, int familiarity) {
	}

	public record TodayPlan(int dueCount, int dailyLimit, String examLevel, List<TodayItem> items) {
	}

	public record VocabSettingsView(String examLevel) {
	}

	public record WeekDayCheckin(String date, boolean checked) {
	}

	public record CheckinStats(int streakDays, List<WeekDayCheckin> week) {
	}

	public record VocabStats(
			int masteredCount,
			int reviewTotal,
			int notebookCount,
			int todayDone,
			int dailyLimit,
			int todayRemaining,
			int streakDays,
			String examLevel) {
	}

	public record NotebookItem(
			VocabListItem vocab,
			String sourceType,
			Long sourceId,
			String note) {
	}

	public record ReviewResult(
			Long vocabularyId,
			String result,
			int familiarity,
			LocalDateTime nextReviewAt,
			int daysUntilNext) {
	}
}
