package com.harmoniedev.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
	private String privateKey;
	private String publicKey;
	private long accessTokenExpiry;
	private long refreshTokenExpiry;
}
