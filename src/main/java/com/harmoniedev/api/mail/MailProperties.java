package com.harmoniedev.api.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
@Getter
@Setter
public class MailProperties {
	private String fromAddress;
	private String fromName;
}
