package com.island.module.file.dto;

import java.time.LocalDateTime;

public record FileDto(
		Long id,
		Long userId,
		String storageType,
		String originalName,
		String mimeType,
		Integer sizeBytes,
		String url,
		LocalDateTime createdAt
) {}
