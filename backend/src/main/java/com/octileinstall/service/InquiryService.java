package com.octileinstall.service;

import com.octileinstall.dto.InquiryCreateRequest;
import com.octileinstall.dto.InquiryDTO;
import com.octileinstall.dto.InquiryUpdateRequest;
import com.octileinstall.dto.PagedResponse;
import com.octileinstall.dto.PublicInquiryCreateRequest;
import com.octileinstall.entity.Inquiry;
import com.octileinstall.entity.User;
import com.octileinstall.exception.NotFoundException;
import com.octileinstall.repository.InquiryRepository;
import com.octileinstall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final int DEFAULT_LIMIT = 20;

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<InquiryDTO> list(Inquiry.Status status, Integer limit, Integer offset) {
        int pageSize = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;
        int startIndex = (offset == null || offset < 0) ? 0 : offset;
        // Spring Data pages by page number, not raw row offset. The admin dashboard only
        // ever requests page-aligned offsets (offset = page * limit), so this division is
        // exact for every real caller; an unaligned offset just rounds down to the start
        // of its containing page rather than erroring.
        int page = startIndex / pageSize;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Inquiry> result = (status != null)
                ? inquiryRepository.findByStatus(status, pageable)
                : inquiryRepository.findAll(pageable);

        return new PagedResponse<>(result.map(InquiryDTO::from).getContent(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public InquiryDTO getById(Long id) {
        return InquiryDTO.from(findEntity(id));
    }

    @Transactional
    public InquiryDTO create(InquiryCreateRequest request, Long createdByUserId) {
        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new NotFoundException("User " + createdByUserId + " not found"));

        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerName(request.getCustomerName());
        inquiry.setCustomerEmail(request.getCustomerEmail());
        inquiry.setCustomerPhone(request.getCustomerPhone());
        inquiry.setProjectType(request.getProjectType());
        inquiry.setCity(request.getCity());
        inquiry.setMessage(request.getMessage());
        inquiry.setCreatedByUser(creator);
        // status defaults to NEW via the entity's field initializer

        return InquiryDTO.from(inquiryRepository.save(inquiry));
    }

    /**
     * Creates an inquiry from the public quote form (PublicInquiryController) - the
     * unauthenticated counterpart to {@link #create}. There is no staff member to record as
     * {@code createdByUser}, so it's left null rather than requiring a lookup like the
     * authenticated path does. Callers are responsible for the honeypot check and rate
     * limiting before reaching this method; it unconditionally persists.
     */
    @Transactional
    public InquiryDTO createPublic(PublicInquiryCreateRequest request) {
        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerName(request.getCustomerName());
        inquiry.setCustomerEmail(request.getCustomerEmail());
        inquiry.setCustomerPhone(request.getCustomerPhone());
        inquiry.setProjectType(request.getProjectType());
        inquiry.setCity(request.getCity());
        inquiry.setMessage(request.getMessage());
        // createdByUser stays null; status defaults to NEW via the entity's field initializer.

        return InquiryDTO.from(inquiryRepository.save(inquiry));
    }

    @Transactional
    public InquiryDTO update(Long id, InquiryUpdateRequest request) {
        Inquiry inquiry = findEntity(id);

        if (request.getStatus() != null) {
            inquiry.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            inquiry.setNotes(request.getNotes());
        }

        return InquiryDTO.from(inquiryRepository.save(inquiry));
    }

    @Transactional
    public void delete(Long id) {
        if (!inquiryRepository.existsById(id)) {
            throw new NotFoundException("Inquiry " + id + " not found");
        }
        inquiryRepository.deleteById(id);
    }

    private Inquiry findEntity(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inquiry " + id + " not found"));
    }
}
