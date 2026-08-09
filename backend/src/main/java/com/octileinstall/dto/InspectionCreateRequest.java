package com.octileinstall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionCreateRequest {

    @NotNull(message = "Inquiry id is required")
    private Long inquiryId;

    @NotNull(message = "Scheduled date/time is required")
    private LocalDateTime scheduledDate;

    private String location;
}
