package com.octileinstall.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries", indexes = {
        @Index(name = "idx_inquiry_status", columnList = "status"),
        @Index(name = "idx_inquiry_created", columnList = "created_at"),
        @Index(name = "idx_inquiry_created_by", columnList = "created_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Column(nullable = false)
    private String customerName;

    @Email(message = "Customer email should be valid")
    @NotBlank(message = "Customer email is required")
    @Column(nullable = false)
    private String customerEmail;

    private String customerPhone;

    @Enumerated(EnumType.STRING)
    private ProjectType projectType;

    private String city;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'NEW'")
    private Status status = Status.NEW;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdByUser;

    public enum ProjectType {
        CUSTOM_SHOWER("Custom Shower"),
        BATHROOM_FLOORS_WALLS("Bathroom Floors & Walls"),
        KITCHEN_BACKSPLASHES("Kitchen Backsplashes"),
        FLOORING("Flooring - Tile, Marble & Mosaic"),
        TUB_SURROUNDS("Tub Surrounds"),
        REPAIRS("Repairs");

        private final String displayName;

        ProjectType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Status {
        NEW, CONTACTED, QUOTED, SCHEDULED, COMPLETED, ARCHIVED
    }
}
