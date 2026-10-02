package org.serviceproject.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.common.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT token generation and validation.
 * <p>
 * Token payload:
 * <ul>
 *   <li>{@code sub} — user account ID</li>
 *   <li>{@code tv} — token version (for invalidation on password/phone change)</li>
 *   <li>{@code iat} — issued at</li>
 *   <li>{@code exp} — expiration</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtProvider {

    private static final String TOKEN_VERSION_CLAIM = "tv";

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a signed JWT for the given user.
     */
    public String generateToken(Long userId, int tokenVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Extract the user ID from a valid token.
     *
     * @return the user ID, or null if the token is invalid
     */
    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? Long.parseLong(claims.getSubject()) : null;
    }

    /**
     * Extract the token version from a valid token.
     *
     * @return the token version, or -1 if the token is invalid
     */
    public int extractTokenVersion(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get(TOKEN_VERSION_CLAIM, Integer.class) : -1;
    }

    /**
     * Check if a token is structurally valid and not expired.
     */
    public boolean isTokenValid(String token) {
        return extractClaims(token) != null;
    }

    // ── Internal ─────────────────────────────────────────────────────

    private Claims extractClaims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired: {}", ex.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return null;
        }
    }
}
