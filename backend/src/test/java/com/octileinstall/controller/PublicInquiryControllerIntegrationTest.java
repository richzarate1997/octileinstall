package com.octileinstall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octileinstall.repository.InquiryRepository;
import com.octileinstall.repository.InspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Each test uses a distinct fake remote address (see {@link #from(String)}) so that
 * RateLimiterService's per-IP counters - which persist for the life of the cached Spring
 * context, i.e. across every test method in this class - can't let one test's requests count
 * against another's limit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicInquiryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @BeforeEach
    void setUp() {
        // Inspections FK to inquiries, so they must go first - other integration test
        // classes share this same in-memory H2 instance and may leave inspection rows
        // behind depending on run order.
        inspectionRepository.deleteAll();
        inquiryRepository.deleteAll();
    }

    private static RequestPostProcessor from(String ip) {
        return request -> {
            ((MockHttpServletRequest) request).setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void submitCreatesAnInquiryWithNoTokenRequired() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Jane Public",
                "customerEmail", "jane@example.com",
                "customerPhone", "(714) 555-9876",
                "projectType", "Custom Shower",
                "city", "Anaheim",
                "message", "Interested in a walk-in shower."));

        mockMvc.perform(post("/api/public/inquiry")
                        .with(from("198.51.100.1"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.message", notNullValue()));

        assertThat(inquiryRepository.count()).isEqualTo(1);
        var saved = inquiryRepository.findAll().get(0);
        assertThat(saved.getCustomerName()).isEqualTo("Jane Public");
        assertThat(saved.getStatus().name()).isEqualTo("NEW");
        assertThat(saved.getCreatedByUser()).isNull();
    }

    @Test
    void submitWithBlankRequiredFieldsReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("customerName", "", "customerEmail", ""));

        mockMvc.perform(post("/api/public/inquiry")
                        .with(from("198.51.100.2"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerName", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.customerEmail", notNullValue()));

        assertThat(inquiryRepository.count()).isZero();
    }

    @Test
    void submitWithInvalidEmailReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Jane Public",
                "customerEmail", "not-an-email"));

        mockMvc.perform(post("/api/public/inquiry")
                        .with(from("198.51.100.3"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerEmail", notNullValue()));

        assertThat(inquiryRepository.count()).isZero();
    }

    @Test
    void submitWithHoneypotFilledReturnsSuccessButDropsSilently() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Bot Name",
                "customerEmail", "bot@example.com",
                "company", "Definitely A Real Company"));

        // Same 201 shape a genuine submission gets - a bot probing the endpoint can't tell
        // its payload was dropped rather than accepted.
        mockMvc.perform(post("/api/public/inquiry")
                        .with(from("198.51.100.4"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.received").value(true));

        assertThat(inquiryRepository.count()).isZero();
    }

    @Test
    void exceedingTheRateLimitReturns429() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Jane Public",
                "customerEmail", "jane@example.com"));

        // application-test.properties caps this at 3 requests per window.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/public/inquiry")
                            .with(from("198.51.100.5"))
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/public/inquiry")
                        .with(from("198.51.100.5"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));

        assertThat(inquiryRepository.count()).isEqualTo(3);
    }

    @Test
    void rateLimitIsTrackedPerIpNotGlobally() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Jane Public",
                "customerEmail", "jane@example.com"));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/public/inquiry")
                            .with(from("198.51.100.6"))
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        // A different IP has its own, untouched budget.
        mockMvc.perform(post("/api/public/inquiry")
                        .with(from("198.51.100.7"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
