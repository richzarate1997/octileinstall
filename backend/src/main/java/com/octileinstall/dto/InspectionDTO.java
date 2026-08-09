package com.octileinstall.dto;

import com.octileinstall.entity.Inspection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDTO {
    private Long id;
    private Long inquiryId;
    private LocalDateTime scheduledDate;
    private String location;
    private Inspection.Status status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InspectionDTO from(Inspection inspection) {
        return InspectionDTO.builder()
                .id(inspection.getId())
                .inquiryId(inspection.getInquiry() != null ? inspection.getInquiry().getId() : null)
                .scheduledDate(inspection.getScheduledDate())
                .location(inspection.getLocation())
                .status(inspection.getStatus())
                .notes(inspection.getNotes())
                .createdAt(inspection.getCreatedAt())
                .updatedAt(inspection.getUpdatedAt())
                .build();
    }
}
