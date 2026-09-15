package com.harmoniedev.api.audit.domain.model;

import com.harmoniedev.api.audit.domain.enums.AuditAction;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDocument {
	@Id
	private String id;
	private String userId;
	private AuditAction action;
	private String ipAddress;
	private String userAgent;
	@CreatedDate
	private Instant createdAt;
}
