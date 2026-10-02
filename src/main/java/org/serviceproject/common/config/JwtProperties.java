package org.serviceproject.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties bound from the {@code jwt.*} namespace.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        /** Secret key for signing JWTs (must be at least 256 bits for HS256). */
        String secret,

        /** Token expiration time in milliseconds (default: 86400000 = 24 hours). */
        long expiration
) {}
