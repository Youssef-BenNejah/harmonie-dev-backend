package com.harmoniedev.api.auth.domain.dto.request;

import com.harmoniedev.api.auth.domain.enums.AccountStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class AdminCreateUserRequest {
	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	private AccountStatus status;

	private Instant planExpiresAt;

	/** Super Admin picks a plan when creating a tenant; null falls back to the seeded free-trial plan. */
	private String planId;
}
