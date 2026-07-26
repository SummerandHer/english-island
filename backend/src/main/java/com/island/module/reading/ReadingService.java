package com.island.module.reading;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.content.ChapterCacheService;
import com.island.module.reading.dto.ReadingProgressRequest;
import com.island.module.reading.mapper.ReadingChapterMapper;
import com.island.module.reading.mapper.UserReadingProgressMapper;
import com.island.module.video.VideoService;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingService {

	private final ReadingChapterMapper chapterMapper;
	private final ReadingPassageService passageService;
	private final UserReadingProgressMapper progressMapper;
	private final VideoService videoService;
	private final ChapterCacheService chapterCache;

	public List<ReadingChapterSummary> listChapters() {
		return chapterMapper.selectList(new LambdaQueryWrapper<ReadingChapter>()
						.eq(ReadingChapter::getStatus, 1)
						.orderByAsc(ReadingChapter::getSortOrder))
				.stream()
				.map(ReadingChapterSummary::from)
				.toList();
	}

	public ReadingChapterDetail getChapter(String slug, IslandUserDetails userDetails) {
		var cached = chapterCache.getReadingChapter(slug);
		if (cached.isPresent()) {
			ReadingChapterDetail base = cached.get();
			checkVip(base.vip(), userDetails);
			return mergeReadingUserFields(base, userDetails);
		}

		ReadingChapter chapter = chapterMapper.selectOne(new LambdaQueryWrapper<ReadingChapter>()
				.eq(ReadingChapter::getSlug, slug)
				.eq(ReadingChapter::getStatus, 1));
		if (chapter == null) {
			throw new BusinessException(404, "章节不存在");
		}
		checkVip(chapter.getIsVip() == 1, userDetails);
		var recommendedVideo = videoService.findSummary(chapter.getRecommendedVideoId(), null);
		ReadingChapterDetail base = ReadingChapterDetail.from(
				chapter, passageService.listByChapterId(chapter.getId()), false, recommendedVideo);
		chapterCache.putReadingChapter(slug, base);
		return mergeReadingUserFields(base, userDetails);
	}

	@Transactional
	public void saveChapterProgress(Long userId, String slug, ReadingProgressRequest request) {
		ReadingChapter chapter = chapterMapper.selectOne(new LambdaQueryWrapper<ReadingChapter>()
				.eq(ReadingChapter::getSlug, slug)
				.eq(ReadingChapter::getStatus, 1));
		if (chapter == null) {
			throw new BusinessException(404, "章节不存在");
		}
		UserReadingProgress progress = progressMapper.selectOne(new LambdaQueryWrapper<UserReadingProgress>()
				.eq(UserReadingProgress::getUserId, userId)
				.eq(UserReadingProgress::getChapterId, chapter.getId()));
		int finished = Boolean.TRUE.equals(request.getFinished()) ? 1 : 0;
		if (progress == null) {
			progress = new UserReadingProgress();
			progress.setUserId(userId);
			progress.setChapterId(chapter.getId());
			progress.setIsFinished(finished);
			progressMapper.insert(progress);
		} else {
			progress.setIsFinished(finished);
			progressMapper.updateById(progress);
		}
	}

	private ReadingChapterDetail mergeReadingUserFields(ReadingChapterDetail base, IslandUserDetails userDetails) {
		boolean finished = isChapterFinished(userDetails, base.id());
		Long userId = userDetails != null ? userDetails.getUser().getId() : null;
		var recommendedVideo = base.recommendedVideo();
		if (recommendedVideo != null) {
			recommendedVideo = videoService.findSummary(recommendedVideo.id(), userId);
		}
		if (finished == base.finished() && recommendedVideo == base.recommendedVideo()) {
			return base;
		}
		return new ReadingChapterDetail(
				base.id(), base.title(), base.slug(), base.summary(), base.contentHtml(),
				base.vip(), base.passages(), finished, recommendedVideo);
	}

	private void checkVip(boolean vipChapter, IslandUserDetails userDetails) {
		if (vipChapter) {
			boolean vip = userDetails != null && userDetails.getUser().isVipActive();
			if (!vip) {
				throw new BusinessException(403, "该章节为 VIP 高级技巧，请升级后阅读");
			}
		}
	}

	private boolean isChapterFinished(IslandUserDetails userDetails, Long chapterId) {
		if (userDetails == null) {
			return false;
		}
		UserReadingProgress p = progressMapper.selectOne(new LambdaQueryWrapper<UserReadingProgress>()
				.eq(UserReadingProgress::getUserId, userDetails.getUser().getId())
				.eq(UserReadingProgress::getChapterId, chapterId));
		return p != null && p.getIsFinished() == 1;
	}

	public record ReadingChapterSummary(Long id, String title, String slug, String summary, boolean vip) {
		static ReadingChapterSummary from(ReadingChapter c) {
			return new ReadingChapterSummary(c.getId(), c.getTitle(), c.getSlug(), c.getSummary(), c.getIsVip() == 1);
		}
	}

	public record ReadingChapterDetail(
			Long id,
			String title,
			String slug,
			String summary,
			String contentHtml,
			boolean vip,
			List<ReadingPassageService.PassageSummary> passages,
			boolean finished,
			VideoService.VideoSummary recommendedVideo) {
		static ReadingChapterDetail from(
				ReadingChapter c,
				List<ReadingPassageService.PassageSummary> passages,
				boolean finished,
				VideoService.VideoSummary recommendedVideo) {
			return new ReadingChapterDetail(
					c.getId(), c.getTitle(), c.getSlug(), c.getSummary(), c.getContentHtml(), c.getIsVip() == 1, passages,
					finished, recommendedVideo);
		}
	}
}
