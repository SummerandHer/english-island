package com.island.module.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.island.common.BusinessException;
import com.island.common.PageResult;
import com.island.module.video.mapper.VideoFavoriteMapper;
import com.island.module.video.mapper.VideoMapper;
import com.island.module.video.mapper.VideoSentenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

	private static final Map<String, String> DIFFICULTY_TAGS = Map.of(
			"easy", "入门",
			"medium", "进阶",
			"hard", "挑战"
	);

	private final VideoMapper videoMapper;
	private final VideoSentenceMapper sentenceMapper;
	private final VideoFavoriteMapper favoriteMapper;

	public PageResult<VideoSummary> listVideos(int page, int size, Long userId) {
		int safePage = Math.max(page, 1);
		int safeSize = Math.min(Math.max(size, 1), 50);

		Page<Video> pageObj = videoMapper.selectPage(new Page<>(safePage, safeSize),
				new LambdaQueryWrapper<Video>()
						.eq(Video::getStatus, 1)
						.orderByAsc(Video::getSortOrder)
						.orderByDesc(Video::getCreatedAt));

		List<Video> records = pageObj.getRecords();
		if (records.isEmpty()) {
			return new PageResult<>(List.of(), pageObj.getTotal(), safePage, safeSize);
		}

		List<Long> videoIds = records.stream().map(Video::getId).toList();
		Map<Long, Integer> sentenceCounts = loadSentenceCounts(videoIds);
		Set<Long> favoritedIds = loadFavoritedIds(userId, videoIds);

		List<VideoSummary> items = records.stream()
				.map(v -> VideoSummary.from(
						v,
						sentenceCounts.getOrDefault(v.getId(), 0),
						favoritedIds.contains(v.getId())))
				.toList();

		return new PageResult<>(items, pageObj.getTotal(), safePage, safeSize);
	}

	public VideoDetail getVideo(Long id, Long userId) {
		Video video = videoMapper.selectById(id);
		if (video == null || video.getStatus() != 1) {
			throw new BusinessException(404, "视频不存在");
		}
		if (video.getIsVip() == 1) {
			throw new BusinessException(403, "该视频为 VIP 专属内容");
		}
		var sentences = sentenceMapper.selectList(new LambdaQueryWrapper<VideoSentence>()
						.eq(VideoSentence::getVideoId, id)
						.orderByAsc(VideoSentence::getSeq))
				.stream()
				.map(SentenceDto::from)
				.toList();
		return VideoDetail.from(video, sentences, isFavorited(userId, id));
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

	public void refreshVocabCount(Long videoId) {
		var sentences = sentenceMapper.selectList(new LambdaQueryWrapper<VideoSentence>()
				.eq(VideoSentence::getVideoId, videoId)
				.select(VideoSentence::getTextEn));
		int count = VocabCounter.countUniqueWords(sentences.stream().map(VideoSentence::getTextEn).toList());
		videoMapper.update(null, new LambdaUpdateWrapper<Video>()
				.eq(Video::getId, videoId)
				.set(Video::getVocabCount, count));
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

	private boolean isFavorited(Long userId, Long videoId) {
		if (userId == null) {
			return false;
		}
		Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<VideoFavorite>()
				.eq(VideoFavorite::getUserId, userId)
				.eq(VideoFavorite::getVideoId, videoId));
		return count != null && count > 0;
	}

	private static List<String> buildTags(Video v) {
		List<String> tags = new ArrayList<>();
		String difficultyTag = DIFFICULTY_TAGS.get(v.getDifficulty());
		if (difficultyTag != null) {
			tags.add(difficultyTag);
		}
		if (v.getIsVip() == 1) {
			tags.add("VIP");
		}
		return tags;
	}

	public record VideoSummary(
			Long id,
			String title,
			String description,
			String coverUrl,
			Integer durationSec,
			String difficulty,
			List<String> tags,
			int sentenceCount,
			int vocabCount,
			boolean vip,
			boolean favorited,
			LocalDateTime createdAt
	) {
		static VideoSummary from(Video v, int sentenceCount, boolean favorited) {
			return new VideoSummary(
					v.getId(),
					v.getTitle(),
					v.getDescription(),
					v.getCoverUrl(),
					v.getDurationSec(),
					v.getDifficulty(),
					buildTags(v),
					sentenceCount,
					v.getVocabCount() != null ? v.getVocabCount() : 0,
					v.getIsVip() == 1,
					favorited,
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
			List<SentenceDto> sentences
	) {
		static VideoDetail from(Video v, List<SentenceDto> sentences, boolean favorited) {
			return new VideoDetail(
					v.getId(), v.getTitle(), v.getDescription(), v.getCoverUrl(),
					v.getStorageType(), v.getProvider(), v.getSourceUrl(), v.getEmbedBvid(),
					v.getPlayUrl(), v.getDurationSec(),
					v.getVocabCount() != null ? v.getVocabCount() : 0,
					sentences.size(),
					v.getIsVip() == 1, favorited, sentences);
		}
	}
}
