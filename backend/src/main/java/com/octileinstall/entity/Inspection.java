package com.octileinstall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspections", indexes = {
        @Index(name = "idx_inspection_scheduled", columnList = "scheduled_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    @NotNull(message = "Inquiry is required")
    private Inquiry inquiry;

    // Single DATETIME for both date and time - see BACKEND_PLAN.md §9. An earlier draft
    // split this into scheduledDate (DATE) + scheduledTime (TIME), which is redundant
    // and invites the two disagreeing. One field, one source of truth.
    @Column(nullable = false)
    @NotNull(message = "Scheduled date/time is required")
    private LocalDateTime scheduledDate;

    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'SCHEDULED'")
    private Status status = Status.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
