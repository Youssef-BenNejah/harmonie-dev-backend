package com.harmoniedev.api.security;

import com.harmoniedev.api.auth.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthenticatedUser {
	private final String id;
	private final String email;
	private final Role role;
}
