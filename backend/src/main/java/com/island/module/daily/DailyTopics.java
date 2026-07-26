package com.island.module.daily;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DailyTopics {

	public static final Set<String> ALL = Set.of(
			"education",
			"technology",
			"environment",
			"society_culture",
			"economy_business",
			"health",
			"psychology",
			"science"
	);

	private static final Map<String, String> LABELS = new LinkedHashMap<>();

	static {
		LABELS.put("education", "教育学习");
		LABELS.put("technology", "科技创新");
		LABELS.put("environment", "环境可持续");
		LABELS.put("society_culture", "社会文化");
		LABELS.put("economy_business", "经济商业");
		LABELS.put("health", "健康医学");
		LABELS.put("psychology", "心理认知");
		LABELS.put("science", "自然科学");
	}

	private DailyTopics() {
	}

	public static boolean isValid(String topic) {
		return topic != null && ALL.contains(topic);
	}

	public static String label(String topic) {
		return LABELS.getOrDefault(topic, topic);
	}

	public static List<Map<String, String>> catalog() {
		return LABELS.entrySet().stream()
				.map(e -> Map.of("slug", e.getKey(), "label", e.getValue()))
				.toList();
	}
}
