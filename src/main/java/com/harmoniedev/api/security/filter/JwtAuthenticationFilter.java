package com.harmoniedev.api.security.filter;

import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.security.TokenBlacklistService;
import com.harmoniedev.api.security.AuthenticatedUser;
import com.harmoniedev.api.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider jwtTokenProvider;
	private final TokenBlacklistService tokenBlacklistService;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenBlacklistService tokenBlacklistService) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.tokenBlacklistService = tokenBlacklistService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			if (jwtTokenProvider.isValid(token)
					&& !tokenBlacklistService.isBlacklisted(token)
					&& SecurityContextHolder.getContext().getAuthentication() == null) {
				String tokenType = jwtTokenProvider.getTokenType(token);
				if ("access".equals(tokenType)) {
					Claims claims = jwtTokenProvider.parseClaims(token);
					String userId = claims.getSubject();
					String email = claims.get("email", String.class);
					String roleValue = claims.get("role", String.class);
					Role role = Role.USER;
					if (roleValue != null) {
						try {
							role = Role.valueOf(roleValue);
						} catch (IllegalArgumentException ignored) {
							role = Role.USER;
						}
					}
					AuthenticatedUser principal = AuthenticatedUser.builder()
							.id(userId)
							.email(email)
							.role(role)
							.build();
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							principal,
							null,
							List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		}
		filterChain.doFilter(request, response);
	}
}
