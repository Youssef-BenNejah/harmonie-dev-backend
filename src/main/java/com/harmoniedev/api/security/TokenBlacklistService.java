package com.harmoniedev.api.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
	private static final String PREFIX = "blacklist:";
	private final RedisTemplate<String, String> redisTemplate;
	private final JwtTokenProvider jwtTokenProvider;

	public TokenBlacklistService(RedisTemplate<String, String> redisTemplate, JwtTokenProvider jwtTokenProvider) {
		this.redisTemplate = redisTemplate;
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
			redisTemplate.opsForValue().set(PREFIX + token, "1", ttl);
		} catch (Exception ex) {
			// ignore invalid tokens
		}
	}

	public boolean isBlacklisted(String token) {
		Boolean exists = redisTemplate.hasKey(PREFIX + token);
		return Boolean.TRUE.equals(exists);
	}
}
