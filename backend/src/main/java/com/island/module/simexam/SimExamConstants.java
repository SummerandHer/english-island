package com.island.module.simexam;

import java.util.Set;

public final class SimExamConstants {

	public static final String LICENSE_NOTE =
			"本人持有之四六级试卷录入，仅作站内仿真题仿写底稿，不对用户展示原文/原题。";

	public static final String SECTION_SHORT = "short_careful";
	public static final String SECTION_LONG = "long_match";

	public static final Set<String> SECTION_TYPES = Set.of(SECTION_SHORT, SECTION_LONG);
	public static final Set<String> EXAM_LEVELS = Set.of("cet4", "cet6");
	public static final Set<String> PASSAGE_STATUS = Set.of("draft", "ready", "published");

	public static final String AI_VERSION = "sim-short-v1";
	public static final String AI_VERSION_LONG = "sim-long-v1";

	/** 与底稿句子级粗相似度超过此阈值 → needs_review */
	public static final double SIMILARITY_REVIEW_THRESHOLD = 0.42;

	private SimExamConstants() {
	}
}
