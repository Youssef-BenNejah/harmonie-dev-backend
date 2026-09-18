package com.harmoniedev.api.auth.service;

import com.harmoniedev.api.audit.domain.enums.AuditAction;
import com.harmoniedev.api.audit.service.AuditService;
import com.harmoniedev.api.auth.domain.dto.request.AdminCreateUserRequest;
import com.harmoniedev.api.auth.domain.dto.request.AdminUpdateUserRequest;
import com.harmoniedev.api.auth.domain.dto.request.LoginRequest;
import com.harmoniedev.api.auth.domain.dto.request.RegisterRequest;
import com.harmoniedev.api.auth.domain.dto.request.UpdateProfileRequest;
import com.harmoniedev.api.auth.domain.dto.response.UserResponse;
import com.harmoniedev.api.auth.domain.enums.AccountStatus;
import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.auth.domain.model.RefreshTokenDocument;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import com.harmoniedev.api.auth.repository.RefreshTokenRepository;
import com.harmoniedev.api.auth.repository.UserRepository;
import com.harmoniedev.api.config.SecurityProperties;
import com.harmoniedev.api.mail.MailService;
import com.harmoniedev.api.currency.domain.model.CurrencyDocument;
import com.harmoniedev.api.currency.repository.CurrencyRepository;
import com.harmoniedev.api.notification.service.NotificationService;
import com.harmoniedev.api.plan.domain.model.PlanDocument;
import com.harmoniedev.api.plan.repository.PlanRepository;
import com.harmoniedev.api.security.JwtTokenProvider;
import com.harmoniedev.api.security.TokenBlacklistService;
import com.harmoniedev.api.storage.FileStorageService;
import com.harmoniedev.api.storage.CloudinaryUploadResult;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import com.harmoniedev.api.security.KeyValueStore;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
	private static final String RESET_CODE_PREFIX = "pwreset:code:";
	private static final String RESET_TOKEN_PREFIX = "pwreset:token:";
	private static final Duration RESET_CODE_TTL = Duration.ofMinutes(10);
	private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
	private static final String PASSWORD_CHARS =
			"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final TokenBlacklistService tokenBlacklistService;
	private final SecurityProperties securityProperties;
	private final AuditService auditService;
	private final MailService mailService;
	private final KeyValueStore keyValueStore;
	private final FileStorageService cloudinaryService;
	private final NotificationService notificationService;
	private final PlanRepository planRepository;
	private final CurrencyRepository currencyRepository;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider,
			TokenBlacklistService tokenBlacklistService,
			SecurityProperties securityProperties,
			AuditService auditService,
			MailService mailService,
			KeyValueStore keyValueStore,
			FileStorageService cloudinaryService,
			NotificationService notificationService,
			PlanRepository planRepository,
			CurrencyRepository currencyRepository) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.tokenBlacklistService = tokenBlacklistService;
		this.securityProperties = securityProperties;
		this.auditService = auditService;
		this.mailService = mailService;
		this.keyValueStore = keyValueStore;
		this.cloudinaryService = cloudinaryService;
		this.notificationService = notificationService;
		this.planRepository = planRepository;
		this.currencyRepository = currencyRepository;
	}

	/** Every new tenant starts with a usable default currency — otherwise invoicing has nothing to select. */
	private void seedDefaultCurrency(String userId) {
		currencyRepository.save(CurrencyDocument.builder()
				.code("TND")
				.name("Dinar tunisien")
				.symbol("DT")
				.createdBy(userId)
				.build());
	}

	public UserResponse register(RegisterRequest request, String ipAddress, String userAgent) {
		String email = request.getEmail().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}
		PlanDocument trialPlan = freeTrialPlan();
		UserDocument user = UserDocument.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.role(Role.USER)
				.planId(trialPlan != null ? trialPlan.getId() : null)
				.planExpiresAt(trialPlan != null && trialPlan.getTrialDurationDays() != null
						? Instant.now().plus(trialPlan.getTrialDurationDays(), java.time.temporal.ChronoUnit.DAYS)
						: null)
				.build();
		userRepository.save(user);
		seedDefaultCurrency(user.getId());
		auditService.log(user.getId(), AuditAction.REGISTER, ipAddress, userAgent);
		return toUserResponse(user);
	}

	/** The single plan flagged isFreeTrial=true, auto-assigned to brand-new tenants. May be absent if Super Admin removed it. */
	private PlanDocument freeTrialPlan() {
		return planRepository.findByIsFreeTrialTrue().orElse(null);
	}

	public AuthResult login(LoginRequest request, String ipAddress, String userAgent) {

		UserDocument user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
		if (!user.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
		}
		unlockIfExpired(user);
		if (user.isLocked()) {
			auditService.log(user.getId(), AuditAction.LOGIN_FAILED, ipAddress, userAgent);
			throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked");
		}
		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			handleFailedLogin(user);
			auditService.log(user.getId(), AuditAction.LOGIN_FAILED, ipAddress, userAgent);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
		resetFailedAttempts(user);
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);
		saveRefreshToken(user, refreshToken, request.isRememberMe());
		auditService.log(user.getId(), AuditAction.LOGIN, ipAddress, userAgent);
		return AuthResult.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.user(toUserResponse(user))
				.rememberMe(request.isRememberMe())
				.build();
	}

	public AuthResult refresh(String refreshToken, String ipAddress, String userAgent) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing");
		}
		String tokenType;
		String userId;
		try {
			tokenType = jwtTokenProvider.getTokenType(refreshToken);
			userId = jwtTokenProvider.getSubject(refreshToken);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}
		if (!"refresh".equals(tokenType)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}
		UserDocument user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
		if (!user.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
		}
		RefreshTokenDocument storedToken = findActiveRefreshToken(userId, refreshToken);
		boolean rememberMe = storedToken.isRememberMe();
		storedToken.setRevoked(true);
		refreshTokenRepository.save(storedToken);

		String newAccessToken = jwtTokenProvider.generateAccessToken(user);
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
		saveRefreshToken(user, newRefreshToken, rememberMe);
		auditService.log(user.getId(), AuditAction.REFRESH, ipAddress, userAgent);
		return AuthResult.builder()
				.accessToken(newAccessToken)
				.refreshToken(newRefreshToken)
				.user(toUserResponse(user))
				.rememberMe(rememberMe)
				.build();
	}

	public void logout(String accessToken, String refreshToken, String ipAddress, String userAgent) {
		if (accessToken != null && !accessToken.isBlank()) {
			tokenBlacklistService.blacklist(accessToken);
		}
		if (refreshToken != null && !refreshToken.isBlank()) {
			try {
				String userId = jwtTokenProvider.getSubject(refreshToken);
				RefreshTokenDocument storedToken = findActiveRefreshToken(userId, refreshToken);
				storedToken.setRevoked(true);
				refreshTokenRepository.save(storedToken);
				auditService.log(userId, AuditAction.LOGOUT, ipAddress, userAgent);
			} catch (Exception ex) {
				// ignore invalid refresh token during logout
			}
		}
	}

	public UserResponse getCurrentUser(String userId) {
		UserDocument user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		return toUserResponse(user);
	}

	/**
	 * Provisions a new tenant account from the Super Admin panel (or from a converted join
	 * request): generates a temporary password, saves the user, and e-mails the credentials.
	 * The raw password is never returned to the caller — it only ever leaves the server by e-mail.
	 */
	public UserResponse createUserByAdmin(AdminCreateUserRequest request, String actorId, String ipAddress, String userAgent) {
		String email = request.getEmail().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}
		String rawPassword = generateRandomPassword();
		PlanDocument plan = request.getPlanId() != null && !request.getPlanId().isBlank()
				? planRepository.findById(request.getPlanId())
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"))
				: freeTrialPlan();
		Instant planExpiresAt = request.getPlanExpiresAt() != null
				? request.getPlanExpiresAt()
				: (plan != null && plan.isFreeTrial() && plan.getTrialDurationDays() != null
						? Instant.now().plus(plan.getTrialDurationDays(), java.time.temporal.ChronoUnit.DAYS)
						: null);
		UserDocument user = UserDocument.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(rawPassword))
				.role(Role.USER)
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.status(request.getStatus() != null ? request.getStatus() : AccountStatus.ACTIVE)
				.planId(plan != null ? plan.getId() : null)
				.planExpiresAt(planExpiresAt)
				.build();
		userRepository.save(user);
		seedDefaultCurrency(user.getId());
		auditService.log(actorId, AuditAction.ADMIN_CREATE_USER, ipAddress, userAgent);
		mailService.sendWelcomeEmail(email, request.getFirstName(), rawPassword);
		return toUserResponse(user);
	}

	/** Self-service — the current user edits their own name. */
	public UserResponse updateOwnProfile(String userId, UpdateProfileRequest request) {
		UserDocument user = findUserOrThrow(userId);
		user.setFirstName(request.getFirstName());
		user.setLastName(request.getLastName());
		userRepository.save(user);
		return toUserResponse(user);
	}

	/** Self-service — the current user uploads their own profile photo. */
	public UserResponse uploadOwnPhoto(String userId, MultipartFile file) {
		UserDocument user = findUserOrThrow(userId);
		CloudinaryUploadResult result = cloudinaryService.uploadImage(file, "profile-photos/" + userId, "photo");
		user.setPhotoUrl(result.url());
		userRepository.save(user);
		return toUserResponse(user);
	}

	/** Super Admin only — lists every tenant (USER role) account, excluding other Super Admins. */
	public List<UserResponse> listTenantUsers() {
		return userRepository.findByRole(Role.USER).stream().map(this::toUserResponse).toList();
	}

	public UserResponse updateUserByAdmin(String id, AdminUpdateUserRequest request, String actorId, String ipAddress, String userAgent) {
		UserDocument user = findUserOrThrow(id);
		user.setFirstName(request.getFirstName());
		user.setLastName(request.getLastName());
		if (request.getStatus() != null) {
			user.setStatus(request.getStatus());
		}
		user.setPlanExpiresAt(request.getPlanExpiresAt());
		if (request.getPlanId() != null && !request.getPlanId().isBlank()) {
			planRepository.findById(request.getPlanId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
			user.setPlanId(request.getPlanId());
		}
		userRepository.save(user);
		auditService.log(actorId, AuditAction.ADMIN_UPDATE_USER, ipAddress, userAgent);
		return toUserResponse(user);
	}

	public void deleteUserByAdmin(String id, String actorId, String ipAddress, String userAgent) {
		UserDocument user = findUserOrThrow(id);
		List<RefreshTokenDocument> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());
		activeTokens.forEach(token -> token.setRevoked(true));
		refreshTokenRepository.saveAll(activeTokens);
		userRepository.delete(user);
		auditService.log(actorId, AuditAction.ADMIN_DELETE_USER, ipAddress, userAgent);
	}

	/** Self-service — a tenant asks the Super Admin for a plan renewal. */
	public void requestRenewal(String userId, String ipAddress, String userAgent) {
		UserDocument user = findUserOrThrow(userId);
		user.setRenewalRequested(true);
		userRepository.save(user);
		auditService.log(userId, AuditAction.RENEWAL_REQUESTED, ipAddress, userAgent);
		String who = (user.getFirstName() + " " + user.getLastName()).trim();
		notificationService.create(
				"renewal_request", userId, "Demande de renouvellement",
				(who.isBlank() ? user.getEmail() : who) + " demande un renouvellement de son abonnement.", true);
	}

	public UserResponse approveRenewal(String id, long extensionDays, String actorId, String ipAddress, String userAgent) {
		UserDocument user = findUserOrThrow(id);
		Instant base = user.getPlanExpiresAt() != null && user.getPlanExpiresAt().isAfter(Instant.now())
				? user.getPlanExpiresAt() : Instant.now();
		user.setPlanExpiresAt(base.plus(Duration.ofDays(extensionDays)));
		user.setRenewalRequested(false);
		user.setStatus(AccountStatus.ACTIVE);
		userRepository.save(user);
		auditService.log(actorId, AuditAction.RENEWAL_APPROVED, ipAddress, userAgent);
		notificationService.create(
				"renewal_approved", id, "Renouvellement approuvé",
				"Votre demande de renouvellement a été approuvée — votre abonnement a été prolongé de " + extensionDays + " jours.",
				false);
		return toUserResponse(user);
	}

	public UserResponse rejectRenewal(String id, String actorId, String ipAddress, String userAgent) {
		UserDocument user = findUserOrThrow(id);
		user.setRenewalRequested(false);
		userRepository.save(user);
		auditService.log(actorId, AuditAction.RENEWAL_REJECTED, ipAddress, userAgent);
		notificationService.create(
				"renewal_rejected", id, "Renouvellement rejeté",
				"Votre demande de renouvellement a été rejetée. Contactez le support pour plus d'informations.", false);
		return toUserResponse(user);
	}

	/** Super Admin only — force-regenerates a tenant's password and e-mails the new credentials. */
	public void resetPasswordByAdmin(String id, String actorId, String ipAddress, String userAgent) {
		UserDocument user = findUserOrThrow(id);
		String rawPassword = generateRandomPassword();
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		userRepository.save(user);
		List<RefreshTokenDocument> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());
		activeTokens.forEach(token -> token.setRevoked(true));
		refreshTokenRepository.saveAll(activeTokens);
		auditService.log(actorId, AuditAction.PASSWORD_RESET_COMPLETED, ipAddress, userAgent);
		mailService.sendPasswordChangedByAdminEmail(user.getEmail(), user.getFirstName(), rawPassword);
	}

	private UserDocument findUserOrThrow(String id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	public void requestPasswordReset(String email, String ipAddress, String userAgent) {
		String normalized = email.trim().toLowerCase();
		userRepository.findByEmail(normalized).ifPresent(user -> {
			String code = generateNumericCode();
			keyValueStore.set(RESET_CODE_PREFIX + normalized, code, RESET_CODE_TTL);
			mailService.sendPasswordResetCode(normalized, user.getFirstName(), code);
			auditService.log(user.getId(), AuditAction.PASSWORD_RESET_REQUESTED, ipAddress, userAgent);
		});
		// Always returns silently for unknown emails too, to avoid leaking which addresses are registered.
	}

	public String verifyResetCode(String email, String code) {
		String normalized = email.trim().toLowerCase();
		String storedCode = keyValueStore.get(RESET_CODE_PREFIX + normalized);
		if (storedCode == null || !storedCode.equals(code)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired code");
		}
		keyValueStore.delete(RESET_CODE_PREFIX + normalized);
		String resetToken = UUID.randomUUID().toString();
		keyValueStore.set(RESET_TOKEN_PREFIX + resetToken, normalized, RESET_TOKEN_TTL);
		return resetToken;
	}

	public void resetPassword(String resetToken, String newPassword, String ipAddress, String userAgent) {
		String email = keyValueStore.get(RESET_TOKEN_PREFIX + resetToken);
		if (email == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset token");
		}
		UserDocument user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset token"));
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		keyValueStore.delete(RESET_TOKEN_PREFIX + resetToken);
		// Revoke every existing session — a password reset should invalidate prior refresh tokens.
		List<RefreshTokenDocument> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());
		activeTokens.forEach(token -> token.setRevoked(true));
		refreshTokenRepository.saveAll(activeTokens);
		auditService.log(user.getId(), AuditAction.PASSWORD_RESET_COMPLETED, ipAddress, userAgent);
	}

	private String generateRandomPassword() {
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder(12);
		for (int i = 0; i < 12; i++) {
			sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
		}
		// Guarantee the character-class regex passes: at least one of each required class.
		sb.setCharAt(0, "abcdefghijkmnpqrstuvwxyz".charAt(random.nextInt(24)));
		sb.setCharAt(1, "ABCDEFGHJKLMNPQRSTUVWXYZ".charAt(random.nextInt(24)));
		sb.setCharAt(2, "23456789".charAt(random.nextInt(8)));
		sb.setCharAt(3, "!@#$%".charAt(random.nextInt(5)));
		return sb.toString();
	}

	private String generateNumericCode() {
		return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
	}

	private void saveRefreshToken(UserDocument user, String refreshToken, boolean rememberMe) {
		RefreshTokenDocument doc = RefreshTokenDocument.builder()
				.userId(user.getId())
				.tokenHash(hashRefreshToken(refreshToken))
				.revoked(false)
				.rememberMe(rememberMe)
				.expiresAt(jwtTokenProvider.getExpiration(refreshToken))
				.build();
		refreshTokenRepository.save(doc);
	}

	private RefreshTokenDocument findActiveRefreshToken(String userId, String refreshToken) {
		String expectedHash = hashRefreshToken(refreshToken);
		List<RefreshTokenDocument> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
		for (RefreshTokenDocument token : tokens) {
			if (hashesEqual(expectedHash, token.getTokenHash())) {
				return token;
			}
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
	}

	private void handleFailedLogin(UserDocument user) {
		int failed = user.getFailedAttempts() + 1;
		user.setFailedAttempts(failed);
		if (failed >= securityProperties.getMaxLoginAttempts()) {
			user.setLocked(true);
			user.setLockExpiresAt(Instant.now().plusSeconds(securityProperties.getLockDurationMinutes() * 60L));
		}
		userRepository.save(user);
	}

	private void resetFailedAttempts(UserDocument user) {
		user.setFailedAttempts(0);
		user.setLocked(false);
		user.setLockExpiresAt(null);
		userRepository.save(user);
	}

	private void unlockIfExpired(UserDocument user) {
		if (user.isLocked() && user.getLockExpiresAt() != null
				&& user.getLockExpiresAt().isBefore(Instant.now())) {
			user.setLocked(false);
			user.setFailedAttempts(0);
			user.setLockExpiresAt(null);
			userRepository.save(user);
		}
	}

	private UserResponse toUserResponse(UserDocument user) {
		return UserResponse.builder()
				.id(user.getId())
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.photoUrl(user.getPhotoUrl())
				.role(user.getRole())
				.status(user.getStatus())
				.planExpiresAt(user.getPlanExpiresAt())
				.planId(user.getPlanId())
				.renewalRequested(user.isRenewalRequested())
				.createdAt(user.getCreatedAt())
				.build();
	}

	private String hashRefreshToken(String refreshToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	private boolean hashesEqual(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				actual.getBytes(StandardCharsets.UTF_8));
	}
}
