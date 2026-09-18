package com.harmoniedev.api.security.ratelimit;

/** Caps login attempts per key (IP). Redis-backed in the cloud deployment, in-process in the desktop build. */
public interface LoginRateLimiter {
	boolean tryConsume(String key);
}
