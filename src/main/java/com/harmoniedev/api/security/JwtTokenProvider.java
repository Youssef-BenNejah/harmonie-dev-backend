package com.harmoniedev.api.security;

import com.harmoniedev.api.config.JwtProperties;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
	private static final Pattern PEM_PATTERN = Pattern.compile(
			"-----BEGIN ([A-Z ]+)-----(.*?)-----END \\1-----",
			Pattern.DOTALL);
	private static final byte[] RSA_OID = new byte[] {
			0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01
	};

	private final JwtProperties properties;
	private PrivateKey privateKey;
	private PublicKey publicKey;

	public JwtTokenProvider(JwtProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void initKeys() {
		if (properties.getPrivateKey() == null || properties.getPrivateKey().isBlank()) {
			throw new IllegalStateException("JWT_PRIVATE_KEY is not set");
		}
		if (properties.getPublicKey() == null || properties.getPublicKey().isBlank()) {
			throw new IllegalStateException("JWT_PUBLIC_KEY is not set");
		}
		try {
			DecodedKey privateMaterial = decodeKeyMaterial(properties.getPrivateKey());
			DecodedKey publicMaterial = decodeKeyMaterial(properties.getPublicKey());
			byte[] privateBytes = privateMaterial.type == KeyType.PEM_RSA_PRIVATE
					? pkcs1ToPkcs8(privateMaterial.der)
					: privateMaterial.der;
			byte[] publicBytes = publicMaterial.type == KeyType.PEM_RSA_PUBLIC
					? rsaPublicKeyToSpki(publicMaterial.der)
					: publicMaterial.der;
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
			publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicBytes));
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to load JWT keys", ex);
		}
	}

	public String generateAccessToken(UserDocument user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getId())
				.id(UUID.randomUUID().toString())
				.issuer("harmonie-dev-api")
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(properties.getAccessTokenExpiry())))
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.claim("type", "access")
				.signWith(privateKey, Jwts.SIG.RS256)
				.compact();
	}

	public String generateRefreshToken(UserDocument user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getId())
				.id(UUID.randomUUID().toString())
				.issuer("harmonie-dev-api")
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(properties.getRefreshTokenExpiry())))
				.claim("email", user.getEmail())
				.claim("type", "refresh")
				.signWith(privateKey, Jwts.SIG.RS256)
				.compact();
	}

	public boolean isValid(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	public Claims parseClaims(String token) {
		Jws<Claims> jws = Jwts.parser()
				.verifyWith(publicKey)
				.build()
				.parseSignedClaims(token);
		return jws.getPayload();
	}

	public String getSubject(String token) {
		return parseClaims(token).getSubject();
	}

	public Instant getExpiration(String token) {
		return parseClaims(token).getExpiration().toInstant();
	}

	public String getTokenType(String token) {
		Object type = parseClaims(token).get("type");
		return type == null ? null : type.toString();
	}

	private DecodedKey decodeKeyMaterial(String value) {
		String trimmed = value.trim();
		DecodedKey directPem = decodePemIfPresent(trimmed);
		if (directPem != null) {
			return directPem;
		}
		byte[] decoded = Base64.getDecoder().decode(trimmed);
		String decodedText = new String(decoded, StandardCharsets.US_ASCII).trim();
		DecodedKey decodedPem = decodePemIfPresent(decodedText);
		if (decodedPem != null) {
			return decodedPem;
		}
		return new DecodedKey(decoded, KeyType.DER);
	}

	private DecodedKey decodePemIfPresent(String text) {
		Matcher matcher = PEM_PATTERN.matcher(text);
		if (!matcher.find()) {
			return null;
		}
		String type = matcher.group(1).trim();
		String base64 = matcher.group(2).replaceAll("\\s", "");
		byte[] der = Base64.getDecoder().decode(base64);
		return new DecodedKey(der, KeyType.fromPemType(type));
	}

	private byte[] pkcs1ToPkcs8(byte[] pkcs1) {
		byte[] algId = derSequence(derOid(RSA_OID), derNull());
		return derSequence(derIntegerZero(), algId, derOctetString(pkcs1));
	}

	private byte[] rsaPublicKeyToSpki(byte[] pkcs1) {
		byte[] algId = derSequence(derOid(RSA_OID), derNull());
		return derSequence(algId, derBitString(pkcs1));
	}

	private byte[] derSequence(byte[]... parts) {
		int totalLength = 0;
		for (byte[] part : parts) {
			totalLength += part.length;
		}
		byte[] value = new byte[totalLength];
		int offset = 0;
		for (byte[] part : parts) {
			System.arraycopy(part, 0, value, offset, part.length);
			offset += part.length;
		}
		return derWrap(0x30, value);
	}

	private byte[] derIntegerZero() {
		return derWrap(0x02, new byte[] { 0x00 });
	}

	private byte[] derOctetString(byte[] value) {
		return derWrap(0x04, value);
	}

	private byte[] derBitString(byte[] value) {
		byte[] prefixed = new byte[value.length + 1];
		prefixed[0] = 0x00;
		System.arraycopy(value, 0, prefixed, 1, value.length);
		return derWrap(0x03, prefixed);
	}

	private byte[] derNull() {
		return derWrap(0x05, new byte[0]);
	}

	private byte[] derOid(byte[] oid) {
		return derWrap(0x06, oid);
	}

	private byte[] derWrap(int tag, byte[] value) {
		byte[] length = derEncodeLength(value.length);
		byte[] out = new byte[1 + length.length + value.length];
		out[0] = (byte) tag;
		System.arraycopy(length, 0, out, 1, length.length);
		System.arraycopy(value, 0, out, 1 + length.length, value.length);
		return out;
	}

	private byte[] derEncodeLength(int length) {
		if (length < 128) {
			return new byte[] { (byte) length };
		}
		int temp = length;
		int numBytes = 0;
		while (temp > 0) {
			numBytes++;
			temp >>= 8;
		}
		byte[] out = new byte[1 + numBytes];
		out[0] = (byte) (0x80 | numBytes);
		for (int i = numBytes; i > 0; i--) {
			out[i] = (byte) (length & 0xFF);
			length >>= 8;
		}
		return out;
	}

	private enum KeyType {
		PEM_RSA_PRIVATE,
		PEM_PRIVATE,
		PEM_RSA_PUBLIC,
		PEM_PUBLIC,
		DER;

		static KeyType fromPemType(String type) {
			if ("RSA PRIVATE KEY".equals(type)) {
				return PEM_RSA_PRIVATE;
			}
			if ("PRIVATE KEY".equals(type)) {
				return PEM_PRIVATE;
			}
			if ("RSA PUBLIC KEY".equals(type)) {
				return PEM_RSA_PUBLIC;
			}
			if ("PUBLIC KEY".equals(type)) {
				return PEM_PUBLIC;
			}
			return DER;
		}
	}

	private static final class DecodedKey {
		private final byte[] der;
		private final KeyType type;

		private DecodedKey(byte[] der, KeyType type) {
			this.der = der;
			this.type = type;
		}
	}
}
