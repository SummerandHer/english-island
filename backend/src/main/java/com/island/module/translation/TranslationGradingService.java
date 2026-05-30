package com.island.module.translation;

import com.island.config.IslandProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationGradingService {

	private final IslandProperties islandProperties;

	public GradingResult grade(String prompt, String referenceAnswer, String userAnswer) {
		if (islandProperties.getAi().isEnabled() && islandProperties.getAi().getApiKey() != null
				&& !islandProperties.getAi().getApiKey().isBlank()) {
			// TODO: 对接 DeepSeek / 通义等 LLM API
			log.info("AI grading enabled but integration pending, using fallback scorer");
		}
		return fallbackGrade(referenceAnswer, userAnswer);
	}

	private GradingResult fallbackGrade(String referenceAnswer, String userAnswer) {
		int refLen = referenceAnswer.trim().split("\\s+").length;
		int userLen = userAnswer.trim().split("\\s+").length;
		int score = Math.min(100, 60 + Math.min(userLen, refLen) * 2);
		return new GradingResult(
				score,
				"MVP 占位批改：已收到你的译文。配置 AI API Key 后将启用完整智能批改。",
				List.of(Map.of(
						"span", userAnswer.length() > 40 ? userAnswer.substring(0, 40) + "..." : userAnswer,
						"suggestion", referenceAnswer.length() > 80 ? referenceAnswer.substring(0, 80) + "..." : referenceAnswer,
						"reason", "请对照参考答案优化表达与搭配"
				)),
				"可参考官方参考答案中的句式与用词。",
				Map.of("mode", "fallback")
		);
	}

	public record GradingResult(
			int score,
			String overallComment,
			List<Map<String, String>> errors,
			String referenceHint,
			Map<String, Object> rawResponse
	) {}
}
