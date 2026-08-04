package com.octileinstall.security;

import com.octileinstall.entity.User;
import com.octileinstall.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads {@code Authorization: Bearer <token>}, verifies it, and populates the SecurityContext.
 *
 * <p>This filter never rejects a request itself. If the token is missing, malformed, expired,
 * or names a user who no longer exists, it simply leaves the context unauthenticated and lets
 * the filter chain proceed — {@code SecurityConfig}'s authorization rules then produce the 401
 * or 403. Keeping rejection in one place is what stops "permitAll" routes from accidentally
 * requiring a valid token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Never overwrite an authentication that an earlier filter already established.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromToken(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromToken(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return;
        }

        // A null id means the signature failed, the token expired, or the subject was junk.
        Long userId = tokenProvider.extractUserId(token);
        if (userId == null) {
            log.debug("Rejected a bearer token that failed verification");
            return;
        }

        // Re-read the user rather than trusting the role claim, so that a demoted or deleted
        // account loses access immediately instead of at token expiry.
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.debug("Bearer token referenced user id {}, which no longer exists", userId);
            return;
        }

        UserPrincipal principal = new UserPrincipal(user.get());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
