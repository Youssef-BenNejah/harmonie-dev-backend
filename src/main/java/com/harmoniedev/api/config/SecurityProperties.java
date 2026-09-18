package com.harmoniedev.api.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityProperties {
	private int bcryptStrength;
	private List<String> allowedOrigins = new ArrayList<>();
	private int maxLoginAttempts;
	private int lockDurationMinutes;
	/** IPs/CIDRs of reverse proxies allowed to set X-Forwarded-For. Empty = never trust the header. */
	private List<String> trustedProxies = new ArrayList<>();
}
