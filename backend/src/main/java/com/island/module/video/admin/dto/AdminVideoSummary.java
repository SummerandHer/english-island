package com.island.module.video.admin.dto;

import java.time.LocalDateTime;

public record AdminVideoSummary(
		Long id,
		String title,
		String coverUrl,
		String storageType,
		Integer status,
		Integer durationSec,
		boolean vip,
		int sentenceCount,
		LocalDateTime createdAt
) {}
