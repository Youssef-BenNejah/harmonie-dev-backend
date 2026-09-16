package com.harmoniedev.api.auth.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class LoginRequest {
	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String password;

	/** When true, the refresh cookie persists across browser restarts; otherwise it's a session cookie. */
	private boolean rememberMe;
}
