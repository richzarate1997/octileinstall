package com.octileinstall.security;

import com.octileinstall.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-0123456789abcdef";
    private static final String OTHER_SECRET = "a-completely-different-secret-key-9876543210zyxw";
    private static final long WEEK_MILLIS = 604_800_000L;

    private static User user() {
        User user = new User();
        user.setId(42L);
        user.setEmail("alberto@octileinstall.com");
        user.setFullName("Alberto");
        user.setRole(User.Role.ADMIN);
        return user;
    }

    @Test
    void generatedTokenCarriesUserIdEmailAndRole() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, WEEK_MILLIS);

        Claims claims = provider.parseClaims(provider.generateToken(user()));

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email")).isEqualTo("alberto@octileinstall.com");
        assertThat(claims.get("role")).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void extractUserIdReturnsTheSubject() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, WEEK_MILLIS);

        assertThat(provider.extractUserId(provider.generateToken(user()))).isEqualTo(42L);
    }

    /**
     * The single most important assertion in this phase: a token signed by someone else must
     * not verify. If signature checking were disabled, anyone could forge an admin token.
     */
    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        String forged = new JwtTokenProvider(OTHER_SECRET, WEEK_MILLIS).generateToken(user());
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, WEEK_MILLIS);

        assertThatThrownBy(() -> provider.parseClaims(forged)).isInstanceOf(JwtException.class);
        assertThat(provider.extractUserId(forged)).isNull();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, WEEK_MILLIS);
        String token = provider.generateToken(user());

        // Flip a character in the payload segment; the signature no longer covers it.
        String[] parts = token.split("\\.");
        parts[1] = parts[1].substring(0, parts[1].length() - 2)
                + (parts[1].endsWith("A") ? "B" : "A");
        String tampered = String.join(".", parts);

        assertThat(provider.extractUserId(tampered)).isNull();
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L);
        String token = shortLived.generateToken(user());

        Thread.sleep(50);

        assertThat(new JwtTokenProvider(SECRET, WEEK_MILLIS).extractUserId(token)).isNull();
    }

    @Test
    void garbageInputIsRejectedWithoutThrowing() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, WEEK_MILLIS);

        assertThat(provider.extractUserId("not-a-jwt")).isNull();
        assertThat(provider.extractUserId("")).isNull();
    }

    /** A short secret cannot produce a secure HS256 signature, so startup must fail loudly. */
    @Test
    void refusesToStartWithATooShortSecret() {
        assertThatThrownBy(() -> new JwtTokenProvider("too-short", WEEK_MILLIS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void expiresInIsReportedInSeconds() {
        assertThat(new JwtTokenProvider(SECRET, WEEK_MILLIS).getExpiresInSeconds()).isEqualTo(604_800L);
    }
}
