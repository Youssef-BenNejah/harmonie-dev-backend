package com.harmoniedev.api.auth.service;

import com.harmoniedev.api.auth.domain.enums.AccountStatus;
import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import com.harmoniedev.api.auth.repository.UserRepository;
import com.harmoniedev.api.config.SeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates a single ADMIN account on first startup so there is always a way into the Super Admin
 * panel in a fresh environment. No-ops once any ADMIN already exists, and no-ops entirely if
 * SEED_ADMIN_EMAIL/SEED_ADMIN_PASSWORD are not set.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountSeeder {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SeedProperties seedProperties;
	private final com.harmoniedev.api.config.AppProperties appProperties;

	@EventListener(ApplicationReadyEvent.class)
	public void seedAdmin() {
		String email = seedProperties.getAdminEmail();
		String password = seedProperties.getAdminPassword();
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			return;
		}
		if (!appProperties.isDev() && password.length() < 12) {
			log.warn("Skipping admin seed: SEED_ADMIN_PASSWORD must be at least 12 characters outside dev");
			return;
		}
		if (userRepository.existsByRole(Role.ADMIN)) {
			return;
		}
		String normalized = email.trim().toLowerCase();
		if (userRepository.existsByEmail(normalized)) {
			return;
		}
		UserDocument admin = UserDocument.builder()
				.email(normalized)
				.passwordHash(passwordEncoder.encode(password))
				.role(Role.ADMIN)
				.firstName("Super")
				.lastName("Admin")
				.status(AccountStatus.ACTIVE)
				.build();
		userRepository.save(admin);
		log.info("Seeded ADMIN account: {}", normalized);
	}
}
