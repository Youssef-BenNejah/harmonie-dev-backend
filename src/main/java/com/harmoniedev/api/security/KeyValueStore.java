package com.harmoniedev.api.security;

import java.time.Duration;

/**
 * Generic TTL key/value store — token blacklist entries and password-reset codes/tokens both need
 * "set with expiry, read once, delete". Redis-backed in the cloud deployment, in-process in the
 * standalone desktop build (see {@link RedisKeyValueStore} / {@link InMemoryKeyValueStore}).
 */
public interface KeyValueStore {
	void set(String key, String value, Duration ttl);

	String get(String key);

	boolean hasKey(String key);

	void delete(String key);
}
