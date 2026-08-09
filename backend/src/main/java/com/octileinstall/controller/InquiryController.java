package com.octileinstall.controller;

import com.octileinstall.dto.InquiryCreateRequest;
import com.octileinstall.dto.InquiryDTO;
import com.octileinstall.dto.InquiryUpdateRequest;
import com.octileinstall.dto.PagedResponse;
import com.octileinstall.entity.Inquiry;
import com.octileinstall.security.UserPrincipal;
import com.octileinstall.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<PagedResponse<InquiryDTO>> list(
            @RequestParam(required = false) Inquiry.Status status,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(inquiryService.list(status, limit, offset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InquiryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(inquiryService.getById(id));
    }

    @PostMapping
    public ResponseEntity<InquiryDTO> create(
            @Valid @RequestBody InquiryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        InquiryDTO created = inquiryService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InquiryDTO> update(
            @PathVariable Long id,
            @RequestBody InquiryUpdateRequest request) {
        return ResponseEntity.ok(inquiryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inquiryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
