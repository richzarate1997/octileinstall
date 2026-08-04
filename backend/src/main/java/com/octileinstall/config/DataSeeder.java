package com.octileinstall.config;

import com.octileinstall.entity.User;
import com.octileinstall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Creates the first admin account on startup.
 *
 * <p>BACKEND_PLAN §9 left open how Alberto's account gets created, since there is no public
 * registration endpoint. Doing it here rather than as a SQL {@code INSERT} of a literal hash
 * means the hash is produced by the same {@link PasswordEncoder} bean that login verifies
 * against, so the two can never drift apart.
 *
 * <p>This only ever <em>creates</em>. It will not touch an existing account, so restarting the
 * app can never silently reset a password that has been changed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed-enabled:true}")
    private boolean seedEnabled;

    @Value("${app.admin.seed-email}")
    private String seedEmail;

    @Value("${app.admin.seed-full-name}")
    private String seedFullName;

    @Value("${app.admin.seed-password:}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        String email = seedEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Admin account {} already exists; leaving it untouched", email);
            return;
        }

        boolean generated = seedPassword == null || seedPassword.isBlank();
        String password = generated ? randomPassword() : seedPassword;

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName(seedFullName);
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);

        if (generated) {
            // Printed once, never stored in plaintext. Setting ADMIN_SEED_PASSWORD avoids this.
            log.warn("""

                    ==========================================================
                     Created admin account: {}
                     Generated one-time password: {}
                     Copy it now - it is not recoverable and will not be shown
                     again. Set ADMIN_SEED_PASSWORD to choose it yourself.
                    ==========================================================
                    """, email, password);
        } else {
            log.info("Created admin account {} using the configured ADMIN_SEED_PASSWORD", email);
        }
    }

    private String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
