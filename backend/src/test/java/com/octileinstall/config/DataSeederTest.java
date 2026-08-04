package com.octileinstall.config;

import com.octileinstall.entity.User;
import com.octileinstall.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.admin.seed-enabled=true",
        "app.admin.seed-email=Seeded.Admin@OctileInstall.com",
        "app.admin.seed-full-name=Seeded Admin",
        "app.admin.seed-password=a-known-seed-password"
})
class DataSeederTest {

    private static final String CONFIGURED_EMAIL = "Seeded.Admin@OctileInstall.com";
    private static final String NORMALIZED_EMAIL = "seeded.admin@octileinstall.com";
    private static final String SEED_PASSWORD = "a-known-seed-password";

    @Autowired
    private DataSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    /**
     * The hash the seeder writes must verify against the plaintext with the same encoder login
     * uses. This is the check that the fabricated hash in the original schema.sql would have failed.
     */
    @Test
    void seedsAnAdminWhosePasswordActuallyVerifies() {
        seeder.run();

        User admin = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow();
        assertThat(admin.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(admin.getFullName()).isEqualTo("Seeded Admin");
        assertThat(passwordEncoder.matches(SEED_PASSWORD, admin.getPasswordHash())).isTrue();
        assertThat(admin.getPasswordHash()).isNotEqualTo(SEED_PASSWORD);
    }

    /** Email is normalized on write so it matches the lowercase lookup AuthService performs. */
    @Test
    void storesTheEmailLowercased() {
        seeder.run();

        assertThat(userRepository.findByEmail(NORMALIZED_EMAIL)).isPresent();
        assertThat(userRepository.findByEmail(CONFIGURED_EMAIL)).isEmpty();
    }

    /**
     * Restarting the app must never reset a password that has since been changed, so a second
     * run is a no-op rather than an overwrite.
     */
    @Test
    void doesNotOverwriteAnExistingAccount() {
        seeder.run();
        String originalHash = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow().getPasswordHash();

        User existing = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow();
        existing.setPasswordHash(passwordEncoder.encode("a-password-the-user-changed-it-to"));
        userRepository.save(existing);

        seeder.run();

        User after = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow();
        assertThat(after.getPasswordHash()).isNotEqualTo(originalHash);
        assertThat(passwordEncoder.matches("a-password-the-user-changed-it-to", after.getPasswordHash())).isTrue();
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
