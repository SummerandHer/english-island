package com.island.module.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.video.mapper.VideoFavoriteMapper;
import com.island.module.video.mapper.VideoMapper;
import com.island.module.video.mapper.VideoSentenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

	private final VideoMapper videoMapper;
	private final VideoSentenceMapper sentenceMapper;
	private final VideoFavoriteMapper favoriteMapper;

	public List<VideoSummary> listVideos(Long userId) {
		return videoMapper.selectList(new LambdaQueryWrapper<Video>()
						.eq(Video::getStatus, 1)
						.orderByAsc(Video::getSortOrder))
				.stream()
				.map(v -> VideoSummary.from(v, isFavorited(userId, v.getId())))
				.toList();
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

	private boolean isFavorited(Long userId, Long videoId) {
		if (userId == null) {
			return false;
		}
		Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<VideoFavorite>()
				.eq(VideoFavorite::getUserId, userId)
				.eq(VideoFavorite::getVideoId, videoId));
		return count != null && count > 0;
	}

	public record VideoSummary(Long id, String title, String coverUrl, String embedBvid, Integer durationSec,
			boolean vip, boolean favorited) {
		static VideoSummary from(Video v, boolean favorited) {
			return new VideoSummary(v.getId(), v.getTitle(), v.getCoverUrl(), v.getEmbedBvid(),
					v.getDurationSec(), v.getIsVip() == 1, favorited);
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
			String playUrl, Integer durationSec, boolean vip, boolean favorited,
			List<SentenceDto> sentences
	) {
		static VideoDetail from(Video v, List<SentenceDto> sentences, boolean favorited) {
			return new VideoDetail(v.getId(), v.getTitle(), v.getDescription(), v.getCoverUrl(),
					v.getStorageType(), v.getProvider(), v.getSourceUrl(), v.getEmbedBvid(),
					v.getPlayUrl(), v.getDurationSec(), v.getIsVip() == 1, favorited, sentences);
		}
	}
}
