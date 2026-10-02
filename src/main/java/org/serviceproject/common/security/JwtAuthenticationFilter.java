package org.serviceproject.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Extracts and validates the JWT from the {@code Authorization: Bearer ...}
 * header on every request. If valid, loads the user from the database,
 * verifies token version, and sets the {@link UserPrincipal} into the
 * Spring Security context.
 * <p>
 * This filter is NOT a Spring @Component; it is created as a bean
 * in {@link SecurityConfig} to avoid double filter registration.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserAccountRepository userAccountRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromToken(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromToken(String token, HttpServletRequest request) {
        if (!jwtProvider.isTokenValid(token)) {
            return;
        }

        Long userId = jwtProvider.extractUserId(token);
        int tokenVersion = jwtProvider.extractTokenVersion(token);

        if (userId == null) {
            return;
        }

        Optional<UserAccount> accountOpt = userAccountRepository.findActiveByIdWithRoles(userId);
        if (accountOpt.isEmpty()) {
            log.debug("JWT user {} not found or inactive", userId);
            return;
        }

        UserAccount account = accountOpt.get();

        // Verify token version matches (invalidated after password/phone change)
        if (account.getTokenVersion() != tokenVersion) {
            log.debug("JWT token version mismatch for user {}: expected={}, got={}",
                    userId, account.getTokenVersion(), tokenVersion);
            return;
        }

        UserPrincipal principal = UserPrincipal.from(account);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
