package com.island.module.video.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminVideoDetail(
		Long id,
		Long seriesId,
		String title,
		String description,
		String coverUrl,
		String storageType,
		String provider,
		String sourceUrl,
		String embedBvid,
		String playUrl,
		Integer durationSec,
		String difficulty,
		boolean vip,
		Integer sortOrder,
		Integer status,
		LocalDateTime createdAt,
		List<TagView> tags,
		List<SentenceView> sentences
) {
	public record TagView(Integer id, String name, String slug) {}

	public record SentenceView(
			Long id, int seq, int startMs, int endMs, String textEn, String textZh
	) {}
}
