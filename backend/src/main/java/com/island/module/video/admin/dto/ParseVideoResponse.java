package com.island.module.video.admin.dto;

import java.util.List;

public record ParseVideoResponse(
		String videoObjectKey,
		String playUrl,
		Long fileId,
		Integer durationMs,
		Integer durationSec,
		List<SentenceDraft> sentences,
		List<String> logs
) {
	public record SentenceDraft(int seq, int startMs, int endMs, String textEn, String textZh) {}
}
