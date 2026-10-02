package org.serviceproject.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.serviceproject.common.config.JwtProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtProvider}.
 */
class JwtProviderTest {

    // A 256-bit key (32+ characters) for HS256
    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, EXPIRATION_MS);
        jwtProvider = new JwtProvider(properties);
    }

    @Test
    void generateToken_producesNonNullToken() {
        String token = jwtProvider.generateToken(1L, 0);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractUserId_returnsCorrectId() {
        String token = jwtProvider.generateToken(42L, 0);
        Long userId = jwtProvider.extractUserId(token);
        assertEquals(42L, userId);
    }

    @Test
    void extractTokenVersion_returnsCorrectVersion() {
        String token = jwtProvider.generateToken(1L, 5);
        int version = jwtProvider.extractTokenVersion(token);
        assertEquals(5, version);
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtProvider.generateToken(1L, 0);
        assertTrue(jwtProvider.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtProvider.generateToken(1L, 0);
        String tampered = token + "x";
        assertFalse(jwtProvider.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_returnsFalseForGarbageString() {
        assertFalse(jwtProvider.isTokenValid("not.a.jwt"));
    }

    @Test
    void isTokenValid_returnsFalseForNull() {
        assertFalse(jwtProvider.isTokenValid(null));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        // Create provider with 0ms expiration
        JwtProperties expired = new JwtProperties(SECRET, 0);
        JwtProvider expiredProvider = new JwtProvider(expired);

        String token = expiredProvider.generateToken(1L, 0);
        assertFalse(expiredProvider.isTokenValid(token));
    }

    @Test
    void extractUserId_returnsNullForInvalidToken() {
        Long userId = jwtProvider.extractUserId("invalid");
        assertNull(userId);
    }

    @Test
    void extractTokenVersion_returnsNegativeForInvalidToken() {
        int version = jwtProvider.extractTokenVersion("invalid");
        assertEquals(-1, version);
    }

    @Test
    void differentUsers_getDifferentTokens() {
        String token1 = jwtProvider.generateToken(1L, 0);
        String token2 = jwtProvider.generateToken(2L, 0);
        assertNotEquals(token1, token2);
    }

    @Test
    void differentVersions_getDifferentTokens() {
        String token1 = jwtProvider.generateToken(1L, 0);
        String token2 = jwtProvider.generateToken(1L, 1);
        assertNotEquals(token1, token2);
    }

    @Test
    void tokenFromDifferentKey_isInvalid() {
        // Generate with a different key
        JwtProperties otherProps = new JwtProperties(
                "another-secret-key-that-is-also-at-least-256-bits-long-for-algorithm", EXPIRATION_MS);
        JwtProvider otherProvider = new JwtProvider(otherProps);

        String token = otherProvider.generateToken(1L, 0);
        assertFalse(jwtProvider.isTokenValid(token));
    }
}
