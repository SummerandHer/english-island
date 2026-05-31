package com.island.module.video.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.island.ai.DeepSeekChatService;
import com.island.ai.LlmException;
import com.island.common.BusinessException;
import com.island.config.IslandProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentenceZhDraftService {

	private final DeepSeekChatService deepSeekChatService;
	private final IslandProperties islandProperties;

	/** 按 seq 顺序返回中文草稿；解析流程失败时返回空字符串，不中断主流程。 */
	public List<String> generateDrafts(List<String> englishLines) {
		return generateDrafts(englishLines, false);
	}

	/** Admin 主动生成时 failOnError=true，失败抛出 BusinessException。 */
	public List<String> generateDrafts(List<String> englishLines, boolean failOnError) {
		if (englishLines == null || englishLines.isEmpty()) {
			return List.of();
		}
		if (!deepSeekChatService.isConfigured()) {
			if (failOnError) {
				throw new BusinessException("AI 未配置：请设置 island.ai.enabled=true 并填写 api-key");
			}
			log.info("[视频解析] AI 未配置，跳过中文草稿生成");
			return englishLines.stream().map(s -> "").toList();
		}

		try {
			List<String> zhList = generateInBatches(englishLines);
			log.info("[视频解析] AI 中文草稿已生成 {} 句", zhList.size());
			if (failOnError && zhList.stream().allMatch(String::isBlank)) {
				throw new BusinessException("AI 未返回有效中文译文，请稍后重试");
			}
			return zhList;
		} catch (BusinessException e) {
			throw e;
		} catch (LlmException e) {
			log.warn("[视频解析] AI 中文草稿失败: {}", e.getMessage());
			if (failOnError) {
				throw new BusinessException(e.getMessage());
			}
			return englishLines.stream().map(s -> "").toList();
		} catch (Exception e) {
			log.warn("[视频解析] AI 中文草稿异常: {}", e.getMessage());
			if (failOnError) {
				throw new BusinessException("AI 中文草稿生成失败：" + e.getMessage());
			}
			return englishLines.stream().map(s -> "").toList();
		}
	}

	private List<String> generateInBatches(List<String> englishLines) {
		int batchSize = Math.max(1, islandProperties.getAi().getZhDraftBatchSize());
		List<String> result = new ArrayList<>(englishLines.size());

		for (int offset = 0; offset < englishLines.size(); offset += batchSize) {
			int end = Math.min(offset + batchSize, englishLines.size());
			List<String> batch = englishLines.subList(offset, end);
			List<String> zhBatch = callLlm(batch);
			result.addAll(zhBatch);
			log.debug("[视频解析] 中译批次 {}-{}/{} 完成", offset + 1, end, englishLines.size());
		}

		while (result.size() < englishLines.size()) {
			result.add("");
		}
		return result;
	}

	private List<String> callLlm(List<String> englishLines) {
		var lines = new StringBuilder();
		for (int i = 0; i < englishLines.size(); i++) {
			lines.append(i + 1).append(". ").append(englishLines.get(i)).append("\n");
		}
		String prompt = """
				你是英语翻译助手。将下列英文句子逐句翻译成自然、准确的中文，用于双语学习字幕。
				严格按 JSON 数组返回，每项为对应序号的中文翻译字符串，不要 markdown，不要解释。
				数组长度必须与句子数量一致。
				例：["译文1","译文2"]

				英文句子：
				""" + lines;

		var chat = deepSeekChatService.chat(prompt, 0.3);
		JsonNode arr = deepSeekChatService.parseJsonContent(chat.content());

		if (!arr.isArray()) {
			throw new LlmException("AI 返回格式错误：期望 JSON 数组");
		}

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
