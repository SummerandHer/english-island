package com.island.module.reading;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.reading.mapper.ReadingChapterMapper;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingService {

	private final ReadingChapterMapper chapterMapper;

	public List<ReadingChapterSummary> listChapters() {
		return chapterMapper.selectList(new LambdaQueryWrapper<ReadingChapter>()
						.eq(ReadingChapter::getStatus, 1)
						.orderByAsc(ReadingChapter::getSortOrder))
				.stream()
				.map(ReadingChapterSummary::from)
				.toList();
	}

	public ReadingChapterDetail getChapter(String slug, IslandUserDetails userDetails) {
		ReadingChapter chapter = chapterMapper.selectOne(new LambdaQueryWrapper<ReadingChapter>()
				.eq(ReadingChapter::getSlug, slug)
				.eq(ReadingChapter::getStatus, 1));
		if (chapter == null) {
			throw new BusinessException(404, "章节不存在");
		}
		if (chapter.getIsVip() == 1) {
			boolean vip = userDetails != null && userDetails.getUser().isVipActive();
			if (!vip) {
				throw new BusinessException(403, "该章节为 VIP 高级技巧，请升级后阅读");
			}
		}
		return ReadingChapterDetail.from(chapter);
	}

	public record ReadingChapterSummary(Long id, String title, String slug, String summary, boolean vip) {
		static ReadingChapterSummary from(ReadingChapter c) {
			return new ReadingChapterSummary(c.getId(), c.getTitle(), c.getSlug(), c.getSummary(), c.getIsVip() == 1);
		}
	}

	public record ReadingChapterDetail(Long id, String title, String slug, String summary, String contentHtml, boolean vip) {
		static ReadingChapterDetail from(ReadingChapter c) {
			return new ReadingChapterDetail(c.getId(), c.getTitle(), c.getSlug(), c.getSummary(), c.getContentHtml(), c.getIsVip() == 1);
		}
	}
}
