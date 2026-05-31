package com.island.module.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.ai.DeepSeekChatService;
import com.island.ai.LlmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationGradingService {

	private final DeepSeekChatService deepSeekChatService;
	private final ObjectMapper objectMapper;

	public GradingResult grade(String direction, String prompt, String referenceAnswer, String userAnswer) {
		if (deepSeekChatService.isConfigured()) {
			try {
				return gradeWithLlm(direction, prompt, referenceAnswer, userAnswer);
			} catch (LlmException e) {
				log.warn("AI 翻译批改失败，使用占位评分: {}", e.getMessage());
			} catch (Exception e) {
				log.warn("AI 翻译批改异常，使用占位评分: {}", e.getMessage());
			}
		}
		return fallbackGrade(referenceAnswer, userAnswer);
	}

	private GradingResult gradeWithLlm(String direction, String prompt, String referenceAnswer, String userAnswer) {
		String taskDesc = "zh2en".equals(direction)
				? "中译英（汉译英）"
				: "英译中（英译汉）";

		String llmPrompt = """
				你是一名大学英语四六级翻译阅卷老师。请对用户的译文进行严格评分与批改，风格贴近四六级阅卷：指出扣分点、语法错误、搭配不当、漏译误译。
				
				题型：%s
				题目原文：%s
				官方参考答案：%s
				用户译文：%s
				
				评分标准（满分 100）：
				- 意思准确（40 分）：是否忠实传达原文含义，有无漏译、误译、增译
				- 语法正确（30 分）：时态、语态、主谓一致、从句、句式结构
				- 用词搭配（20 分）：词汇选择、固定搭配、地道性与正式度
				- 拼写标点（10 分）：拼写、大小写、标点
				
				要求：
				1. score 为 0-100 整数，按四六级标准严格给分
				2. errors 列出 1-5 个主要扣分点；若无明显错误可返回空数组
				3. span 为用户译文中需指出的片段；suggestion 为改进表达；reason 为扣分原因
				4. referenceHint 给出可借鉴的句型或表达提示，不要整段复制参考答案
				5. overallComment 为 2-4 句总体评价，点明主要问题与改进方向
				
				严格按 JSON 返回，不要 markdown，不要任何解释：
				{"score":85,"overallComment":"...","errors":[{"span":"...","suggestion":"...","reason":"..."}],"referenceHint":"..."}
				""".formatted(taskDesc, prompt, referenceAnswer, userAnswer);

		var chat = deepSeekChatService.chat(llmPrompt, 0.2);
		JsonNode json = deepSeekChatService.parseJsonContent(chat.content());

		int score = Math.clamp(json.path("score").asInt(0), 0, 100);
		String overallComment = json.path("overallComment").asText("").trim();
		String referenceHint = json.path("referenceHint").asText("").trim();
		List<Map<String, String>> errors = parseErrors(json.path("errors"));

		if (overallComment.isBlank()) {
			overallComment = "已完成 AI 批改，请根据错误点与参考提示改进译文。";
		}

		Map<String, Object> raw = new LinkedHashMap<>(chat.rawResponse());
		raw.put("parsed", objectMapper.convertValue(json, Map.class));

		return new GradingResult(
				score,
				overallComment,
				errors,
				referenceHint,
				chat.model(),
				raw
		);
	}

	private List<Map<String, String>> parseErrors(JsonNode errorsNode) {
		List<Map<String, String>> errors = new ArrayList<>();
		if (!errorsNode.isArray()) {
			return errors;
		}
		for (JsonNode node : errorsNode) {
			String span = node.path("span").asText("").trim();
			String suggestion = node.path("suggestion").asText("").trim();
			String reason = node.path("reason").asText("").trim();
			if (span.isBlank() && suggestion.isBlank() && reason.isBlank()) {
				continue;
			}
			Map<String, String> err = new LinkedHashMap<>();
			err.put("span", span);
			err.put("suggestion", suggestion);
			err.put("reason", reason);
			errors.add(err);
		}
		return errors;
	}

	private GradingResult fallbackGrade(String referenceAnswer, String userAnswer) {
		int refLen = referenceAnswer.trim().split("\\s+").length;
		int userLen = userAnswer.trim().split("\\s+").length;
		int score = Math.min(100, 60 + Math.min(userLen, refLen) * 2);
		return new GradingResult(
				score,
				"AI 暂不可用，以下为占位评分。请配置 island.ai 后获得完整智能批改。",
				List.of(Map.of(
						"span", userAnswer.length() > 40 ? userAnswer.substring(0, 40) + "..." : userAnswer,
						"suggestion", referenceAnswer.length() > 80 ? referenceAnswer.substring(0, 80) + "..." : referenceAnswer,
						"reason", "请对照参考答案优化表达与搭配"
				)),
				"可参考官方参考答案中的句式与用词。",
				"fallback",
				Map.of("mode", "fallback")
		);
	}

	public record GradingResult(
			int score,
			String overallComment,
			List<Map<String, String>> errors,
			String referenceHint,
			String aiModel,
			Map<String, Object> rawResponse
	) {}
}
