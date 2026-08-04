package com.octileinstall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octileinstall.entity.Inquiry;
import com.octileinstall.entity.User;
import com.octileinstall.repository.InquiryRepository;
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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InquiryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User admin;
    private String token;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        userRepository.deleteAll();

        admin = new User();
        admin.setEmail("staff@octileinstall.com");
        admin.setPasswordHash(passwordEncoder.encode("irrelevant"));
        admin.setFullName("Staff");
        admin.setRole(User.Role.ADMIN);
        admin = userRepository.save(admin);

        token = tokenProvider.generateToken(admin);
    }

    private Inquiry saveInquiry(String name, Inquiry.Status status) {
        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerName(name);
        inquiry.setCustomerEmail(name.toLowerCase() + "@example.com");
        inquiry.setStatus(status);
        inquiry.setCreatedByUser(admin);
        return inquiryRepository.save(inquiry);
    }

    // --- auth ---

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/inquiry"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/inquiry")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- create ---

    @Test
    void createInquirySetsDefaultsAndCreatedBy() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "John Doe",
                "customerEmail", "john@example.com",
                "customerPhone", "(714) 555-1234",
                "projectType", "Custom Shower",
                "city", "Irvine",
                "message", "Looking for a custom shower design"));

        mockMvc.perform(post("/api/inquiry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.projectType").value("Custom Shower"))
                .andExpect(jsonPath("$.createdBy").value(admin.getId()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void createWithBlankRequiredFieldsReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("customerName", "", "customerEmail", ""));

        mockMvc.perform(post("/api/inquiry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithInvalidEmailReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Jane Doe", "customerEmail", "not-an-email"));

        mockMvc.perform(post("/api/inquiry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- get / list ---

    @Test
    void getByIdReturnsTheInquiry() throws Exception {
        Inquiry saved = saveInquiry("Alice", Inquiry.Status.NEW);

        mockMvc.perform(get("/api/inquiry/" + saved.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Alice"));
    }

    @Test
    void getByIdMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/inquiry/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsAllSortedByCreatedAtDesc() throws Exception {
        saveInquiry("First", Inquiry.Status.NEW);
        saveInquiry("Second", Inquiry.Status.NEW);
        saveInquiry("Third", Inquiry.Status.NEW);

        mockMvc.perform(get("/api/inquiry").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data[0].customerName").value("Third"))
                .andExpect(jsonPath("$.data[2].customerName").value("First"));
    }

    @Test
    void listFiltersByStatus() throws Exception {
        saveInquiry("NewOne", Inquiry.Status.NEW);
        saveInquiry("ContactedOne", Inquiry.Status.CONTACTED);

        mockMvc.perform(get("/api/inquiry?status=CONTACTED").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].customerName").value("ContactedOne"));
    }

    @Test
    void listPaginatesWithLimitAndOffset() throws Exception {
        saveInquiry("A", Inquiry.Status.NEW);
        saveInquiry("B", Inquiry.Status.NEW);
        saveInquiry("C", Inquiry.Status.NEW);

        mockMvc.perform(get("/api/inquiry?limit=2&offset=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].customerName").value("C"));

        mockMvc.perform(get("/api/inquiry?limit=2&offset=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].customerName").value("A"));
    }

    // --- update (partial-update semantics) ---

    @Test
    void updateAppliesOnlyStatusWhenNotesOmitted() throws Exception {
        Inquiry saved = saveInquiry("Bob", Inquiry.Status.NEW);
        saved.setNotes("original note");
        inquiryRepository.save(saved);

        String body = objectMapper.writeValueAsString(Map.of("status", "CONTACTED"));

        mockMvc.perform(put("/api/inquiry/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"))
                .andExpect(jsonPath("$.notes").value("original note"));
    }

    @Test
    void updateAppliesOnlyNotesWhenStatusOmitted() throws Exception {
        Inquiry saved = saveInquiry("Carol", Inquiry.Status.QUOTED);

        String body = objectMapper.writeValueAsString(Map.of("notes", "called, left voicemail"));

        mockMvc.perform(put("/api/inquiry/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.notes").value("called, left voicemail"));
    }

    @Test
    void updateMissingReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "CONTACTED"));

        mockMvc.perform(put("/api/inquiry/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- delete ---

    @Test
    void deleteRemovesTheInquiry() throws Exception {
        Inquiry saved = saveInquiry("ToDelete", Inquiry.Status.NEW);

        mockMvc.perform(delete("/api/inquiry/" + saved.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/inquiry/" + saved.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        mockMvc.perform(delete("/api/inquiry/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
