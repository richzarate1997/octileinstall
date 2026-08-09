package com.octileinstall.service;

import com.octileinstall.dto.LoginRequest;
import com.octileinstall.dto.LoginResponse;
import com.octileinstall.entity.User;
import com.octileinstall.exception.UnauthorizedException;
import com.octileinstall.repository.UserRepository;
import com.octileinstall.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * A real BCrypt hash of a random throwaway string, computed once at startup.
     *
     * <p>When the email does not exist we still run a full BCrypt comparison against this.
     * Without it, "unknown email" would return in microseconds while "known email, wrong
     * password" would take ~100ms, letting an attacker enumerate valid accounts by timing.
     * It is generated rather than hard-coded so it is guaranteed to be a well-formed hash —
     * a malformed constant would short-circuit and reintroduce the timing gap.
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;

        byte[] filler = new byte[32];
        new SecureRandom().nextBytes(filler);
        this.dummyHash = passwordEncoder.encode(Base64.getEncoder().encodeToString(filler));
    }

    /**
     * Verifies credentials and issues a token.
     *
     * @throws UnauthorizedException if the email is unknown or the password does not match.
     *                                The message is deliberately identical in both cases so the
     *                                response never reveals whether an account exists.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        Optional<User> found = userRepository.findByEmail(email);

        // Note the argument order: (raw password, stored hash). Reversing these makes
        // matches() always return false, which looks like "wrong password" forever.
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                found.map(User::getPasswordHash).orElse(dummyHash));

        if (found.isEmpty() || !passwordMatches) {
            log.info("Failed login attempt for email {}", email);
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = found.get();
        String token = tokenProvider.generateToken(user);
        log.info("User {} (id {}) logged in", user.getEmail(), user.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(tokenProvider.getExpiresInSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
