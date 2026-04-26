package org.fia.alumni.alumnifiauesbackend.entity.verification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_verifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "name_match")
    private Boolean nameMatch = false;

    @Column(name = "student_id_match")
    private Boolean studentIdMatch = false;

    @Column(name = "document_match")
    private Boolean documentMatch = false;

    @Column(name = "match_score")
    private Integer matchScore = 0;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum VerificationStatus {
        PENDING, IN_REVIEW, APPROVED, REJECTED
    }
}