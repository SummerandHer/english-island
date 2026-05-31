package com.island.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AiRestConfig {

	@Bean
	@Qualifier("aiRestTemplate")
	public RestTemplate aiRestTemplate(IslandProperties islandProperties) {
		var factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(15));
		factory.setReadTimeout(Duration.ofSeconds(islandProperties.getAi().getTimeoutSeconds()));
		return new RestTemplate(factory);
	}
}
