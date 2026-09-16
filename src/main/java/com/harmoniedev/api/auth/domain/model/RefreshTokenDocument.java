package com.harmoniedev.api.auth.domain.model;

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

@Document(collection = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenDocument {
	@Id
	private String id;
	private String tokenHash;
	private String userId;
	private Instant expiresAt;
	@Default
	private boolean revoked = false;
	@Default
	private boolean rememberMe = false;
	@CreatedDate
	private Instant createdAt;
}
