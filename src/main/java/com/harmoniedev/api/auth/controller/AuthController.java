package com.harmoniedev.api.auth.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.config.AppProperties;
import com.harmoniedev.api.config.JwtProperties;
import com.harmoniedev.api.auth.domain.dto.request.AdminCreateUserRequest;
import com.harmoniedev.api.auth.domain.dto.request.AdminUpdateUserRequest;
import com.harmoniedev.api.auth.domain.dto.request.ForgotPasswordRequest;
import com.harmoniedev.api.auth.domain.dto.request.LoginRequest;
import com.harmoniedev.api.auth.domain.dto.request.RegisterRequest;
import com.harmoniedev.api.auth.domain.dto.request.ResetPasswordRequest;
import com.harmoniedev.api.auth.domain.dto.request.UpdateProfileRequest;
import com.harmoniedev.api.auth.domain.dto.request.VerifyResetCodeRequest;
import com.harmoniedev.api.auth.domain.dto.response.AuthResponse;
import com.harmoniedev.api.auth.domain.dto.response.ResetCodeVerifiedResponse;
import com.harmoniedev.api.auth.domain.dto.response.UserResponse;
import com.harmoniedev.api.auth.service.AuthResult;
import com.harmoniedev.api.auth.service.AuthService;
import com.harmoniedev.api.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private static final String REFRESH_COOKIE = "refresh_token";
	private final AuthService authService;
	private final AppProperties appProperties;
	private final JwtProperties jwtProperties;

	public AuthController(AuthService authService, AppProperties appProperties, JwtProperties jwtProperties) {
		this.authService = authService;
		this.appProperties = appProperties;
		this.jwtProperties = jwtProperties;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(
			@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest) {
		UserResponse user = authService.register(request, resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Registration successful", user));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		AuthResult result = authService.login(request, resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		setRefreshCookie(httpResponse, result.getRefreshToken());
		AuthResponse response = AuthResponse.builder()
				.accessToken(result.getAccessToken())
				.user(result.getUser())
				.build();
		return ResponseEntity.ok(ApiResponse.success("Login successful", response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthResponse>> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		AuthResult result = authService.refresh(refreshToken, resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		setRefreshCookie(httpResponse, result.getRefreshToken());
		AuthResponse response = AuthResponse.builder()
				.accessToken(result.getAccessToken())
				.user(result.getUser())
				.build();
		return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		String accessToken = extractBearer(authorization);
		authService.logout(accessToken, refreshToken, resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		clearRefreshCookie(httpResponse);
		return ResponseEntity.ok(ApiResponse.success("Logged out", null));
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		UserResponse user = authService.getCurrentUser(principal.getId());
		return ResponseEntity.ok(ApiResponse.success("Current user", user));
	}

	@PutMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
			@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		UserResponse user = authService.updateOwnProfile(principal.getId(), request);
		return ResponseEntity.ok(ApiResponse.success("Profile updated", user));
	}

	@PostMapping("/me/photo")
	public ResponseEntity<ApiResponse<UserResponse>> uploadPhoto(
			@RequestParam("file") MultipartFile file, Authentication authentication) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		UserResponse user = authService.uploadOwnPhoto(principal.getId(), file);
		return ResponseEntity.ok(ApiResponse.success("Photo uploaded", user));
	}

	/** Super Admin only (see SecurityConfig) — provisions a tenant account and e-mails the credentials. */
	@PostMapping("/admin/users")
	public ResponseEntity<ApiResponse<UserResponse>> createUser(
			@Valid @RequestBody AdminCreateUserRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		UserResponse user = authService.createUserByAdmin(
				request, principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("User created — credentials sent by e-mail", user));
	}

	/** Super Admin only (see SecurityConfig) — lists every tenant account. */
	@GetMapping("/admin/users")
	public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers() {
		return ResponseEntity.ok(ApiResponse.success("Users", authService.listTenantUsers()));
	}

	@PutMapping("/admin/users/{id}")
	public ResponseEntity<ApiResponse<UserResponse>> updateUser(
			@PathVariable String id,
			@Valid @RequestBody AdminUpdateUserRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		UserResponse user = authService.updateUserByAdmin(
				id, request, principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("User updated", user));
	}

	@DeleteMapping("/admin/users/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteUser(
			@PathVariable String id, Authentication authentication, HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		authService.deleteUserByAdmin(id, principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("User deleted", null));
	}

	@PostMapping("/admin/users/{id}/approve-renewal")
	public ResponseEntity<ApiResponse<UserResponse>> approveRenewal(
			@PathVariable String id, Authentication authentication, HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		UserResponse user = authService.approveRenewal(
				id, 30, principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Renewal approved", user));
	}

	@PostMapping("/admin/users/{id}/reject-renewal")
	public ResponseEntity<ApiResponse<UserResponse>> rejectRenewal(
			@PathVariable String id, Authentication authentication, HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		UserResponse user = authService.rejectRenewal(
				id, principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Renewal rejected", user));
	}

	@PostMapping("/admin/users/{id}/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPasswordByAdmin(
			@PathVariable String id, Authentication authentication, HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		authService.resetPasswordByAdmin(id, principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Password reset — new credentials sent by e-mail", null));
	}

	/** Self-service — a logged-in tenant requests a renewal of their own plan. */
	@PostMapping("/request-renewal")
	public ResponseEntity<ApiResponse<Void>> requestRenewal(
			Authentication authentication, HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		authService.requestRenewal(principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Renewal requested", null));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> forgotPassword(
			@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
		authService.requestPasswordReset(request.getEmail(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("If that e-mail exists, a code has been sent", null));
	}

	@PostMapping("/verify-reset-code")
	public ResponseEntity<ApiResponse<ResetCodeVerifiedResponse>> verifyResetCode(
			@Valid @RequestBody VerifyResetCodeRequest request) {
		String resetToken = authService.verifyResetCode(request.getEmail(), request.getCode());
		return ResponseEntity.ok(ApiResponse.success("Code verified", ResetCodeVerifiedResponse.builder()
				.resetToken(resetToken)
				.build()));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(
			@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
		authService.resetPassword(
				request.getResetToken(), request.getNewPassword(),
				resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Password updated", null));
	}

	private void setRefreshCookie(HttpServletResponse response, String token) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, token)
				.httpOnly(true)
				.secure(!appProperties.isDev())
				.sameSite("Strict")
				.path("/api/v1/auth")
				.maxAge(Duration.ofMillis(jwtProperties.getRefreshTokenExpiry()))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private void clearRefreshCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
				.httpOnly(true)
				.secure(!appProperties.isDev())
				.sameSite("Strict")
				.path("/api/v1/auth")
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private String extractBearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return null;
		}
		return authorization.substring(7);
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
