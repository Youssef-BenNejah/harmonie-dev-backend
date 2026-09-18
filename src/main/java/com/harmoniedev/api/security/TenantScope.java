package com.harmoniedev.api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tenant isolation: every business record carries the id of the account that created it
 * ({@code createdBy}), and a caller may only see or change their own. Services use this instead of
 * trusting ids from the URL, so guessing another customer's id yields a 404, never their data.
 */
public final class TenantScope {
	private TenantScope() {
	}

	public static String currentId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
	}

	public static boolean owns(String createdBy) {
		return createdBy != null && createdBy.equals(currentId());
	}
}
