package com.harmoniedev.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seed")
@Getter
@Setter
public class SeedProperties {
	private String adminEmail;
	private String adminPassword;
}
