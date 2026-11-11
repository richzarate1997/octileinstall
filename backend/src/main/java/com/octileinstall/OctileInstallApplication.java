package com.octileinstall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UserDetailsServiceAutoConfiguration is excluded because Spring Boot would otherwise create a
 * default in-memory "user" account with a randomly generated password printed to the logs. This
 * API authenticates solely via JWTs issued by AuthService, so that account is unreachable dead
 * weight - and a credential nobody is tracking is worth deleting rather than explaining.
 *
 * <p>{@code @EnableScheduling} powers RateLimiterService's periodic cleanup of stale rate-limit
 * windows - the only scheduled task in the app so far.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class OctileInstallApplication {

    public static void main(String[] args) {
        SpringApplication.run(OctileInstallApplication.class, args);
    }
}
