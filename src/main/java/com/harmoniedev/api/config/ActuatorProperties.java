package com.harmoniedev.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "actuator")
@Getter
@Setter
public class ActuatorProperties {
	private String username;
	private String password;
}
