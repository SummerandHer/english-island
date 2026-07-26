package com.island.module.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.module.reading.ReadingService;
import com.island.module.translation.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChapterCacheService {

	private static final Duration TTL = Duration.ofHours(1);
	private static final String READING_KEY_PREFIX = "chapter:reading:";
	private static final String TRANSLATION_KEY_PREFIX = "chapter:translation:";

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public Optional<ReadingService.ReadingChapterDetail> getReadingChapter(String slug) {
		return get(READING_KEY_PREFIX + slug, new TypeReference<>() {});
	}

	public void putReadingChapter(String slug, ReadingService.ReadingChapterDetail detail) {
		put(READING_KEY_PREFIX + slug, detail);
	}

	public Optional<TranslationService.ChapterDetail> getTranslationChapter(String slug) {
		return get(TRANSLATION_KEY_PREFIX + slug, new TypeReference<>() {});
	}

	public void putTranslationChapter(String slug, TranslationService.ChapterDetail detail) {
		put(TRANSLATION_KEY_PREFIX + slug, detail);
	}

	private <T> Optional<T> get(String key, TypeReference<T> type) {
		String json = redis.opsForValue().get(key);
		if (json == null || json.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, type));
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	private void put(String key, Object value) {
		try {
			String json = objectMapper.writeValueAsString(value);
			redis.opsForValue().set(key, json, TTL);
		} catch (Exception ignored) {
			// cache write failure should not break the request
		}
	}
}
