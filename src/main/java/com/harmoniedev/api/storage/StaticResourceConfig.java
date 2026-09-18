package com.harmoniedev.api.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves files written by {@link LocalFileStorageService} back over HTTP in the desktop build. */
@Configuration
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "desktop")
public class StaticResourceConfig implements WebMvcConfigurer {
	private final String localDir;

	public StaticResourceConfig(@Value("${storage.local-dir}") String localDir) {
		this.localDir = localDir;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String location = localDir.endsWith("/") ? localDir : localDir + "/";
		registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + location);
	}
}
