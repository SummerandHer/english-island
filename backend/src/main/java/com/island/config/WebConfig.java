package com.island.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final IslandProperties islandProperties;

	public WebConfig(IslandProperties islandProperties) {
		this.islandProperties = islandProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String dir = islandProperties.getUpload().getDir();
		if (!dir.endsWith("/") && !dir.endsWith("\\")) {
			dir = dir + "/";
		}
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:" + dir);
	}
}
