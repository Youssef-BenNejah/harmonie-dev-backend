package com.harmoniedev.api.security.filter;

import com.harmoniedev.api.security.ratelimit.LoginRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
	private final LoginRateLimiter rateLimiter;

	public RateLimitingFilter(LoginRateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !HttpMethod.POST.matches(request.getMethod())
				|| !"/api/v1/auth/login".equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String clientIp = resolveClientIp(request);
		if (!rateLimiter.tryConsume("login:" + clientIp)) {
			response.setStatus(429);
			response.setContentType("application/problem+json");
			String body = "{"
					+ "\"type\":\"https://yourapp.com/errors/rate-limit\","
					+ "\"title\":\"Too Many Requests\","
					+ "\"status\":429,"
					+ "\"detail\":\"Too many login attempts\","
					+ "\"instance\":\"" + request.getRequestURI() + "\""
					+ "}";
			response.getWriter().write(body);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
