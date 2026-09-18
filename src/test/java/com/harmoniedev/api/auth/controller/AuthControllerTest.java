package com.harmoniedev.api.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.harmoniedev.api.config.AppProperties;
import com.harmoniedev.api.config.JwtProperties;
import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.auth.domain.dto.response.UserResponse;
import com.harmoniedev.api.security.filter.JwtAuthenticationFilter;
import com.harmoniedev.api.security.filter.RateLimitingFilter;
import com.harmoniedev.api.auth.service.AuthResult;
import com.harmoniedev.api.auth.service.AuthService;
import com.harmoniedev.api.security.AuthenticatedUser;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthService authService;
	@MockBean
	private AppProperties appProperties;
	@MockBean
	private JwtProperties jwtProperties;
	@MockBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;
	@MockBean
	private RateLimitingFilter rateLimitingFilter;
	@MockBean
	private com.harmoniedev.api.security.ClientIpResolver clientIpResolver;

	@Test
	void login_setsRefreshCookie() throws Exception {
		UserResponse user = userResponse("user-1", "test@example.com");
		AuthResult result = AuthResult.builder()
				.accessToken("access")
				.refreshToken("refresh")
				.user(user)
				.build();
		when(authService.login(any(), any(), any())).thenReturn(result);
		when(appProperties.isDev()).thenReturn(true);
		when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800000L);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"test@example.com\",\"password\":\"Aa1!aaaa\"}"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=")));
	}

	@Test
	void register_returnsUser() throws Exception {
		UserResponse user = userResponse("user-2", "new@example.com");
		when(authService.register(any(), any(), any())).thenReturn(user);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"new@example.com\",\"password\":\"Aa1!aaaa\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value("new@example.com"));
	}

	@Test
	void refresh_setsRefreshCookie() throws Exception {
		UserResponse user = userResponse("user-1", "test@example.com");
		AuthResult result = AuthResult.builder()
				.accessToken("new-access")
				.refreshToken("new-refresh")
				.user(user)
				.build();
		when(authService.refresh(eq("refresh"), any(), any())).thenReturn(result);
		when(appProperties.isDev()).thenReturn(true);
		when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800000L);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.cookie(new Cookie("refresh_token", "refresh")))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Path=/api/v1/auth")));
	}

	@Test
	void logout_clearsRefreshCookie() throws Exception {
		when(appProperties.isDev()).thenReturn(true);

		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.cookie(new Cookie("refresh_token", "refresh")))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

		verify(authService).logout(eq("access-token"), eq("refresh"), any(), any());
	}

	@Test
	void me_returnsCurrentUser() throws Exception {
		UserResponse user = userResponse("user-3", "me@example.com");
		when(authService.getCurrentUser("user-3")).thenReturn(user);

		AuthenticatedUser principal = AuthenticatedUser.builder()
				.id("user-3")
				.email("me@example.com")
				.role(Role.USER)
				.build();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal, null, Collections.emptyList());

		mockMvc.perform(get("/api/v1/auth/me").principal(authentication))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value("user-3"))
				.andExpect(jsonPath("$.data.email").value("me@example.com"));
	}

	private UserResponse userResponse(String id, String email) {
		return UserResponse.builder()
				.id(id)
				.email(email)
				.role(Role.USER)
				.createdAt(Instant.parse("2026-03-04T10:00:00Z"))
				.build();
	}
}
