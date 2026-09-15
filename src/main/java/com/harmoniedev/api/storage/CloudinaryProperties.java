package com.harmoniedev.api.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
@Getter
@Setter
public class CloudinaryProperties {
	private String cloudName;
	private String apiKey;
	private String apiSecret;
}
