package com.octileinstall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octileinstall.entity.Inquiry;
import com.octileinstall.entity.Inspection;
import com.octileinstall.entity.User;
import com.octileinstall.repository.InquiryRepository;
import com.octileinstall.repository.InspectionRepository;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InspectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String token;
    private Inquiry inquiry;

    @BeforeEach
    void setUp() {
        inspectionRepository.deleteAll();
        inquiryRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setEmail("staff@octileinstall.com");
        admin.setPasswordHash(passwordEncoder.encode("irrelevant"));
        admin.setFullName("Staff");
        admin.setRole(User.Role.ADMIN);
        admin = userRepository.save(admin);
        token = tokenProvider.generateToken(admin);

        inquiry = new Inquiry();
        inquiry.setCustomerName("Jane Homeowner");
        inquiry.setCustomerEmail("jane@example.com");
        inquiry.setCreatedByUser(admin);
        inquiry = inquiryRepository.save(inquiry);
    }

    private Inspection saveInspection(LocalDateTime scheduledDate, Inspection.Status status) {
        Inspection inspection = new Inspection();
        inspection.setInquiry(inquiry);
        inspection.setScheduledDate(scheduledDate);
        inspection.setStatus(status);
        return inspectionRepository.save(inspection);
    }

    // --- auth ---

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/inspection"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/inspection")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- create ---

    @Test
    void createWithValidInquiryIdSucceeds() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "inquiryId", inquiry.getId(),
                "scheduledDate", "2027-01-10T14:00:00",
                "location", "123 Main St, Irvine, CA 92614"));

        mockMvc.perform(post("/api/inspection")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.inquiryId").value(inquiry.getId()))
                .andExpect(jsonPath("$.scheduledDate").value("2027-01-10T14:00:00"))
                .andExpect(jsonPath("$.location").value("123 Main St, Irvine, CA 92614"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void createForNonExistentInquiryReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "inquiryId", 999999, "scheduledDate", "2027-01-10T14:00:00"));

        mockMvc.perform(post("/api/inspection")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithPastDateReturns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "inquiryId", inquiry.getId(), "scheduledDate", "2020-01-10T14:00:00"));

        mockMvc.perform(post("/api/inspection")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithMissingRequiredFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/inspection")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- get / list ---

    @Test
    void getByIdReturnsTheInspection() throws Exception {
        Inspection saved = saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.SCHEDULED);

        mockMvc.perform(get("/api/inspection/" + saved.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiryId").value(inquiry.getId()));
    }

    @Test
    void getByIdMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/inspection/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsAllSortedByScheduledDateAsc() throws Exception {
        saveInspection(LocalDateTime.now().plusDays(10), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.now().plusDays(3), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.now().plusDays(20), Inspection.Status.SCHEDULED);

        mockMvc.perform(get("/api/inspection").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data[0].id").exists());
        // Ascending order proof: the soonest (plusDays(3)) inspection must be first.
        Inspection soonest = inspectionRepository.findAll().stream()
                .min((a, b) -> a.getScheduledDate().compareTo(b.getScheduledDate())).orElseThrow();
        mockMvc.perform(get("/api/inspection").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].id").value(soonest.getId()));
    }

    @Test
    void listFiltersByStatus() throws Exception {
        saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.CANCELLED);

        mockMvc.perform(get("/api/inspection?status=CANCELLED").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].status").value("CANCELLED"));
    }

    @Test
    void listFiltersByDateRange() throws Exception {
        saveInspection(LocalDateTime.of(2027, 1, 5, 10, 0), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.of(2027, 1, 15, 10, 0), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.of(2027, 2, 5, 10, 0), Inspection.Status.SCHEDULED);

        mockMvc.perform(get("/api/inspection?from=2027-01-01&to=2027-01-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void listFiltersByDateRangeAndStatusCombined() throws Exception {
        saveInspection(LocalDateTime.of(2027, 1, 5, 10, 0), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.of(2027, 1, 15, 10, 0), Inspection.Status.CANCELLED);

        mockMvc.perform(get("/api/inspection?from=2027-01-01&to=2027-01-31&status=SCHEDULED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"));
    }

    @Test
    void listPaginatesWithLimitAndOffset() throws Exception {
        saveInspection(LocalDateTime.now().plusDays(1), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.now().plusDays(2), Inspection.Status.SCHEDULED);
        saveInspection(LocalDateTime.now().plusDays(3), Inspection.Status.SCHEDULED);

        mockMvc.perform(get("/api/inspection?limit=2&offset=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data", hasSize(2)));

        mockMvc.perform(get("/api/inspection?limit=2&offset=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // --- update ---

    @Test
    void updateReschedulesToAFutureDate() throws Exception {
        Inspection saved = saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.SCHEDULED);
        String body = objectMapper.writeValueAsString(Map.of("scheduledDate", "2027-06-01T09:00:00"));

        mockMvc.perform(put("/api/inspection/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledDate").value("2027-06-01T09:00:00"));
    }

    @Test
    void updateRescheduleToPastDateReturns400() throws Exception {
        Inspection saved = saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.SCHEDULED);
        String body = objectMapper.writeValueAsString(Map.of("scheduledDate", "2020-01-01T09:00:00"));

        mockMvc.perform(put("/api/inspection/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * The important behavioral case: closing out an inspection that already happened must not
     * be blocked by the future-date check, since the check only applies when scheduledDate
     * itself is part of the request (an actual reschedule).
     */
    @Test
    void markingAPastInspectionCompletedSucceedsWithoutTouchingTheDate() throws Exception {
        Inspection saved = saveInspection(LocalDateTime.now().minusDays(2), Inspection.Status.SCHEDULED);
        String body = objectMapper.writeValueAsString(Map.of(
                "status", "COMPLETED", "notes", "Inspection done, design finalized"));

        mockMvc.perform(put("/api/inspection/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.notes").value("Inspection done, design finalized"));
    }

    @Test
    void updateAppliesOnlyStatusWhenNotesAndDateOmitted() throws Exception {
        Inspection saved = saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.SCHEDULED);
        saved.setNotes("original note");
        inspectionRepository.save(saved);
        // Read back through the repository rather than trusting the in-memory object: H2's
        // DATETIME column truncates to microseconds, so the pre-save Java LocalDateTime
        // (nanosecond precision) doesn't string-match what the API actually returns even
        // though the value is otherwise unchanged.
        LocalDateTime originalDate = inspectionRepository.findById(saved.getId()).orElseThrow().getScheduledDate();

        String body = objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"));

        mockMvc.perform(put("/api/inspection/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.notes").value("original note"))
                .andExpect(jsonPath("$.scheduledDate").value(originalDate.toString()));
    }

    @Test
    void updateAppliesOnlyNotesWhenStatusAndDateOmitted() throws Exception {
        Inspection saved = saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.IN_PROGRESS);

        String body = objectMapper.writeValueAsString(Map.of("notes", "left a voicemail to confirm"));

        mockMvc.perform(put("/api/inspection/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.notes").value("left a voicemail to confirm"));
    }

    @Test
    void updateMissingReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "COMPLETED"));

        mockMvc.perform(put("/api/inspection/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- delete ---

    @Test
    void deleteRemovesTheInspection() throws Exception {
        Inspection saved = saveInspection(LocalDateTime.now().plusDays(5), Inspection.Status.SCHEDULED);

        mockMvc.perform(delete("/api/inspection/" + saved.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/inspection/" + saved.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        mockMvc.perform(delete("/api/inspection/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
