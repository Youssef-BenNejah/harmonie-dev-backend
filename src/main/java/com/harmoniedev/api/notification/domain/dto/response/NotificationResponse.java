package com.harmoniedev.api.notification.domain.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
	private String id;
	private String type;
	private String tenantAdminId;
	private String title;
	private String message;
	private Instant createdAt;
	private boolean read;
}
