package com.harmoniedev.api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Single-process stand-in for Redis in the desktop build — good enough since there's only one instance. */
@Service
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "desktop")
public class InMemoryKeyValueStore implements KeyValueStore {
	private record Entry(String value, Instant expiry) {
	}

	private final Map<String, Entry> entries = new ConcurrentHashMap<>();

	@Override
	public void set(String key, String value, Duration ttl) {
		entries.put(key, new Entry(value, Instant.now().plus(ttl)));
	}

	@Override
	public String get(String key) {
		Entry entry = entries.get(key);
		if (entry == null) {
			return null;
		}
		if (entry.expiry().isBefore(Instant.now())) {
			entries.remove(key);
			return null;
		}
		return entry.value();
	}

	@Override
	public boolean hasKey(String key) {
		return get(key) != null;
	}

	@Override
	public void delete(String key) {
		entries.remove(key);
	}

	@Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
	void evictExpired() {
		Instant now = Instant.now();
		entries.values().removeIf(entry -> entry.expiry().isBefore(now));
	}
}
