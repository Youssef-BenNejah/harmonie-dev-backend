package com.harmoniedev.api.config;

import com.harmoniedev.api.security.filter.JwtAuthenticationFilter;
import com.harmoniedev.api.security.filter.RateLimitingFilter;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
	@Bean
	public PasswordEncoder passwordEncoder(SecurityProperties properties) {
		return new BCryptPasswordEncoder(properties.getBcryptStrength());
	}

	@Bean
	public UserDetailsService actuatorUserDetailsService(ActuatorProperties properties, PasswordEncoder passwordEncoder) {
		return new InMemoryUserDetailsManager(User.withUsername(properties.getUsername())
				.password(passwordEncoder.encode(properties.getPassword()))
				.roles("ACTUATOR")
				.build());
	}

	@Bean
	public AuthenticationEntryPoint restAuthenticationEntryPoint() {
		return (request, response, authException) -> {
			response.setStatus(401);
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			String body = "{"
					+ "\"type\":\"https://harmonie-dev.netlify.app/errors/unauthorized\","
					+ "\"title\":\"Unauthorized\","
					+ "\"status\":401,"
					+ "\"detail\":\"Authentication required\","
					+ "\"instance\":\"" + request.getRequestURI() + "\""
					+ "}";
			response.getWriter().write(body);
		};
	}

	@Bean
	public AccessDeniedHandler restAccessDeniedHandler() {
		return (request, response, accessDeniedException) -> {
			response.setStatus(403);
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			String body = "{"
					+ "\"type\":\"https://harmonie-dev.netlify.app/errors/forbidden\","
					+ "\"title\":\"Forbidden\","
					+ "\"status\":403,"
					+ "\"detail\":\"Access denied\","
					+ "\"instance\":\"" + request.getRequestURI() + "\""
					+ "}";
			response.getWriter().write(body);
		};
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain apiSecurityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			RateLimitingFilter rateLimitingFilter,
			CorsConfigurationSource corsConfigurationSource,
			AuthenticationEntryPoint restAuthenticationEntryPoint,
			AccessDeniedHandler restAccessDeniedHandler) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-reset-code").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/join-requests").permitAll()
						.requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/v1/join-requests", "/api/v1/join-requests/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/plans", "/api/v1/plans/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/plans").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/plans/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/plans/**").hasRole("ADMIN")
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler))
				.headers(headers -> {
					headers.frameOptions(frame -> frame.deny());
					headers.contentTypeOptions(Customizer.withDefaults());
					headers.referrerPolicy(referrer -> referrer
							.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
					headers.httpStrictTransportSecurity(hsts -> hsts
							.includeSubDomains(true)
							.maxAgeInSeconds(31536000));
					headers.contentSecurityPolicy(csp -> csp
							.policyDirectives("default-src 'self'"));
				});

		http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
