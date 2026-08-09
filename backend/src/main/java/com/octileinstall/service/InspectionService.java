package com.octileinstall.service;

import com.octileinstall.dto.InspectionCreateRequest;
import com.octileinstall.dto.InspectionDTO;
import com.octileinstall.dto.InspectionUpdateRequest;
import com.octileinstall.dto.PagedResponse;
import com.octileinstall.entity.Inquiry;
import com.octileinstall.entity.Inspection;
import com.octileinstall.exception.NotFoundException;
import com.octileinstall.exception.ValidationException;
import com.octileinstall.repository.InquiryRepository;
import com.octileinstall.repository.InspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private static final int DEFAULT_LIMIT = 20;

    private final InspectionRepository inspectionRepository;
    private final InquiryRepository inquiryRepository;

    @Transactional(readOnly = true)
    public PagedResponse<InspectionDTO> list(
            LocalDate from, LocalDate to, Inspection.Status status, Integer limit, Integer offset) {
        int pageSize = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;
        int startIndex = (offset == null || offset < 0) ? 0 : offset;
        // Same page-alignment assumption as InquiryService.list(): offset = page * limit.
        int page = startIndex / pageSize;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "scheduledDate"));

        boolean hasRange = from != null || to != null;
        Page<Inspection> result;
        if (hasRange && status != null) {
            result = inspectionRepository.findByScheduledDateBetweenAndStatus(
                    startOfRange(from), endOfRange(to), status, pageable);
        } else if (hasRange) {
            result = inspectionRepository.findByScheduledDateBetween(startOfRange(from), endOfRange(to), pageable);
        } else if (status != null) {
            result = inspectionRepository.findByStatus(status, pageable);
        } else {
            result = inspectionRepository.findAll(pageable);
        }

        return new PagedResponse<>(result.map(InspectionDTO::from).getContent(), result.getTotalElements());
    }

    // ?from=2026-08-01&to=2026-08-31 per BACKEND_PLAN.md §5 is calendar-day granularity, but
    // scheduledDate is a LocalDateTime. An absent bound is unbounded in that direction rather
    // than defaulting to "today", so ?to=2026-08-31 alone still returns everything up through
    // that day with no lower limit.
    private LocalDateTime startOfRange(LocalDate from) {
        return from != null ? from.atStartOfDay() : LocalDateTime.MIN;
    }

    private LocalDateTime endOfRange(LocalDate to) {
        return to != null ? to.atTime(LocalTime.MAX) : LocalDateTime.MAX;
    }

    @Transactional(readOnly = true)
    public InspectionDTO getById(Long id) {
        return InspectionDTO.from(findEntity(id));
    }

    @Transactional
    public InspectionDTO create(InspectionCreateRequest request) {
        Inquiry inquiry = inquiryRepository.findById(request.getInquiryId())
                .orElseThrow(() -> new NotFoundException("Inquiry " + request.getInquiryId() + " not found"));

        requireFutureDate(request.getScheduledDate());

        Inspection inspection = new Inspection();
        inspection.setInquiry(inquiry);
        inspection.setScheduledDate(request.getScheduledDate());
        inspection.setLocation(request.getLocation());
        // status defaults to SCHEDULED via the entity's field initializer

        return InspectionDTO.from(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionDTO update(Long id, InspectionUpdateRequest request) {
        Inspection inspection = findEntity(id);

        if (request.getScheduledDate() != null) {
            // Only reschedules are validated against "now". A status-only update (e.g. marking
            // an already-past inspection COMPLETED) must not be blocked by this check - that's
            // the normal way a completed inspection gets closed out.
            requireFutureDate(request.getScheduledDate());
            inspection.setScheduledDate(request.getScheduledDate());
        }
        if (request.getStatus() != null) {
            inspection.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            inspection.setNotes(request.getNotes());
        }

        return InspectionDTO.from(inspectionRepository.save(inspection));
    }

    @Transactional
    public void delete(Long id) {
        if (!inspectionRepository.existsById(id)) {
            throw new NotFoundException("Inspection " + id + " not found");
        }
        inspectionRepository.deleteById(id);
    }

    private Inspection findEntity(Long id) {
        return inspectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inspection " + id + " not found"));
    }

    private void requireFutureDate(LocalDateTime scheduledDate) {
        if (!scheduledDate.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Scheduled date must be in the future");
        }
    }
}
