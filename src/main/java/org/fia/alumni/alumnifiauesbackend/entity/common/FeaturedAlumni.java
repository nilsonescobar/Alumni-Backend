package org.fia.alumni.alumnifiauesbackend.entity.common;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "outstanding_alumni")   // nombre real en BD
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FeaturedAlumni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "recognition_date", nullable = false)  // nombre real
    private LocalDate recognitionDate;

    @Column(name = "awarded_by_user_id")   // nombre real
    private Long awardedByUserId;

    @Column(name = "reference_url", length = 512)
    private String referenceUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}