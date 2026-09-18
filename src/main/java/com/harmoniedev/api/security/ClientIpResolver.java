package com.harmoniedev.api.security;

import com.harmoniedev.api.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/**
 * Works out the caller's IP for rate limiting and audit logs.
 *
 * <p>{@code X-Forwarded-For} is written by whoever sends the request, so it is only believed when
 * the TCP peer is one of the proxies listed in {@code TRUSTED_PROXIES} (IPs or CIDRs). With none
 * configured the header is ignored and the socket address is used, which cannot be forged. Behind a
 * trusted proxy chain the client is the right-most entry that is not itself a trusted proxy — the
 * left-most entries are whatever the client claimed.
 */
@Component
public class ClientIpResolver {
	private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");
	private final List<IpAddressMatcher> trustedProxies;

	public ClientIpResolver(SecurityProperties properties) {
		this.trustedProxies = properties.getTrustedProxies().stream()
				.map(String::trim)
				.filter(entry -> !entry.isEmpty())
				.map(IpAddressMatcher::new)
				.toList();
	}

	public String resolve(HttpServletRequest request) {
		String peer = request.getRemoteAddr();
		if (trustedProxies.isEmpty() || !isTrusted(peer)) {
			return peer;
		}
		String header = request.getHeader("X-Forwarded-For");
		if (header == null || header.isBlank()) {
			return peer;
		}
		String[] hops = header.split(",");
		for (int i = hops.length - 1; i >= 0; i--) {
			String hop = hops[i].trim();
			if (!IP_LITERAL.matcher(hop).matches()) {
				return peer;
			}
			if (!isTrusted(hop)) {
				return hop;
			}
		}
		return peer;
	}

	private boolean isTrusted(String ip) {
		if (ip == null || !IP_LITERAL.matcher(ip).matches()) {
			return false;
		}
		for (IpAddressMatcher matcher : trustedProxies) {
			try {
				if (matcher.matches(ip)) {
					return true;
				}
			} catch (IllegalArgumentException ex) {
				return false;
			}
		}
		return false;
	}
}
