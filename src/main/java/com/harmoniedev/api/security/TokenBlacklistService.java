package com.harmoniedev.api.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
	private static final String PREFIX = "blacklist:";
	private final KeyValueStore store;
	private final JwtTokenProvider jwtTokenProvider;

	public TokenBlacklistService(KeyValueStore store, JwtTokenProvider jwtTokenProvider) {
		this.store = store;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	public void blacklist(String token) {
		try {
			Instant expiration = jwtTokenProvider.getExpiration(token);
			Instant now = Instant.now();
			if (expiration.isBefore(now)) {
				return;
			}
			Duration ttl = Duration.between(now, expiration);
			store.set(PREFIX + token, "1", ttl);
		} catch (Exception ex) {
			// ignore invalid tokens
		}
	}

	public boolean isBlacklisted(String token) {
		return store.hasKey(PREFIX + token);
	}
}
