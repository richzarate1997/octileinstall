package com.octileinstall.dto;

import com.octileinstall.entity.Inspection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Partial update: a field left out of the request body is left unchanged (same pattern as
 * InquiryUpdateRequest). scheduledDate doubles as "reschedule" per BACKEND_PLAN.md §3 Phase 5 -
 * when present it's validated as being in the future; when absent (e.g. a status-only update
 * marking a past inspection COMPLETED) no future-date check applies. See InspectionService.update().
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionUpdateRequest {
    private Inspection.Status status;
    private String notes;
    private LocalDateTime scheduledDate;
}
