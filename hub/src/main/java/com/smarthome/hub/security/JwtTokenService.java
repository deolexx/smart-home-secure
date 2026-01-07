package com.smarthome.hub.security;

import com.smarthome.hub.domain.Role;
import com.smarthome.hub.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenService {

	private final SecretKey key;
	private final String issuer;
	private final long expirationSeconds;

	public JwtTokenService(
			@Value("${security.jwt.secret-base64}") String base64Secret,
			@Value("${security.jwt.issuer}") String issuer,
			@Value("${security.jwt.expiration-seconds}") long expirationSeconds
	) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
		this.issuer = issuer;
		this.expirationSeconds = expirationSeconds;
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		Set<String> roles = user.getRoles().stream().map(Role::getName).map(Enum::name).collect(Collectors.toSet());
		return Jwts.builder()
				.subject(user.getUsername())
				.issuer(issuer)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(expirationSeconds)))
				.claim("roles", roles)
				.signWith(key)
				.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.requireIssuer(issuer)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}

