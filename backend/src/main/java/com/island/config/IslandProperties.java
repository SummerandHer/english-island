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
	private Cors cors = new Cors();
	private Ai ai = new Ai();

	@Data
	public static class Jwt {
		private String secret;
		private long expirationMs = 604_800_000L;
	}

	@Data
	public static class Upload {
		private String dir = "../uploads";
	}

	@Data
	public static class Cors {
		private List<String> allowedOrigins = List.of("http://localhost:3000");
	}

	@Data
	public static class Ai {
		private boolean enabled;
		private String provider;
		private String apiKey;
		private String baseUrl;
	}
}
