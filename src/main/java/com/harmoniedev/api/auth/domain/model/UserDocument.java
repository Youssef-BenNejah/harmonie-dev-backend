package com.harmoniedev.api.auth.domain.model;

import com.harmoniedev.api.auth.domain.enums.AccountStatus;
import com.harmoniedev.api.auth.domain.enums.Role;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDocument {
	@Id
	private String id;
	private String email;
	private String passwordHash;
	private Role role;
	private String firstName;
	private String lastName;
	private String photoUrl;
	@Default
	private AccountStatus status = AccountStatus.ACTIVE;
	private Instant planExpiresAt;
	private String planId;
	@Default
	private boolean renewalRequested = false;
	@Default
	private boolean enabled = true;
	@Default
	private boolean locked = false;
	@Default
	private int failedAttempts = 0;
	private Instant lockExpiresAt;
	@CreatedDate
	private Instant createdAt;
	@LastModifiedDate
	private Instant updatedAt;
}
