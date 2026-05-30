package com.island.module.video.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.config.IslandProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentenceZhDraftService {

	private final IslandProperties islandProperties;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RestTemplate restTemplate = new RestTemplate();

	/** 按 seq 顺序返回中文草稿，失败时返回空字符串列表。 */
	public List<String> generateDrafts(List<String> englishLines) {
		if (englishLines == null || englishLines.isEmpty()) {
			return List.of();
		}
		if (!islandProperties.getAi().isEnabled()
				|| islandProperties.getAi().getApiKey() == null
				|| islandProperties.getAi().getApiKey().isBlank()) {
			log.info("[视频解析] AI 未配置，跳过中文草稿生成");
			return englishLines.stream().map(s -> "").toList();
		}
		try {
			List<String> zhList = callLlm(englishLines);
			log.info("[视频解析] AI 中文草稿已生成 {} 句", zhList.size());
			return zhList;
		} catch (Exception e) {
			log.warn("[视频解析] AI 中文草稿失败: {}", e.getMessage());
			return englishLines.stream().map(s -> "").toList();
		}
	}

	private List<String> callLlm(List<String> englishLines) throws Exception {
		var lines = new StringBuilder();
		for (int i = 0; i < englishLines.size(); i++) {
			lines.append(i + 1).append(". ").append(englishLines.get(i)).append("\n");
		}
		String prompt = """
				你是英语翻译助手。将下列英文句子逐句翻译成自然的中文，用于双语学习字幕。
				严格按 JSON 数组返回，每项为对应序号的中文翻译字符串，不要 markdown，不要解释。
				例：["译文1","译文2"]

				英文句子：
				""" + lines;

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", "deepseek-chat");
		body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
		body.put("temperature", 0.3);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(islandProperties.getAi().getApiKey());

		String baseUrl = islandProperties.getAi().getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.deepseek.com";
		}
		String url = baseUrl.replaceAll("/$", "") + "/v1/chat/completions";

		ResponseEntity<String> response = restTemplate.postForEntity(
				url, new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

		JsonNode root = objectMapper.readTree(response.getBody());
		String content = root.path("choices").path(0).path("message").path("content").asText("");
		content = content.trim();
		if (content.startsWith("```")) {
			content = content.replaceAll("^```json\\s*|^```\\s*|```\\s*$", "").trim();
		}
		JsonNode arr = objectMapper.readTree(content);
		List<String> out = new ArrayList<>();
		for (JsonNode n : arr) {
			out.add(n.asText("").trim());
		}
		while (out.size() < englishLines.size()) {
			out.add("");
		}
		return out;
	}
}
