package com.octileinstall.dto;

import com.octileinstall.entity.Inquiry;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of the unauthenticated {@code POST /api/public/inquiry} endpoint - the public site's
 * quote form submits this directly instead of going through Formspree. See
 * PublicInquiryController.
 *
 * <p>Deliberately a separate type from {@link InquiryCreateRequest} rather than reusing it:
 * this one is reachable by anyone on the internet, not just an authenticated staff member, so
 * it carries its own {@code @Size} caps (the authenticated admin form has no such limits - a
 * staff member typing too much is not a threat model) and the {@link #company} honeypot field
 * that has no business existing on the admin DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicInquiryCreateRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name is too long")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email should be valid")
    @Size(max = 255, message = "Customer email is too long")
    private String customerEmail;

    @Size(max = 20, message = "Customer phone is too long")
    private String customerPhone;

    private Inquiry.ProjectType projectType;

    @Size(max = 255, message = "City is too long")
    private String city;

    @Size(max = 5000, message = "Message is too long")
    private String message;

    /**
     * Anti-bot honeypot. Matches the hidden {@code name="company"} input in index.html's
     * quote form, which is invisible to a human and off the tab order - a real visitor never
     * fills it in, but a naive bot that fills every field will. Non-blank means "drop this
     * submission" (see PublicInquiryController), never "reject it" - responding with an error
     * would tell the bot its payload was noticed, inviting it to adapt.
     */
    private String company;
}
