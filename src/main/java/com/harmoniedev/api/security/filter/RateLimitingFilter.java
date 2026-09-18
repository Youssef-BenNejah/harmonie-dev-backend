package com.harmoniedev.api.security.filter;

import com.harmoniedev.api.security.ClientIpResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.BucketConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
	private static final Set<String> LIMITED_PATHS = Set.of(
			"/api/v1/auth/login",
			"/api/v1/auth/register",
			"/api/v1/auth/forgot-password",
			"/api/v1/auth/verify-reset-code",
			"/api/v1/auth/reset-password",
			"/api/v1/join-requests");
	private final ProxyManager<byte[]> proxyManager;
	private final ClientIpResolver clientIpResolver;
	private final BucketConfiguration configuration;

	public RateLimitingFilter(ProxyManager<byte[]> proxyManager, ClientIpResolver clientIpResolver) {
		this.proxyManager = proxyManager;
		this.clientIpResolver = clientIpResolver;
		this.configuration = BucketConfiguration.builder()
				.addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
				.build();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !HttpMethod.POST.matches(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String clientIp = clientIpResolver.resolve(request);
		byte[] bucketKey = (request.getRequestURI() + ":" + clientIp).getBytes(StandardCharsets.UTF_8);
		Bucket bucket = proxyManager.builder().build(bucketKey, configuration);
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (!probe.isConsumed()) {
			response.setStatus(429);
			response.setContentType("application/problem+json");
			String body = "{"
					+ "\"type\":\"https://yourapp.com/errors/rate-limit\","
					+ "\"title\":\"Too Many Requests\","
					+ "\"status\":429,"
					+ "\"detail\":\"Too many attempts, try again in a minute\","
					+ "\"instance\":\"" + request.getRequestURI() + "\""
					+ "}";
			response.getWriter().write(body);
			return;
		}
		filterChain.doFilter(request, response);
	}

}
