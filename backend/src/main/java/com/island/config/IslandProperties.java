package com.island.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "island")
public class IslandProperties {

	private Jwt jwt = new Jwt();
	private Upload upload = new Upload();
	private Oss oss = new Oss();
	private Cors cors = new Cors();
	private Ai ai = new Ai();
	private Verification verification = new Verification();
	private Ffmpeg ffmpeg = new Ffmpeg();
	private Whisper whisper = new Whisper();
	private Admin admin = new Admin();

	@Data
	public static class Verification {
		/** redis | memory（本地无 Redis 时用 memory） */
		private String store = "redis";
	}

	@Data
	public static class Jwt {
		private String secret;
		private long expirationMs = 604_800_000L;
	}

	@Data
	public static class Upload {
		private String dir = "../uploads";
		/** local | oss */
		private String storageType = "oss";
		private long maxSizeMb = 200;
	}

	@Data
	public static class Oss {
		private String endpoint = "https://oss-cn-guangzhou.aliyuncs.com";
		private String region = "cn-guangzhou";
		private String bucket = "english-island";
		private String accessKeyId;
		private String accessKeySecret;
		/** 可选：自定义 CDN/域名，不填则按 bucket + endpoint 拼接公网 URL */
		private String publicBaseUrl;
	}

	@Data
	public static class Cors {
		private List<String> allowedOrigins = List.of("http://localhost:3000");
	}

	@Data
	public static class Ffmpeg {
		private String path = "E:/software/ffmpeg/bin/ffmpeg.exe";
	}

	@Data
	public static class Whisper {
		private String python = "python";
		private String scriptPath = "scripts/whisper_transcribe.py";
		private String model = "small";
		private String device = "cuda";
		private String hfEndpoint = "https://hf-mirror.com";
	}

	@Data
	public static class Admin {
		private int minSentencesOnPublish = 1;
	}

	@Data
	public static class Ai {
		private boolean enabled;
		private String provider;
		private String apiKey;
		private String baseUrl;
		private String model = "deepseek-chat";
		private int timeoutSeconds = 120;
		/** 视频字幕中译时每批句子数 */
		private int zhDraftBatchSize = 40;
	}
}
