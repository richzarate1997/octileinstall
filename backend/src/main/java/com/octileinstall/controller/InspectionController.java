package com.octileinstall.controller;

import com.octileinstall.dto.InspectionCreateRequest;
import com.octileinstall.dto.InspectionDTO;
import com.octileinstall.dto.InspectionUpdateRequest;
import com.octileinstall.dto.PagedResponse;
import com.octileinstall.entity.Inspection;
import com.octileinstall.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/inspection")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @GetMapping
    public ResponseEntity<PagedResponse<InspectionDTO>> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Inspection.Status status,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return ResponseEntity.ok(inspectionService.list(from, to, status, limit, offset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(inspectionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<InspectionDTO> create(@Valid @RequestBody InspectionCreateRequest request) {
        InspectionDTO created = inspectionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InspectionDTO> update(
            @PathVariable Long id,
            @RequestBody InspectionUpdateRequest request) {
        return ResponseEntity.ok(inspectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inspectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
