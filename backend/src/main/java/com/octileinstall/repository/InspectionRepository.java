package com.octileinstall.repository;

import com.octileinstall.entity.Inspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    Page<Inspection> findByStatus(Inspection.Status status, Pageable pageable);

    Page<Inspection> findByScheduledDateBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Inspection> findByScheduledDateBetweenAndStatus(
            LocalDateTime from, LocalDateTime to, Inspection.Status status, Pageable pageable);
}
