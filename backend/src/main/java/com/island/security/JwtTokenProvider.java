package com.island.security;

import com.island.config.IslandProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final long expirationMs;

	public JwtTokenProvider(IslandProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
		this.expirationMs = properties.getJwt().getExpirationMs();
	}

	public String createToken(Long userId, String email) {
		Date now = new Date();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("email", email)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	public Long getUserId(String token) {
		return Long.parseLong(parseClaims(token).getSubject());
	}

	public boolean validate(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
