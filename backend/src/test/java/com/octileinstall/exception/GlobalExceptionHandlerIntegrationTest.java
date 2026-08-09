package com.octileinstall.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octileinstall.entity.User;
import com.octileinstall.repository.InquiryRepository;
import com.octileinstall.repository.UserRepository;
import com.octileinstall.security.JwtTokenProvider;
import com.octileinstall.service.InquiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 6: proves the centralized {@link GlobalExceptionHandler} - not just that each error
 * case returns the right status code (Phases 3-5 already covered that through each
 * controller's now-removed local handlers), but that every case shares one response shape,
 * and specifically that the 500 path never leaks internal exception detail to the client.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockBean
    private InquiryService inquiryService;

    private String token;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setEmail("staff@octileinstall.com");
        admin.setPasswordHash(passwordEncoder.encode("irrelevant"));
        admin.setFullName("Staff");
        admin.setRole(User.Role.ADMIN);
        admin = userRepository.save(admin);

        token = tokenProvider.generateToken(admin);
    }

    @Test
    void beanValidationFailureReturnsFieldErrorsMap() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("customerName", "", "customerEmail", ""));

        mockMvc.perform(post("/api/inquiry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.customerName").exists())
                .andExpect(jsonPath("$.fieldErrors.customerEmail").exists());
    }

    @Test
    void malformedJsonBodyReturns400WithConsistentShape() throws Exception {
        mockMvc.perform(post("/api/inquiry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void unrecognizedEnumValueInBodyReturns400() throws Exception {
        // "status" only accepts NEW/CONTACTED/QUOTED/SCHEDULED/COMPLETED/ARCHIVED - Jackson's
        // enum deserialization fails on anything else with the same HttpMessageNotReadableException
        // as literally-malformed JSON, so it should get the same handling.
        String body = "{\"status\": \"NOT_A_REAL_STATUS\"}";

        mockMvc.perform(put("/api/inquiry/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidQueryParamTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/inquiry?limit=not-a-number")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("limit")));
    }

    @Test
    void invalidEnumQueryParamReturns400() throws Exception {
        mockMvc.perform(get("/api/inquiry?status=NOT_A_REAL_STATUS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void badLoginCredentialsReturn401WithConsistentShape() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", "x"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    /**
     * The property that matters: an unexpected exception must become a generic 500, and the
     * client must never see the real exception message - only the log does. Proven by mocking
     * a service to throw something with a deliberately identifying message, then asserting that
     * exact string is absent from the response.
     */
    @Test
    void unexpectedExceptionReturns500WithoutLeakingInternalDetail() throws Exception {
        when(inquiryService.getById(anyLong()))
                .thenThrow(new RuntimeException("db password is hunter2, connection pool exhausted at row 4471"));

        mockMvc.perform(get("/api/inquiry/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("hunter2"))));
    }
}
