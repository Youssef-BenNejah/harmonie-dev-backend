package com.harmoniedev.api.auth.domain.dto.response;

import com.harmoniedev.api.auth.domain.enums.AccountStatus;
import com.harmoniedev.api.auth.domain.enums.Role;
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
public class UserResponse {
	private String id;
	private String email;
	private String firstName;
	private String lastName;
	private String photoUrl;
	private Role role;
	private AccountStatus status;
	private Instant planExpiresAt;
	private String planId;
	private boolean renewalRequested;
	private Instant createdAt;
}
