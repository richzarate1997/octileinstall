package com.octileinstall.dto;

import com.octileinstall.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryDTO {
    private Long id;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Inquiry.ProjectType projectType;
    private String city;
    private String message;
    private Inquiry.Status status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;

    public static InquiryDTO from(Inquiry inquiry) {
        return InquiryDTO.builder()
                .id(inquiry.getId())
                .customerName(inquiry.getCustomerName())
                .customerEmail(inquiry.getCustomerEmail())
                .customerPhone(inquiry.getCustomerPhone())
                .projectType(inquiry.getProjectType())
                .city(inquiry.getCity())
                .message(inquiry.getMessage())
                .status(inquiry.getStatus())
                .notes(inquiry.getNotes())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .createdBy(inquiry.getCreatedByUser() != null ? inquiry.getCreatedByUser().getId() : null)
                .build();
    }
}
