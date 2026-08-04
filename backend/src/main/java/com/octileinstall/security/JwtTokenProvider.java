package com.octileinstall.security;

import com.octileinstall.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies the HS256 JWTs used to authenticate admin dashboard sessions.
 *
 * <p>The token subject is the user id. Email and role travel as claims for convenience,
 * but they are <em>not</em> trusted for authorization: {@link JwtAuthFilter} re-loads the
 * user from the database on every request, so a role change or a deleted account takes
 * effect immediately rather than lingering until the token expires.
 */
@Component
public class JwtTokenProvider {

    /** HS256 requires a key of at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expirationMillis) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256, but was "
                            + keyBytes.length + ". Set a longer JWT_SECRET.");
        }
        if (expirationMillis <= 0) {
            throw new IllegalStateException("app.jwt.expiration must be positive, but was " + expirationMillis);
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = expirationMillis;
    }

    /** Mints a signed token for the given user. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies the signature and expiry, returning the payload.
     *
     * @throws JwtException if the token is malformed, tampered with, or expired
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns the user id carried by a valid token, or {@code null} if the token is
     * unusable for any reason. Callers must treat {@code null} as "not authenticated".
     */
    public Long extractUserId(String token) {
        try {
            String subject = parseClaims(token).getSubject();
            return subject == null ? null : Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Token lifetime in seconds, for the {@code expiresIn} field of the login response. */
    public long getExpiresInSeconds() {
        return expirationMillis / 1000L;
    }
}
