package com.island.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.config.IslandProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeepSeekChatService {

	private final IslandProperties islandProperties;
	private final ObjectMapper objectMapper;
	private final RestTemplate aiRestTemplate;

	public DeepSeekChatService(
			IslandProperties islandProperties,
			ObjectMapper objectMapper,
			@Qualifier("aiRestTemplate") RestTemplate aiRestTemplate) {
		this.islandProperties = islandProperties;
		this.objectMapper = objectMapper;
		this.aiRestTemplate = aiRestTemplate;
	}

	public boolean isConfigured() {
		var ai = islandProperties.getAi();
		return ai.isEnabled()
				&& ai.getApiKey() != null
				&& !ai.getApiKey().isBlank();
	}

	public String getModel() {
		var model = islandProperties.getAi().getModel();
		return model != null && !model.isBlank() ? model : "deepseek-chat";
	}

	public ChatResult chat(String userPrompt, double temperature) {
		return chat(null, userPrompt, temperature);
	}

	public ChatResult chat(String systemPrompt, String userPrompt, double temperature) {
		if (!isConfigured()) {
			throw new LlmException("AI 未配置：请设置 island.ai.enabled=true 并填写 api-key");
		}

		List<Map<String, String>> messages = new java.util.ArrayList<>();
		if (systemPrompt != null && !systemPrompt.isBlank()) {
			messages.add(Map.of("role", "system", "content", systemPrompt));
		}
		messages.add(Map.of("role", "user", "content", userPrompt));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", getModel());
		body.put("messages", messages);
		body.put("temperature", temperature);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(islandProperties.getAi().getApiKey());

		String url = resolveChatCompletionsUrl();

		try {
			ResponseEntity<String> response = aiRestTemplate.postForEntity(
					url,
					new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
					String.class);

			JsonNode root = objectMapper.readTree(response.getBody());
			String content = root.path("choices").path(0).path("message").path("content").asText("");
			if (content.isBlank()) {
				throw new LlmException("AI 返回内容为空");
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> raw = objectMapper.readValue(response.getBody(), Map.class);
			return new ChatResult(content.trim(), getModel(), raw);
		} catch (RestClientResponseException e) {
			log.warn("DeepSeek API HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new LlmException("AI 接口调用失败（HTTP " + e.getStatusCode().value() + "）", e);
		} catch (LlmException e) {
			throw e;
		} catch (Exception e) {
			log.warn("DeepSeek API 调用异常: {}", e.getMessage());
			throw new LlmException("AI 接口调用失败：" + e.getMessage(), e);
		}
	}

	public JsonNode parseJsonContent(String content) {
		try {
			return objectMapper.readTree(stripMarkdownFence(content));
		} catch (Exception e) {
			throw new LlmException("AI 返回的 JSON 无法解析", e);
		}
	}

	public static String stripMarkdownFence(String content) {
		String trimmed = content.trim();
		if (trimmed.startsWith("```")) {
			return trimmed.replaceAll("^```json\\s*|^```\\s*|```\\s*$", "").trim();
		}
		return trimmed;
	}

	private String resolveChatCompletionsUrl() {
		String baseUrl = islandProperties.getAi().getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.deepseek.com";
		}
		return baseUrl.replaceAll("/$", "") + "/v1/chat/completions";
	}

	public record ChatResult(String content, String model, Map<String, Object> rawResponse) {}
}
