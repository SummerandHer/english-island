package com.island.module.video.asr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.common.BusinessException;
import com.island.config.IslandProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperAsrService {

	private final IslandProperties islandProperties;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AsrResult transcribe(Path mediaFile) {
		IslandProperties.Whisper cfg = islandProperties.getWhisper();
		Path script = resolveScript(cfg.getScriptPath());
		if (!Files.exists(script)) {
			throw new BusinessException("ASR 脚本不存在: " + script);
		}
		if (!Files.exists(mediaFile)) {
			throw new BusinessException("媒体文件不存在: " + mediaFile);
		}

		List<String> cmd = new ArrayList<>();
		cmd.add(cfg.getPython());
		cmd.add(script.toString());
		cmd.add("--input");
		cmd.add(mediaFile.toAbsolutePath().toString());
		cmd.add("--ffmpeg");
		cmd.add(islandProperties.getFfmpeg().getPath());
		cmd.add("--model");
		cmd.add(cfg.getModel());
		cmd.add("--device");
		cmd.add(cfg.getDevice());

		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(false);
		var env = pb.environment();
		env.put("PYTHONIOENCODING", "utf-8");
		if (cfg.getHfEndpoint() != null && !cfg.getHfEndpoint().isBlank()) {
			env.put("HF_ENDPOINT", cfg.getHfEndpoint());
		}
		env.put("ISLAND_WHISPER_MODEL", cfg.getModel());
		env.put("ISLAND_WHISPER_DEVICE", cfg.getDevice());

		List<String> parseLogs = new ArrayList<>();
		String jsonLine = null;

		try {
			log.info("[视频解析] 启动 Whisper 子进程");
			Process process = pb.start();

			Thread stderrReader = new Thread(() -> readStderr(process, parseLogs));
			stderrReader.start();

			StringBuilder stdout = new StringBuilder();
			try (var reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					stdout.append(line).append('\n');
					if (line.trim().startsWith("{")) {
						jsonLine = line.trim();
					}
				}
			}

			stderrReader.join(TimeUnit.MINUTES.toMillis(30));
			boolean finished = process.waitFor(30, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				throw new BusinessException("语音识别超时，请缩短视频或改用更小模型");
			}
			int exit = process.exitValue();
			if (exit != 0) {
				throw new BusinessException("语音识别失败 (exit=" + exit + ")，请查看日志");
			}
			if (jsonLine == null) {
				jsonLine = stdout.toString().lines()
						.filter(l -> l.trim().startsWith("{"))
						.reduce((a, b) -> b)
						.orElse(null);
			}
			if (jsonLine == null) {
				throw new BusinessException("未收到 ASR JSON 输出");
			}

			JsonNode root = objectMapper.readTree(jsonLine);
			Integer durationMs = root.has("durationMs") && !root.get("durationMs").isNull()
					? root.get("durationMs").asInt() : null;
			List<SentenceCue> sentences = new ArrayList<>();
			for (JsonNode node : root.get("sentences")) {
				sentences.add(new SentenceCue(
						node.get("seq").asInt(),
						node.get("start_ms").asInt(),
						node.get("end_ms").asInt(),
						node.get("text_en").asText("").trim()
				));
			}
			log.info("[视频解析] ASR 完成，共 {} 句", sentences.size());
			return new AsrResult(durationMs, sentences, List.copyOf(parseLogs));
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessException("语音识别异常: " + e.getMessage());
		}
	}

	private void readStderr(Process process, List<String> parseLogs) {
		try (var reader = new BufferedReader(
				new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				parseLogs.add(line);
				log.info("{}", line);
			}
		} catch (Exception e) {
			log.warn("[视频解析] 读取 stderr 失败: {}", e.getMessage());
		}
	}

	private Path resolveScript(String configured) {
		Path path = Path.of(configured);
		if (path.isAbsolute() && Files.exists(path)) {
			return path;
		}
		Path cwd = Path.of(System.getProperty("user.dir"));
		Path fromCwd = cwd.resolve(configured);
		if (Files.exists(fromCwd)) {
			return fromCwd;
		}
		Path fromParent = cwd.getParent() != null ? cwd.getParent().resolve("backend").resolve(configured) : fromCwd;
		if (Files.exists(fromParent)) {
			return fromParent;
		}
		return fromCwd;
	}

	public record SentenceCue(int seq, int startMs, int endMs, String textEn) {}

	public record AsrResult(Integer durationMs, List<SentenceCue> sentences, List<String> logs) {}
}
