package com.octileinstall.dto;

import com.octileinstall.entity.Inquiry;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryCreateRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email should be valid")
    private String customerEmail;

    private String customerPhone;

    private Inquiry.ProjectType projectType;

    private String city;

    private String message;
}
