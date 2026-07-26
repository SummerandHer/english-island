package com.island.module.daily.dto;

import java.util.List;
import java.util.Map;

/**
 * Admin AI 增强结果：含质量门禁与规范化后的业务字段。
 */
public record DailyAiEnrichmentResult(
		String gate,
		List<String> warnings,
		String error,
		String aiVersion,
		String summaryZh,
		String topic,
		String difficulty,
		String slugSuggestion,
		Integer wordCount,
		String coverHint,
		String cetVocabJson,
		String hardVocabJson,
		String structuresJson,
		String aiRawJson,
		List<Map<String, Object>> cetVocab,
		List<Map<String, Object>> hardVocab,
		List<Map<String, Object>> structures
) {
	public static final String GATE_OK = "ok";
	public static final String GATE_NEEDS_REVIEW = "needs_review";
	public static final String GATE_FAILED = "failed";

	public boolean isHardFail() {
		return GATE_FAILED.equals(gate);
	}
}
