package com.octileinstall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octileinstall.entity.User;
import com.octileinstall.repository.UserRepository;
import com.octileinstall.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of BACKEND_PLAN Phase 3 acceptance criteria, driven through the real
 * filter chain so a filter that never runs shows up as a failure.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String EMAIL = "alberto@octileinstall.com";
    private static final String PASSWORD = "correct-horse-battery-staple";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private Long userId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("Alberto");
        user.setRole(User.Role.ADMIN);
        userId = userRepository.save(user).getId();
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
    }

    @Test
    void loginWithValidCredentialsReturnsAToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.expiresIn").value(604800))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginIsCaseInsensitiveOnEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody("Alberto@OctileInstall.com", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody("nobody@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody("", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    /** A token whose signature does not match ours must not authenticate anyone. */
    @Test
    void protectedEndpointWithForeignlySignedTokenReturns401() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        String forged = new JwtTokenProvider(
                "an-entirely-different-signing-secret-abcdefghij", 604_800_000L)
                .generateToken(user);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    /** Phase 3 acceptance: "Principal available in controller". */
    @Test
    void protectedEndpointWithValidTokenResolvesThePrincipal() throws Exception {
        String token = tokenProvider.generateToken(userRepository.findByEmail(EMAIL).orElseThrow());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    /**
     * The filter re-reads the user on every request, so deleting the account revokes access
     * immediately rather than leaving a valid-looking token working until it expires.
     */
    @Test
    void tokenForADeletedUserNoLongerAuthenticates() throws Exception {
        String token = tokenProvider.generateToken(userRepository.findByEmail(EMAIL).orElseThrow());
        userRepository.deleteAll();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedAuthorizationHeaderIsIgnoredRatherThanCrashing() throws Exception {
        String token = tokenProvider.generateToken(userRepository.findByEmail(EMAIL).orElseThrow());

        // Missing the "Bearer " scheme entirely.
        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }
}
