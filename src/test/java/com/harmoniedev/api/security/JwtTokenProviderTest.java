package com.harmoniedev.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import com.harmoniedev.api.config.JwtProperties;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
	private JwtTokenProvider provider;
	private final UserDocument user = UserDocument.builder().id("u1").email("a@b.c").role(Role.USER).build();

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(2048);
		KeyPair pair = gen.generateKeyPair();
		JwtProperties props = new JwtProperties();
		props.setPrivateKey(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
		props.setPublicKey(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
		props.setAccessTokenExpiry(60_000);
		props.setRefreshTokenExpiry(600_000);
		provider = new JwtTokenProvider(props);
		provider.initKeys();
	}

	@Test
	void refreshTokensIssuedInTheSameSecondAreStillDistinct() {
		// The refresh_tokens collection has a unique index on the token hash: identical tokens (same
		// claims, same second) made the second /auth/refresh fail with a duplicate-key 500.
		assertThat(provider.generateRefreshToken(user)).isNotEqualTo(provider.generateRefreshToken(user));
		assertThat(provider.generateAccessToken(user)).isNotEqualTo(provider.generateAccessToken(user));
	}

	@Test
	void generatedTokensAreValid() {
		assertThat(provider.isValid(provider.generateAccessToken(user))).isTrue();
		assertThat(provider.getSubject(provider.generateRefreshToken(user))).isEqualTo("u1");
	}
}
