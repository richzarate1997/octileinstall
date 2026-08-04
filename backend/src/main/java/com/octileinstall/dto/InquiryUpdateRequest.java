package com.octileinstall.dto;

import com.octileinstall.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Partial update: a field left out of the request body is left unchanged. See InquiryService.update(). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryUpdateRequest {
    private Inquiry.Status status;
    private String notes;
}
