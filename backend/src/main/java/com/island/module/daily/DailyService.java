package com.island.module.daily;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.common.BusinessException;
import com.island.module.admin.dto.AdminDailyArticleRequest;
import com.island.module.daily.dto.DailyAiEnrichmentResult;
import com.island.module.daily.dto.DailyAnnotationRequest;
import com.island.module.daily.dto.DailyCheckinRequest;
import com.island.module.daily.mapper.DailyAnnotationMapper;
import com.island.module.daily.mapper.DailyArticleMapper;
import com.island.module.daily.mapper.DailyCheckinMapper;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DailyService {

	private static final Set<String> COLORS = Set.of("moss", "amber", "sky");
	private static final Pattern SLUG_SAFE = Pattern.compile("[^a-z0-9]+");
	private static final Set<DayOfWeek> PUBLISH_DAYS = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

	private final DailyArticleMapper articleMapper;
	private final DailyCheckinMapper checkinMapper;
	private final DailyAnnotationMapper annotationMapper;
	private final DailyAiParseService aiParseService;
	private final ObjectMapper objectMapper;

	public List<Map<String, String>> topics() {
		return DailyTopics.catalog();
	}

	public HubPayload hub(String topic, IslandUserDetails user) {
		LocalDate today = LocalDate.now();
		LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate wednesday = monday.plusDays(2);
		LocalDate friday = monday.plusDays(4);

		LambdaQueryWrapper<DailyArticle> qw = new LambdaQueryWrapper<DailyArticle>()
				.eq(DailyArticle::getStatus, "published")
				.orderByDesc(DailyArticle::getPublishDate)
				.orderByDesc(DailyArticle::getId);
		if (StringUtils.hasText(topic) && DailyTopics.isValid(topic)) {
			qw.eq(DailyArticle::getTopic, topic);
		}
		List<DailyArticle> published = articleMapper.selectList(qw);

		Set<Long> checkedIds = new HashSet<>();
		int streakDays = 0;
		if (user != null) {
			Long userId = user.getUser().getId();
			List<DailyCheckin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<DailyCheckin>()
					.eq(DailyCheckin::getUserId, userId));
			for (DailyCheckin c : checkins) {
				checkedIds.add(c.getArticleId());
			}
			streakDays = computeStreak(checkins);
		}

		List<ArticleSummary> weekSlots = List.of(
				slotForDate(monday, published, checkedIds, today),
				slotForDate(wednesday, published, checkedIds, today),
				slotForDate(friday, published, checkedIds, today)
		);

		List<ArticleSummary> archive = published.stream()
				.filter(a -> !a.getPublishDate().isAfter(today))
				.limit(30)
				.map(a -> toSummary(a, checkedIds.contains(a.getId()), !a.getPublishDate().isAfter(today)))
				.toList();

		ArticleSummary todayArticle = weekSlots.stream()
				.filter(s -> s != null && s.publishDate() != null && s.publishDate().equals(today) && s.unlocked())
				.findFirst()
				.orElse(weekSlots.stream()
						.filter(s -> s != null && s.unlocked() && !s.checkedIn())
						.findFirst()
						.orElse(null));

		return new HubPayload(weekSlots, archive, streakDays, todayArticle, DailyTopics.catalog());
	}

	public ArticleDetail getPublishedDetail(Long id, IslandUserDetails user) {
		DailyArticle article = requirePublishedVisible(id);
		boolean checkedIn = false;
		List<AnnotationView> annotations = List.of();
		if (user != null) {
			Long userId = user.getUser().getId();
			checkedIn = checkinMapper.selectCount(new LambdaQueryWrapper<DailyCheckin>()
					.eq(DailyCheckin::getUserId, userId)
					.eq(DailyCheckin::getArticleId, id)) > 0;
			annotations = annotationMapper.selectList(new LambdaQueryWrapper<DailyAnnotation>()
							.eq(DailyAnnotation::getUserId, userId)
							.eq(DailyAnnotation::getArticleId, id)
							.orderByAsc(DailyAnnotation::getStartOffset))
					.stream()
					.map(this::toAnnotationView)
					.toList();
		}
		List<ArticleSummary> related = articleMapper.selectList(new LambdaQueryWrapper<DailyArticle>()
						.eq(DailyArticle::getStatus, "published")
						.eq(DailyArticle::getTopic, article.getTopic())
						.ne(DailyArticle::getId, id)
						.le(DailyArticle::getPublishDate, LocalDate.now())
						.orderByDesc(DailyArticle::getPublishDate)
						.last("LIMIT 5"))
				.stream()
				.map(a -> toSummary(a, false, true))
				.toList();
		return toDetail(article, checkedIn, annotations, related);
	}

	@Transactional
	public void checkin(Long articleId, Long userId, DailyCheckinRequest request) {
		DailyArticle article = requirePublishedVisible(articleId);
		if (!article.getPublishDate().equals(LocalDate.now())
				&& article.getPublishDate().isAfter(LocalDate.now())) {
			throw new BusinessException(400, "该日报尚未开放打卡");
		}
		Long exists = checkinMapper.selectCount(new LambdaQueryWrapper<DailyCheckin>()
				.eq(DailyCheckin::getUserId, userId)
				.eq(DailyCheckin::getArticleId, articleId));
		if (exists != null && exists > 0) {
			return;
		}
		DailyCheckin row = new DailyCheckin();
		row.setUserId(userId);
		row.setArticleId(articleId);
		row.setCheckDate(LocalDate.now());
		row.setReadSeconds(request != null ? Math.max(0, request.getReadSeconds()) : 0);
		checkinMapper.insert(row);
	}

	public List<AnnotationView> listAnnotations(Long articleId, Long userId) {
		requirePublishedVisible(articleId);
		return annotationMapper.selectList(new LambdaQueryWrapper<DailyAnnotation>()
						.eq(DailyAnnotation::getUserId, userId)
						.eq(DailyAnnotation::getArticleId, articleId)
						.orderByAsc(DailyAnnotation::getStartOffset))
				.stream()
				.map(this::toAnnotationView)
				.toList();
	}

	@Transactional
	public AnnotationView createAnnotation(Long articleId, Long userId, DailyAnnotationRequest request) {
		DailyArticle article = requirePublishedVisible(articleId);
		validateAnnotation(article.getContentEn(), request);
		DailyAnnotation row = new DailyAnnotation();
		row.setUserId(userId);
		row.setArticleId(articleId);
		row.setStartOffset(request.getStartOffset());
		row.setEndOffset(request.getEndOffset());
		row.setSelectedText(request.getSelectedText().trim());
		row.setColor(normalizeColor(request.getColor()));
		row.setNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
		annotationMapper.insert(row);
		return toAnnotationView(row);
	}

	@Transactional
	public void deleteAnnotation(Long articleId, Long annotationId, Long userId) {
		DailyAnnotation row = annotationMapper.selectById(annotationId);
		if (row == null || !row.getArticleId().equals(articleId) || !row.getUserId().equals(userId)) {
			throw new BusinessException(404, "标注不存在");
		}
		annotationMapper.deleteById(annotationId);
	}

	public List<AdminArticleSummary> adminList(String status) {
		LambdaQueryWrapper<DailyArticle> qw = new LambdaQueryWrapper<DailyArticle>()
				.orderByDesc(DailyArticle::getPublishDate)
				.orderByDesc(DailyArticle::getId);
		if (StringUtils.hasText(status)) {
			qw.eq(DailyArticle::getStatus, status.trim());
		}
		return articleMapper.selectList(qw).stream().map(this::toAdminSummary).toList();
	}

	public AdminArticleDetail adminGet(Long id) {
		DailyArticle a = articleMapper.selectById(id);
		if (a == null) {
			throw new BusinessException(404, "日报不存在");
		}
		return toAdminDetail(a);
	}

	@Transactional
	public Long adminCreate(AdminDailyArticleRequest request) {
		DailyArticle a = new DailyArticle();
		applyAdminRequest(a, request, true);
		articleMapper.insert(a);
		return a.getId();
	}

	@Transactional
	public void adminUpdate(Long id, AdminDailyArticleRequest request) {
		DailyArticle a = articleMapper.selectById(id);
		if (a == null) {
			throw new BusinessException(404, "日报不存在");
		}
		applyAdminRequest(a, request, false);
		articleMapper.updateById(a);
	}

	@Transactional
	public void adminSetStatus(Long id, String status) {
		DailyArticle a = articleMapper.selectById(id);
		if (a == null) {
			throw new BusinessException(404, "日报不存在");
		}
		String next = normalizeStatus(status);
		if ("ready".equals(next) || "published".equals(next)) {
			assertEnrichmentComplete(a);
		}
		if ("published".equals(next)) {
			assertNoPublishCollision(a.getPublishDate(), a.getId());
		}
		a.setStatus(next);
		articleMapper.updateById(a);
	}

	public DailyAiEnrichmentResult adminAiEnrich(String title, String contentEn) {
		if (!StringUtils.hasText(contentEn)) {
			throw new BusinessException(400, "正文不能为空");
		}
		return aiParseService.enrich(title, contentEn);
	}

	/** 兼容旧端点 */
	public Map<String, Object> adminAiParse(String title, String contentEn, String topic, String difficulty) {
		DailyAiEnrichmentResult r = adminAiEnrich(title, contentEn);
		Map<String, Object> out = new java.util.LinkedHashMap<>();
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
		out.put("contentZh", r.contentZh());
		out.put("sentencesJson", r.sentencesJson());
		out.put("aiRawJson", r.aiRawJson());
		out.put("cetVocab", r.cetVocab());
		out.put("hardVocab", r.hardVocab());
		out.put("structures", r.structures());
		out.put("sentences", r.sentences());
		return out;
	}

	@Transactional
	public DailyAiEnrichmentResult adminAiEnrichArticle(Long id) {
		DailyArticle a = articleMapper.selectById(id);
		if (a == null) {
			throw new BusinessException(404, "日报不存在");
		}
		a.setAiStatus("running");
		a.setAiError(null);
		articleMapper.updateById(a);

		DailyAiEnrichmentResult result = aiParseService.enrich(a.getTitle(), a.getContentEn());
		a.setAiVersion(result.aiVersion());
		a.setAiRawJson(result.aiRawJson());
		if (result.isHardFail()) {
			a.setAiStatus("failed");
			a.setAiError(result.error());
			articleMapper.updateById(a);
			return result;
		}
		a.setAiStatus(result.gate());
		a.setAiError(null);
		a.setSummaryZh(result.summaryZh());
		a.setTopic(result.topic());
		a.setDifficulty(result.difficulty());
		a.setWordCount(result.wordCount());
		a.setCetVocabJson(result.cetVocabJson());
		a.setHardVocabJson(result.hardVocabJson());
		a.setStructuresJson(result.structuresJson());
		a.setContentZh(result.contentZh());
		a.setSentencesJson(result.sentencesJson());
		articleMapper.updateById(a);
		return result;
	}

	private void applyAdminRequest(DailyArticle a, AdminDailyArticleRequest request, boolean creating) {
		validatePublishDay(request.getPublishDate());
		if (!StringUtils.hasText(request.getCoverUrl()) && request.getCoverAssetId() == null) {
			throw new BusinessException(400, "请填写封面图 URL");
		}
		if (!StringUtils.hasText(request.getSourceAuthor())) {
			throw new BusinessException(400, "请填写作者");
		}
		if (!StringUtils.hasText(request.getSourcePlace())) {
			throw new BusinessException(400, "请填写地点/媒体语境");
		}
		if (request.getSourcePublishedAt() == null) {
			throw new BusinessException(400, "请填写文章出处时间");
		}

		String status = normalizeStatus(request.getStatus() != null
				? request.getStatus()
				: (creating ? "draft" : a.getStatus()));

		String topic = StringUtils.hasText(request.getTopic()) ? request.getTopic().trim() : null;
		String difficulty = StringUtils.hasText(request.getDifficulty()) ? request.getDifficulty().trim() : null;

		if ("draft".equals(status)) {
			if (topic != null && !DailyTopics.isValid(topic)) {
				throw new BusinessException(400, "无效主题");
			}
			if (difficulty != null && !"cet4".equals(difficulty) && !"cet6".equals(difficulty)) {
				throw new BusinessException(400, "难度须为 cet4 或 cet6");
			}
			if (topic == null) {
				topic = creating || !StringUtils.hasText(a.getTopic()) ? "education" : a.getTopic();
			}
			if (difficulty == null) {
				difficulty = creating || !StringUtils.hasText(a.getDifficulty()) ? "cet4" : a.getDifficulty();
			}
		} else {
			if (!DailyTopics.isValid(topic)) {
				throw new BusinessException(400, "待发/发布前须有有效主题（请先 AI 增强）");
			}
			if (!"cet4".equals(difficulty) && !"cet6".equals(difficulty)) {
				throw new BusinessException(400, "待发/发布前须有有效难度（请先 AI 增强）");
			}
		}

		a.setTitle(request.getTitle().trim());
		a.setTopic(topic);
		a.setDifficulty(difficulty);
		a.setContentEn(request.getContentEn());
		a.setCoverUrl(blankToNull(request.getCoverUrl()));
		a.setCoverAssetId(request.getCoverAssetId());
		a.setSummaryZh(blankToNull(request.getSummaryZh()));
		a.setPublishDate(request.getPublishDate());
		a.setSourceId(request.getSourceId());
		a.setSourcePublishedAt(request.getSourcePublishedAt());
		a.setSourceAuthor(blankToNull(request.getSourceAuthor()));
		a.setSourcePlace(blankToNull(request.getSourcePlace()));
		a.setCetVocabJson(blankToNull(request.getCetVocabJson()));
		a.setHardVocabJson(blankToNull(request.getHardVocabJson()));
		a.setStructuresJson(blankToNull(request.getStructuresJson()));
		a.setContentZh(blankToNull(request.getContentZh()));
		a.setSentencesJson(blankToNull(request.getSentencesJson()));
		int wc = request.getWordCount() != null && request.getWordCount() > 0
				? request.getWordCount()
				: DailyAiParseService.countWords(request.getContentEn());
		a.setWordCount(wc);
		a.setStatus(status);

		if ("ready".equals(status) || "published".equals(status)) {
			assertEnrichmentComplete(a);
		}
		if ("published".equals(status)) {
			assertNoPublishCollision(request.getPublishDate(), creating ? null : a.getId());
		}

		String slug = StringUtils.hasText(request.getSlug()) ? slugify(request.getSlug()) : slugify(request.getTitle());
		if (creating) {
			a.setSlug(uniqueSlug(slug, null));
			if (!StringUtils.hasText(a.getAiStatus())) {
				a.setAiStatus("idle");
			}
		} else {
			a.setSlug(uniqueSlug(slug, a.getId()));
		}
	}

	private void assertEnrichmentComplete(DailyArticle a) {
		if (!StringUtils.hasText(a.getSummaryZh())) {
			throw new BusinessException(400, "缺少中文摘要，请先 AI 增强");
		}
		if (!StringUtils.hasText(a.getCetVocabJson()) || "[]".equals(a.getCetVocabJson().trim())) {
			throw new BusinessException(400, "缺少高频词，请先 AI 增强");
		}
		if (!StringUtils.hasText(a.getHardVocabJson()) || "[]".equals(a.getHardVocabJson().trim())) {
			throw new BusinessException(400, "缺少难词，请先 AI 增强");
		}
		if (!StringUtils.hasText(a.getStructuresJson()) || "[]".equals(a.getStructuresJson().trim())) {
			throw new BusinessException(400, "缺少句式结构，请先 AI 增强");
		}
		List<Map<String, Object>> structures = parseList(a.getStructuresJson());
		if (structures.size() < 3) {
			throw new BusinessException(400, "句式结构至少 3 条");
		}
		if (!StringUtils.hasText(a.getSentencesJson()) || "[]".equals(a.getSentencesJson().trim())) {
			throw new BusinessException(400, "缺少逐句中译，请先 AI 增强");
		}
	}

	private void assertNoPublishCollision(LocalDate publishDate, Long excludeId) {
		LambdaQueryWrapper<DailyArticle> qw = new LambdaQueryWrapper<DailyArticle>()
				.eq(DailyArticle::getPublishDate, publishDate)
				.eq(DailyArticle::getStatus, "published");
		if (excludeId != null) {
			qw.ne(DailyArticle::getId, excludeId);
		}
		Long count = articleMapper.selectCount(qw);
		if (count != null && count > 0) {
			throw new BusinessException(400, "该排期日已有已发布日报，请换一天或先下架冲突篇");
		}
	}

	private void validatePublishDay(LocalDate date) {
		if (date == null || !PUBLISH_DAYS.contains(date.getDayOfWeek())) {
			throw new BusinessException(400, "发布日须为周一、周三或周五");
		}
	}

	private DailyArticle requirePublishedVisible(Long id) {
		DailyArticle article = articleMapper.selectById(id);
		if (article == null || !"published".equals(article.getStatus())) {
			throw new BusinessException(404, "日报不存在或未发布");
		}
		if (article.getPublishDate().isAfter(LocalDate.now())) {
			throw new BusinessException(403, "该日报尚未开放");
		}
		return article;
	}

	private void validateAnnotation(String content, DailyAnnotationRequest request) {
		if (request.getEndOffset() <= request.getStartOffset()) {
			throw new BusinessException(400, "标注范围无效");
		}
		if (content == null || request.getEndOffset() > content.length()) {
			throw new BusinessException(400, "标注超出正文范围");
		}
		String slice = content.substring(request.getStartOffset(), request.getEndOffset());
		if (!slice.equals(request.getSelectedText())) {
			throw new BusinessException(400, "标注文本与正文不一致");
		}
	}

	private String normalizeColor(String color) {
		if (!StringUtils.hasText(color)) {
			return "moss";
		}
		String c = color.trim().toLowerCase(Locale.ROOT);
		return COLORS.contains(c) ? c : "moss";
	}

	private String normalizeStatus(String status) {
		String s = status == null ? "draft" : status.trim().toLowerCase(Locale.ROOT);
		if (!Set.of("draft", "ready", "published").contains(s)) {
			throw new BusinessException(400, "状态须为 draft / ready / published");
		}
		return s;
	}

	private ArticleSummary slotForDate(
			LocalDate date,
			List<DailyArticle> published,
			Set<Long> checkedIds,
			LocalDate today) {
		DailyArticle match = published.stream()
				.filter(a -> date.equals(a.getPublishDate()))
				.findFirst()
				.orElse(null);
		boolean unlocked = !date.isAfter(today);
		if (match == null) {
			return new ArticleSummary(
					null, null, null, null, null, null, null, date, null, false, unlocked, weekdayLabel(date));
		}
		return toSummary(match, checkedIds.contains(match.getId()), unlocked);
	}

	private ArticleSummary toSummary(DailyArticle a, boolean checkedIn, boolean unlocked) {
		return new ArticleSummary(
				a.getId(),
				a.getTitle(),
				a.getSlug(),
				a.getTopic(),
				DailyTopics.label(a.getTopic()),
				a.getDifficulty(),
				a.getCoverUrl(),
				a.getPublishDate(),
				a.getWordCount(),
				checkedIn,
				unlocked,
				weekdayLabel(a.getPublishDate())
		);
	}

	private ArticleDetail toDetail(
			DailyArticle a,
			boolean checkedIn,
			List<AnnotationView> annotations,
			List<ArticleSummary> related) {
		return new ArticleDetail(
				a.getId(),
				a.getTitle(),
				a.getSlug(),
				a.getTopic(),
				DailyTopics.label(a.getTopic()),
				a.getDifficulty(),
				a.getContentEn(),
				a.getContentZh(),
				a.getCoverUrl(),
				a.getSummaryZh(),
				a.getPublishDate(),
				a.getSourcePublishedAt(),
				a.getSourceAuthor(),
				a.getSourcePlace(),
				a.getWordCount(),
				parseList(a.getCetVocabJson()),
				parseList(a.getHardVocabJson()),
				parseList(a.getStructuresJson()),
				parseList(a.getSentencesJson()),
				checkedIn,
				annotations,
				related
		);
	}

	private AdminArticleSummary toAdminSummary(DailyArticle a) {
		return new AdminArticleSummary(
				a.getId(), a.getTitle(), a.getSlug(), a.getTopic(), a.getDifficulty(),
				a.getPublishDate(), a.getStatus(), a.getWordCount(), a.getCoverUrl());
	}

	private AdminArticleDetail toAdminDetail(DailyArticle a) {
		return new AdminArticleDetail(
				a.getId(), a.getTitle(), a.getSlug(), a.getTopic(), a.getDifficulty(),
				a.getContentEn(), a.getContentZh(), a.getCoverUrl(), a.getCoverAssetId(), a.getSummaryZh(),
				a.getPublishDate(), a.getSourceId(), a.getSourcePublishedAt(),
				a.getSourceAuthor(), a.getSourcePlace(),
				a.getCetVocabJson(), a.getHardVocabJson(), a.getStructuresJson(), a.getSentencesJson(),
				a.getWordCount(), a.getStatus(),
				a.getAiStatus(), a.getAiError(), a.getAiVersion()
		);
	}

	private AnnotationView toAnnotationView(DailyAnnotation a) {
		return new AnnotationView(
				a.getId(), a.getStartOffset(), a.getEndOffset(),
				a.getSelectedText(), a.getColor(), a.getNote());
	}

	private List<Map<String, Object>> parseList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private int computeStreak(List<DailyCheckin> checkins) {
		if (checkins == null || checkins.isEmpty()) {
			return 0;
		}
		Set<LocalDate> dates = new HashSet<>();
		for (DailyCheckin c : checkins) {
			dates.add(c.getCheckDate());
		}
		// Streak by consecutive publish slots (Mon/Wed/Fri) with check-in
		LocalDate cursor = LocalDate.now();
		while (!PUBLISH_DAYS.contains(cursor.getDayOfWeek())) {
			cursor = cursor.minusDays(1);
		}
		int streak = 0;
		while (true) {
			if (!dates.contains(cursor)) {
				// allow missing today if today is publish day and not yet checked
				if (streak == 0 && cursor.equals(LocalDate.now())) {
					cursor = previousPublishDay(cursor);
					continue;
				}
				break;
			}
			streak++;
			cursor = previousPublishDay(cursor);
		}
		return streak;
	}

	private LocalDate previousPublishDay(LocalDate date) {
		LocalDate d = date.minusDays(1);
		while (!PUBLISH_DAYS.contains(d.getDayOfWeek())) {
			d = d.minusDays(1);
		}
		return d;
	}

	private String weekdayLabel(LocalDate date) {
		if (date == null) {
			return "";
		}
		return switch (date.getDayOfWeek()) {
			case MONDAY -> "周一";
			case WEDNESDAY -> "周三";
			case FRIDAY -> "周五";
			default -> date.getDayOfWeek().name();
		};
	}

	private String slugify(String input) {
		String s = input.trim().toLowerCase(Locale.ROOT);
		s = SLUG_SAFE.matcher(s).replaceAll("-");
		s = s.replaceAll("^-+|-+$", "");
		if (!StringUtils.hasText(s)) {
			s = "daily-" + System.currentTimeMillis();
		}
		if (s.length() > 100) {
			s = s.substring(0, 100);
		}
		return s;
	}

	private String uniqueSlug(String base, Long excludeId) {
		String candidate = base;
		int i = 2;
		while (true) {
			LambdaQueryWrapper<DailyArticle> qw = new LambdaQueryWrapper<DailyArticle>()
					.eq(DailyArticle::getSlug, candidate);
			if (excludeId != null) {
				qw.ne(DailyArticle::getId, excludeId);
			}
			Long count = articleMapper.selectCount(qw);
			if (count == null || count == 0) {
				return candidate;
			}
			candidate = base + "-" + i;
			i++;
		}
	}

	private static String blankToNull(String s) {
		return StringUtils.hasText(s) ? s.trim() : null;
	}

	public record HubPayload(
			List<ArticleSummary> weekSlots,
			List<ArticleSummary> archive,
			int streakDays,
			ArticleSummary todayArticle,
			List<Map<String, String>> topics
	) {}

	public record ArticleSummary(
			Long id,
			String title,
			String slug,
			String topic,
			String topicLabel,
			String difficulty,
			String coverUrl,
			LocalDate publishDate,
			Integer wordCount,
			boolean checkedIn,
			boolean unlocked,
			String weekdayLabel
	) {}

	public record ArticleDetail(
			Long id,
			String title,
			String slug,
			String topic,
			String topicLabel,
			String difficulty,
			String contentEn,
			String contentZh,
			String coverUrl,
			String summaryZh,
			LocalDate publishDate,
			LocalDate sourcePublishedAt,
			String sourceAuthor,
			String sourcePlace,
			Integer wordCount,
			List<Map<String, Object>> cetVocab,
			List<Map<String, Object>> hardVocab,
			List<Map<String, Object>> structures,
			List<Map<String, Object>> sentences,
			boolean checkedIn,
			List<AnnotationView> annotations,
			List<ArticleSummary> related
	) {}

	public record AnnotationView(
			Long id,
			int startOffset,
			int endOffset,
			String selectedText,
			String color,
			String note
	) {}

	public record AdminArticleSummary(
			Long id,
			String title,
			String slug,
			String topic,
			String difficulty,
			LocalDate publishDate,
			String status,
			Integer wordCount,
			String coverUrl
	) {}

	public record AdminArticleDetail(
			Long id,
			String title,
			String slug,
			String topic,
			String difficulty,
			String contentEn,
			String contentZh,
			String coverUrl,
			Long coverAssetId,
			String summaryZh,
			LocalDate publishDate,
			Integer sourceId,
			LocalDate sourcePublishedAt,
			String sourceAuthor,
			String sourcePlace,
			String cetVocabJson,
			String hardVocabJson,
			String structuresJson,
			String sentencesJson,
			Integer wordCount,
			String status,
			String aiStatus,
			String aiError,
			String aiVersion
	) {}
}
