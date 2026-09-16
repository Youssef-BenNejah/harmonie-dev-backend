package com.harmoniedev.api.auth.service;

import com.harmoniedev.api.auth.domain.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthResult {
	private final String accessToken;
	private final String refreshToken;
	private final UserResponse user;
	private final boolean rememberMe;
}
