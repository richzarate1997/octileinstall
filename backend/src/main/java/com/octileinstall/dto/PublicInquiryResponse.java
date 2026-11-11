package com.octileinstall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for {@code POST /api/public/inquiry}. Deliberately minimal - unlike
 * {@link InquiryDTO}, it never exposes the row id, status, timestamps, or anything else about
 * the admin data model to an anonymous caller. It is also the exact same shape whether the
 * submission was real or silently dropped as a honeypot hit (see PublicInquiryController),
 * so a bot probing the endpoint cannot distinguish "accepted" from "detected and ignored".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInquiryResponse {
    private boolean received;
    private String message;
}
