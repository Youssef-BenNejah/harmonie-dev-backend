package com.harmoniedev.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.harmoniedev.api.audit.domain.enums.AuditAction;
import com.harmoniedev.api.audit.service.AuditService;
import com.harmoniedev.api.config.SecurityProperties;
import com.harmoniedev.api.security.TokenBlacklistService;
import com.harmoniedev.api.auth.domain.model.RefreshTokenDocument;
import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import com.harmoniedev.api.auth.domain.dto.request.LoginRequest;
import com.harmoniedev.api.auth.domain.dto.request.RegisterRequest;
import com.harmoniedev.api.auth.domain.dto.response.UserResponse;
import com.harmoniedev.api.auth.repository.RefreshTokenRepository;
import com.harmoniedev.api.auth.repository.UserRepository;
import com.harmoniedev.api.mail.MailService;
import com.harmoniedev.api.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
	@Mock
	private UserRepository userRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private TokenBlacklistService tokenBlacklistService;
	@Mock
	private SecurityProperties securityProperties;
	@Mock
	private AuditService auditService;
	@Mock
	private MailService mailService;
	@Mock
	private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
	@Mock
	private com.harmoniedev.api.storage.CloudinaryService cloudinaryService;
	@Mock
	private com.harmoniedev.api.notification.service.NotificationService notificationService;
	@Mock
	private com.harmoniedev.api.plan.repository.PlanRepository planRepository;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				userRepository,
				refreshTokenRepository,
				passwordEncoder,
				jwtTokenProvider,
				tokenBlacklistService,
				securityProperties,
				auditService,
				mailService,
				redisTemplate,
				cloudinaryService,
				notificationService,
				planRepository);
	}

	@Test
	void register_createsUser_andReturnsResponse() {
		RegisterRequest request = RegisterRequest.builder()
				.email("Test@Example.com")
				.password("Aa1!aaaa")
				.build();

		when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
		when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("hashed");
		when(planRepository.findByIsFreeTrialTrue()).thenReturn(Optional.empty());
		when(userRepository.save(any(UserDocument.class))).thenAnswer(invocation -> {
			UserDocument saved = invocation.getArgument(0);
			saved.setId("user-1");
			saved.setCreatedAt(Instant.parse("2026-03-04T10:00:00Z"));
			return saved;
		});
		doNothing().when(auditService).log(eq("user-1"), eq(AuditAction.REGISTER), any(), any());

		UserResponse response = authService.register(request, "1.2.3.4", "agent");

		assertThat(response.getId()).isEqualTo("user-1");
		assertThat(response.getEmail()).isEqualTo("test@example.com");
		assertThat(response.getRole()).isEqualTo(Role.USER);
		verify(userRepository).save(any(UserDocument.class));
		verify(auditService).log("user-1", AuditAction.REGISTER, "1.2.3.4", "agent");
	}

	@Test
	void register_duplicateEmail_throwsConflict() {
		RegisterRequest request = RegisterRequest.builder()
				.email("test@example.com")
				.password("Aa1!aaaa")
				.build();
		when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request, "1.2.3.4", "agent"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("409 CONFLICT");
		verify(userRepository, never()).save(any(UserDocument.class));
	}

	@Test
	void login_success_returnsTokensAndUser() {
		LoginRequest request = LoginRequest.builder()
				.email("test@example.com")
				.password("Aa1!aaaa")
				.build();
		UserDocument user = UserDocument.builder()
				.id("user-1")
				.email("test@example.com")
				.passwordHash("hashed")
				.role(Role.USER)
				.enabled(true)
				.locked(false)
				.failedAttempts(0)
				.build();

		when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Aa1!aaaa", "hashed")).thenReturn(true);
		when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access");
		when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh");
		when(jwtTokenProvider.getExpiration("refresh")).thenReturn(Instant.parse("2026-03-05T10:00:00Z"));

		AuthResult result = authService.login(request, "1.2.3.4", "agent");

		assertThat(result.getAccessToken()).isEqualTo("access");
		assertThat(result.getRefreshToken()).isEqualTo("refresh");
		assertThat(result.getUser().getId()).isEqualTo("user-1");
		verify(refreshTokenRepository).save(any(RefreshTokenDocument.class));
		verify(auditService).log("user-1", AuditAction.LOGIN, "1.2.3.4", "agent");
	}

	@Test
	void login_wrongPassword_incrementsAttempts_andLocksWhenThresholdReached() {
		LoginRequest request = LoginRequest.builder()
				.email("test@example.com")
				.password("WrongPass1!")
				.build();
		UserDocument user = UserDocument.builder()
				.id("user-1")
				.email("test@example.com")
				.passwordHash("hashed")
				.role(Role.USER)
				.enabled(true)
				.locked(false)
				.failedAttempts(2)
				.build();

		when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("WrongPass1!", "hashed")).thenReturn(false);
		when(securityProperties.getMaxLoginAttempts()).thenReturn(3);
		when(securityProperties.getLockDurationMinutes()).thenReturn(30);

		assertThatThrownBy(() -> authService.login(request, "1.2.3.4", "agent"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("401 UNAUTHORIZED");

		ArgumentCaptor<UserDocument> captor = ArgumentCaptor.forClass(UserDocument.class);
		verify(userRepository).save(captor.capture());
		UserDocument saved = captor.getValue();
		assertThat(saved.getFailedAttempts()).isEqualTo(3);
		assertThat(saved.isLocked()).isTrue();
		assertThat(saved.getLockExpiresAt()).isNotNull();
		verify(auditService).log("user-1", AuditAction.LOGIN_FAILED, "1.2.3.4", "agent");
	}

	@Test
	void refresh_success_rotatesToken_andRevokesOld() {
		UserDocument user = UserDocument.builder()
				.id("user-1")
				.email("test@example.com")
				.passwordHash("hashed")
				.role(Role.USER)
				.enabled(true)
				.build();
		String oldRefresh = "old-refresh-token";
		String hashedOld = sha256Base64(oldRefresh);
		RefreshTokenDocument stored = RefreshTokenDocument.builder()
				.id("rt-1")
				.userId("user-1")
				.tokenHash(hashedOld)
				.revoked(false)
				.expiresAt(Instant.parse("2026-03-05T10:00:00Z"))
				.build();

		when(jwtTokenProvider.getTokenType(oldRefresh)).thenReturn("refresh");
		when(jwtTokenProvider.getSubject(oldRefresh)).thenReturn("user-1");
		when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
		when(refreshTokenRepository.findByUserIdAndRevokedFalse("user-1")).thenReturn(List.of(stored));
		when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access");
		when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("new-refresh");
		when(jwtTokenProvider.getExpiration("new-refresh")).thenReturn(Instant.parse("2026-03-06T10:00:00Z"));

		AuthResult result = authService.refresh(oldRefresh, "1.2.3.4", "agent");

		assertThat(result.getAccessToken()).isEqualTo("new-access");
		assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
		verify(refreshTokenRepository).save(stored);
		assertThat(stored.isRevoked()).isTrue();
		verify(auditService).log("user-1", AuditAction.REFRESH, "1.2.3.4", "agent");
	}

	private String sha256Base64(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
