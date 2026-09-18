package com.harmoniedev.api.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Single-process stand-in for Redis-backed Bucket4j in the desktop build. */
@Service
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "desktop")
public class InMemoryLoginRateLimiter implements LoginRateLimiter {
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	@Override
	public boolean tryConsume(String key) {
		Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
				.addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
				.build());
		return bucket.tryConsume(1);
	}
}
