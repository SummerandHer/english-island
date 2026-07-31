package com.island.module.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.island.common.BusinessException;
import com.island.common.PageResult;
import com.island.module.video.mapper.UserVideoProgressMapper;
import com.island.module.video.mapper.VideoFavoriteMapper;
import com.island.module.video.mapper.VideoMapper;
import com.island.module.video.mapper.VideoSentenceMapper;
import com.island.module.video.mapper.VideoTagMapper;
import com.island.module.video.mapper.VideoTagRelMapper;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

	private final VideoMapper videoMapper;
	private final VideoSentenceMapper sentenceMapper;
	private final VideoFavoriteMapper favoriteMapper;
	private final VideoTagMapper videoTagMapper;
	private final VideoTagRelMapper videoTagRelMapper;
	private final UserVideoProgressMapper progressMapper;

	public PageResult<VideoSummary> listVideos(
			int page,
			int size,
			Long userId,
			String tagSlug,
			String filter
	) {
		int safePage = Math.max(page, 1);
		int safeSize = Math.min(Math.max(size, 1), 50);
		String safeFilter = normalizeFilter(filter);

		Set<Long> scopeIds = resolveScopeVideoIds(userId, tagSlug, safeFilter);
		if (scopeIds != null && scopeIds.isEmpty()) {
			return new PageResult<>(List.of(), 0, safePage, safeSize);
		}

		LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
				.eq(Video::getStatus, 1);
		if (scopeIds != null) {
			wrapper.in(Video::getId, scopeIds);
		}
		if ("history".equals(safeFilter) && userId != null) {
			List<Long> historyOrder = listHistoryVideoIds(userId);
			if (historyOrder.isEmpty()) {
				return new PageResult<>(List.of(), 0, safePage, safeSize);
			}
			wrapper.in(Video::getId, historyOrder);
			// 保持观看时间倒序
			String orderSql = historyOrder.stream().map(String::valueOf).collect(Collectors.joining(","));
			wrapper.last("ORDER BY FIELD(id," + orderSql + ")");
		} else {
			wrapper.orderByAsc(Video::getSortOrder).orderByDesc(Video::getCreatedAt);
		}

		Page<Video> pageObj = videoMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
		List<Video> records = pageObj.getRecords();
		if (records.isEmpty()) {
			return new PageResult<>(List.of(), pageObj.getTotal(), safePage, safeSize);
		}

		List<Long> videoIds = records.stream().map(Video::getId).toList();
		Map<Long, Integer> sentenceCounts = loadSentenceCounts(videoIds);
		Set<Long> favoritedIds = loadFavoritedIds(userId, videoIds);
		Set<Long> learnedIds = loadLearnedIds(userId, videoIds);
		Map<Long, List<TagDto>> tagsByVideo = loadTagsByVideoIds(videoIds);

		List<VideoSummary> items = records.stream()
				.map(v -> VideoSummary.from(
						v,
						tagsByVideo.getOrDefault(v.getId(), List.of()),
						sentenceCounts.getOrDefault(v.getId(), 0),
						favoritedIds.contains(v.getId()),
						learnedIds.contains(v.getId())))
				.toList();

		return new PageResult<>(items, pageObj.getTotal(), safePage, safeSize);
	}

	public VideoOverview overview(Long userId) {
		Long total = videoMapper.selectCount(new LambdaQueryWrapper<Video>().eq(Video::getStatus, 1));
		long totalCount = total == null ? 0 : total;
		long learned = 0;
		long favorited = 0;
		long history = 0;
		if (userId != null) {
			learned = progressMapper.selectCount(new LambdaQueryWrapper<UserVideoProgress>()
					.eq(UserVideoProgress::getUserId, userId));
			favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<VideoFavorite>()
					.eq(VideoFavorite::getUserId, userId));
			history = learned;
		}
		List<TagDto> tags = listActiveTags();
		return new VideoOverview(totalCount, learned, Math.max(0, totalCount - learned), favorited, history, tags);
	}

	public List<TagDto> listActiveTags() {
		return videoTagMapper.selectList(new LambdaQueryWrapper<VideoTag>()
						.eq(VideoTag::getStatus, 1)
						.orderByAsc(VideoTag::getSortOrder))
				.stream()
				.map(t -> new TagDto(t.getId(), t.getName(), t.getSlug()))
				.toList();
	}

	public List<TagDto> listTagsForVideo(Long videoId) {
		return loadTagsByVideoIds(List.of(videoId)).getOrDefault(videoId, List.of());
	}

	/** 章节推荐位：仅返回已发布视频摘要，不存在则 null。 */
	public VideoSummary findSummary(Long videoId, Long userId) {
		if (videoId == null) {
			return null;
		}
		Video video = videoMapper.selectById(videoId);
		if (video == null || video.getStatus() != 1) {
			return null;
		}
		int sentenceCount = loadSentenceCounts(List.of(videoId)).getOrDefault(videoId, 0);
		boolean favorited = isFavorited(userId, videoId);
		boolean learned = loadLearnedIds(userId, List.of(videoId)).contains(videoId);
		List<TagDto> tags = loadTagsByVideoIds(List.of(videoId)).getOrDefault(videoId, List.of());
		return VideoSummary.from(video, tags, sentenceCount, favorited, learned);
	}

	public VideoDetail getVideo(Long id, IslandUserDetails userDetails) {
		Video video = videoMapper.selectById(id);
		if (video == null || video.getStatus() != 1) {
			throw new BusinessException(404, "视频不存在");
		}
		checkVipVideo(video.getIsVip(), userDetails);
		Long userId = userDetails != null ? userDetails.getUser().getId() : null;
		if (userId != null) {
			touchProgress(userId, id, null, null);
		}
		var sentences = sentenceMapper.selectList(new LambdaQueryWrapper<VideoSentence>()
						.eq(VideoSentence::getVideoId, id)
						.orderByAsc(VideoSentence::getSeq))
				.stream()
				.map(SentenceDto::from)
				.toList();
		List<TagDto> tags = loadTagsByVideoIds(List.of(id)).getOrDefault(id, List.of());
		return VideoDetail.from(video, tags, sentences, isFavorited(userId, id));
	}

	@Transactional
	public void toggleFavorite(Long userId, Long videoId) {
		if (videoMapper.selectById(videoId) == null) {
			throw new BusinessException(404, "视频不存在");
		}
		VideoFavorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<VideoFavorite>()
				.eq(VideoFavorite::getUserId, userId)
				.eq(VideoFavorite::getVideoId, videoId));
		if (existing != null) {
			favoriteMapper.deleteById(existing.getId());
		} else {
			VideoFavorite fav = new VideoFavorite();
			fav.setUserId(userId);
			fav.setVideoId(videoId);
			favoriteMapper.insert(fav);
		}
	}

	@Transactional
	public void touchProgress(Long userId, Long videoId, Integer sentenceSeq, Integer positionMs) {
		UserVideoProgress existing = progressMapper.selectOne(new LambdaQueryWrapper<UserVideoProgress>()
				.eq(UserVideoProgress::getUserId, userId)
				.eq(UserVideoProgress::getVideoId, videoId));
		if (existing == null) {
			UserVideoProgress p = new UserVideoProgress();
			p.setUserId(userId);
			p.setVideoId(videoId);
			p.setLastSentenceSeq(sentenceSeq);
			p.setLastPositionMs(positionMs);
			progressMapper.insert(p);
			return;
		}
		if (sentenceSeq != null) {
			existing.setLastSentenceSeq(sentenceSeq);
		}
		if (positionMs != null) {
			existing.setLastPositionMs(positionMs);
		}
		existing.setUpdatedAt(LocalDateTime.now());
		progressMapper.updateById(existing);
	}

	@Transactional
	public void replaceVideoTags(Long videoId, List<Integer> tagIds) {
		videoTagRelMapper.delete(new LambdaQueryWrapper<VideoTagRel>().eq(VideoTagRel::getVideoId, videoId));
		if (tagIds == null || tagIds.isEmpty()) {
			return;
		}
		Set<Integer> unique = new LinkedHashSet<>(tagIds);
		for (Integer tagId : unique) {
			if (tagId == null) continue;
			VideoTag tag = videoTagMapper.selectById(tagId);
			if (tag == null || tag.getStatus() != 1) {
				throw new BusinessException("标签不存在或已停用: " + tagId);
			}
			VideoTagRel rel = new VideoTagRel();
			rel.setVideoId(videoId);
			rel.setTagId(tagId);
			videoTagRelMapper.insert(rel);
		}
	}

	public void refreshVocabCount(Long videoId) {
		var sentences = sentenceMapper.selectList(new LambdaQueryWrapper<VideoSentence>()
				.eq(VideoSentence::getVideoId, videoId)
				.select(VideoSentence::getTextEn));
		int count = VocabCounter.countUniqueWords(sentences.stream().map(VideoSentence::getTextEn).toList());
		videoMapper.update(null, new LambdaUpdateWrapper<Video>()
				.eq(Video::getId, videoId)
				.set(Video::getVocabCount, count));
	}

	private Set<Long> resolveScopeVideoIds(Long userId, String tagSlug, String filter) {
		Set<Long> byTag = null;
		if (StringUtils.hasText(tagSlug) && !"all".equalsIgnoreCase(tagSlug)) {
			byTag = new LinkedHashSet<>(videoTagRelMapper.listVideoIdsByTagSlug(tagSlug.trim()));
		}

		Set<Long> byFilter = null;
		switch (filter) {
			case "learned" -> {
				if (userId == null) return Set.of();
				byFilter = new LinkedHashSet<>(listProgressVideoIds(userId));
			}
			case "unlearned" -> {
				List<Video> all = videoMapper.selectList(new LambdaQueryWrapper<Video>()
						.eq(Video::getStatus, 1)
						.select(Video::getId));
				Set<Long> allIds = all.stream().map(Video::getId).collect(Collectors.toCollection(LinkedHashSet::new));
				if (userId != null) {
					allIds.removeAll(listProgressVideoIds(userId));
				}
				byFilter = allIds;
			}
			case "favorited" -> {
				if (userId == null) return Set.of();
				byFilter = favoriteMapper.selectList(new LambdaQueryWrapper<VideoFavorite>()
								.eq(VideoFavorite::getUserId, userId))
						.stream()
						.map(VideoFavorite::getVideoId)
						.collect(Collectors.toCollection(LinkedHashSet::new));
			}
			case "history" -> {
				if (userId == null) return Set.of();
				byFilter = new LinkedHashSet<>(listHistoryVideoIds(userId));
			}
			default -> {
			}
		}

		if (byTag == null && byFilter == null) {
			return null;
		}
		if (byTag == null) {
			return byFilter;
		}
		if (byFilter == null) {
			return byTag;
		}
		byTag.retainAll(byFilter);
		return byTag;
	}

	private List<Long> listProgressVideoIds(Long userId) {
		return progressMapper.selectList(new LambdaQueryWrapper<UserVideoProgress>()
						.eq(UserVideoProgress::getUserId, userId)
						.select(UserVideoProgress::getVideoId))
				.stream()
				.map(UserVideoProgress::getVideoId)
				.toList();
	}

	private List<Long> listHistoryVideoIds(Long userId) {
		return progressMapper.selectList(new LambdaQueryWrapper<UserVideoProgress>()
						.eq(UserVideoProgress::getUserId, userId)
						.orderByDesc(UserVideoProgress::getUpdatedAt)
						.select(UserVideoProgress::getVideoId, UserVideoProgress::getUpdatedAt))
				.stream()
				.map(UserVideoProgress::getVideoId)
				.toList();
	}

	private Map<Long, Integer> loadSentenceCounts(List<Long> videoIds) {
		if (videoIds.isEmpty()) {
			return Map.of();
		}
		return sentenceMapper.countByVideoIds(videoIds).stream()
				.collect(Collectors.toMap(
						VideoSentenceMapper.SentenceCountRow::videoId,
						VideoSentenceMapper.SentenceCountRow::cnt));
	}

	private Map<Long, List<TagDto>> loadTagsByVideoIds(List<Long> videoIds) {
		if (videoIds.isEmpty()) {
			return Map.of();
		}
		List<VideoTagRel> rels = videoTagRelMapper.selectList(new LambdaQueryWrapper<VideoTagRel>()
				.in(VideoTagRel::getVideoId, videoIds));
		if (rels.isEmpty()) {
			return Map.of();
		}
		Set<Integer> tagIds = rels.stream().map(VideoTagRel::getTagId).collect(Collectors.toSet());
		Map<Integer, VideoTag> tagMap = videoTagMapper.selectList(new LambdaQueryWrapper<VideoTag>()
						.in(VideoTag::getId, tagIds))
				.stream()
				.filter(t -> t.getStatus() != null && t.getStatus() == 1)
				.collect(Collectors.toMap(VideoTag::getId, t -> t));

		Map<Long, List<TagDto>> map = new HashMap<>();
		for (VideoTagRel rel : rels) {
			VideoTag tag = tagMap.get(rel.getTagId());
			if (tag == null) continue;
			map.computeIfAbsent(rel.getVideoId(), k -> new ArrayList<>())
					.add(new TagDto(tag.getId(), tag.getName(), tag.getSlug()));
		}
		for (List<TagDto> list : map.values()) {
			list.sort(Comparator.comparing(TagDto::name, Comparator.nullsLast(String::compareTo)));
		}
		return map;
	}

	private Set<Long> loadFavoritedIds(Long userId, List<Long> videoIds) {
		if (userId == null || videoIds.isEmpty()) {
			return Set.of();
		}
		return favoriteMapper.selectList(new LambdaQueryWrapper<VideoFavorite>()
						.eq(VideoFavorite::getUserId, userId)
						.in(VideoFavorite::getVideoId, videoIds))
				.stream()
				.map(VideoFavorite::getVideoId)
				.collect(Collectors.toSet());
	}

	private Set<Long> loadLearnedIds(Long userId, List<Long> videoIds) {
		if (userId == null || videoIds.isEmpty()) {
			return Set.of();
		}
		return progressMapper.selectList(new LambdaQueryWrapper<UserVideoProgress>()
						.eq(UserVideoProgress::getUserId, userId)
						.in(UserVideoProgress::getVideoId, videoIds))
				.stream()
				.map(UserVideoProgress::getVideoId)
				.collect(Collectors.toSet());
	}

	private boolean isFavorited(Long userId, Long videoId) {
		if (userId == null) {
			return false;
		}
		Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<VideoFavorite>()
				.eq(VideoFavorite::getUserId, userId)
				.eq(VideoFavorite::getVideoId, videoId));
		return count != null && count > 0;
	}

	private void checkVipVideo(Integer isVip, IslandUserDetails userDetails) {
		if (isVip != null && isVip == 1) {
			boolean vip = userDetails != null && userDetails.getUser().isVipActive();
			if (!vip) {
				throw new BusinessException(403, "该视频为 VIP 专属内容，请升级后观看");
			}
		}
	}

	private static String normalizeFilter(String filter) {
		if (!StringUtils.hasText(filter)) {
			return "all";
		}
		return switch (filter.trim().toLowerCase()) {
			case "learned", "unlearned", "favorited", "history" -> filter.trim().toLowerCase();
			default -> "all";
		};
	}

	public record TagDto(Integer id, String name, String slug) {}

	public record VideoOverview(
			long totalCount,
			long learnedCount,
			long unlearnedCount,
			long favoritedCount,
			long historyCount,
			List<TagDto> tags
	) {}

	public record VideoSummary(
			Long id,
			String title,
			String description,
			String coverUrl,
			Integer durationSec,
			String difficulty,
			List<TagDto> tags,
			int sentenceCount,
			int vocabCount,
			boolean vip,
			boolean favorited,
			boolean learned,
			LocalDateTime createdAt
	) {
		static VideoSummary from(
				Video v,
				List<TagDto> tags,
				int sentenceCount,
				boolean favorited,
				boolean learned
		) {
			return new VideoSummary(
					v.getId(),
					v.getTitle(),
					v.getDescription(),
					v.getCoverUrl(),
					v.getDurationSec(),
					v.getDifficulty(),
					tags,
					sentenceCount,
					v.getVocabCount() != null ? v.getVocabCount() : 0,
					v.getIsVip() == 1,
					favorited,
					learned,
					v.getCreatedAt());
		}
	}

	public record SentenceDto(Long id, int seq, int startMs, int endMs, String textEn, String textZh) {
		static SentenceDto from(VideoSentence s) {
			return new SentenceDto(s.getId(), s.getSeq(), s.getStartMs(), s.getEndMs(), s.getTextEn(), s.getTextZh());
		}
	}

	public record VideoDetail(
			Long id, String title, String description, String coverUrl,
			String storageType, String provider, String sourceUrl, String embedBvid,
			String playUrl, Integer durationSec, int vocabCount, int sentenceCount,
			boolean vip, boolean favorited,
			List<TagDto> tags,
			List<SentenceDto> sentences
	) {
		static VideoDetail from(Video v, List<TagDto> tags, List<SentenceDto> sentences, boolean favorited) {
			return new VideoDetail(
					v.getId(), v.getTitle(), v.getDescription(), v.getCoverUrl(),
					v.getStorageType(), v.getProvider(), v.getSourceUrl(), v.getEmbedBvid(),
					v.getPlayUrl(), v.getDurationSec(),
					v.getVocabCount() != null ? v.getVocabCount() : 0,
					sentences.size(),
					v.getIsVip() == 1, favorited, tags, sentences);
		}
	}
}
