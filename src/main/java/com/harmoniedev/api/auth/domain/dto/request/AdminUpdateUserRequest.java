package com.harmoniedev.api.auth.domain.dto.request;

import com.harmoniedev.api.auth.domain.enums.AccountStatus;
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
public class AdminUpdateUserRequest {
	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	private AccountStatus status;

	private Instant planExpiresAt;

	/** Super Admin re-assigns the tenant's plan; null leaves the current plan unchanged. */
	private String planId;
}
