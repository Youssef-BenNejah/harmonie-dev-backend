package com.harmoniedev.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
	private String env;
	private int port;
	private String frontendUrl;

	public boolean isDev() {
		return env != null && env.equalsIgnoreCase("dev");
	}
}
