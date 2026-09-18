package com.harmoniedev.api.auth.service;

import com.harmoniedev.api.auth.domain.enums.AccountStatus;
import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import com.harmoniedev.api.auth.repository.UserRepository;
import com.harmoniedev.api.plan.domain.model.PlanDocument;
import com.harmoniedev.api.plan.repository.PlanRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Desktop build only: seeds one USER account per subscription plan (one per row of
 * {@link com.harmoniedev.api.plan.service.PlanSeeder}) so a fresh install already has something
 * to click around in per plan tier, instead of an empty tenant list. No-ops once any of these
 * demo accounts already exists. Runs after {@link com.harmoniedev.api.plan.service.PlanSeeder}
 * (see its {@code @Order}) so the plans it looks up by name already exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "desktop")
public class DemoAccountSeeder {
	private static final String PASSWORD_SUFFIX = "Desktop#2026";

	private record DemoAccount(String planName, String emailLocalPart, String firstName, String lastName, int expiresInDays) {
	}

	private static final List<DemoAccount> ACCOUNTS = List.of(
			new DemoAccount("Essai gratuit", "essai", "Amira", "Trial", 15),
			new DemoAccount("Starter", "starter", "Karim", "Starter", 30),
			new DemoAccount("Pro", "pro", "Sami", "Pro", 30),
			new DemoAccount("Entreprise", "entreprise", "Nadia", "Entreprise", 30));

	private final UserRepository userRepository;
	private final PlanRepository planRepository;
	private final PasswordEncoder passwordEncoder;

	@EventListener(ApplicationReadyEvent.class)
	@Order(2)
	public void seedDemoAccounts() {
		Map<String, PlanDocument> plansByName = planRepository.findAll().stream()
				.collect(java.util.stream.Collectors.toMap(PlanDocument::getNom, p -> p, (a, b) -> a));

		for (DemoAccount account : ACCOUNTS) {
			String email = account.emailLocalPart() + "@harmonie.local";
			if (userRepository.existsByEmail(email)) {
				continue;
			}
			PlanDocument plan = plansByName.get(account.planName());
			if (plan == null) {
				log.warn("Skipping demo account {} — plan '{}' not found (was PlanSeeder run first?)", email, account.planName());
				continue;
			}
			String password = capitalize(account.emailLocalPart()) + PASSWORD_SUFFIX;
			UserDocument user = UserDocument.builder()
					.email(email)
					.passwordHash(passwordEncoder.encode(password))
					.role(Role.USER)
					.firstName(account.firstName())
					.lastName(account.lastName())
					.status(AccountStatus.ACTIVE)
					.planId(plan.getId())
					.planExpiresAt(Instant.now().plus(account.expiresInDays(), ChronoUnit.DAYS))
					.build();
			userRepository.save(user);
			log.info("Seeded demo account [{}] {} / {}", account.planName(), email, password);
		}
	}

	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
