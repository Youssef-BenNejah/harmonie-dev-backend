package com.harmoniedev.api.notification.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDocument {
	@Id
	private String id;

	/** "renewal_request" | "renewal_approved" | "renewal_rejected" | "join_request" — extensible. */
	private String type;

	/** Subject of the notification — the tenant user id, or (for join_request) the JoinRequest id. */
	private String tenantAdminId;

	private String title;
	private String message;

	@CreatedDate
	private Instant createdAt;

	@Default
	private boolean read = false;

	/** true = visible to Super Admins (platform-wide); false = visible only to the tenant named by tenantAdminId. */
	private boolean forAdmin;
}
