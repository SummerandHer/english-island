package com.island.module.video;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从英文字幕文本统计去重词汇量（列表展示用，非 NLP 精确分词）。
 */
public final class VocabCounter {

	private VocabCounter() {}

	public static int countUniqueWords(Iterable<String> englishTexts) {
		Set<String> words = Arrays.stream(collectJoined(englishTexts).split("\\s+"))
				.map(VocabCounter::normalize)
				.filter(w -> w.length() >= 2)
				.collect(Collectors.toSet());
		return words.size();
	}

	private static String collectJoined(Iterable<String> texts) {
		StringBuilder sb = new StringBuilder();
		for (String text : texts) {
			if (text == null || text.isBlank()) {
				continue;
			}
			if (!sb.isEmpty()) {
				sb.append(' ');
			}
			sb.append(text.replaceAll("[^A-Za-z'\\- ]", " "));
		}
		return sb.toString();
	}

	private static String normalize(String raw) {
		String w = raw.toLowerCase(Locale.ROOT);
		while (w.length() >= 2 && (w.startsWith("'") || w.endsWith("'"))) {
			w = w.replaceAll("^'+|'+$", "");
		}
		return w;
	}
}
