package org.fia.alumni.alumnifiauesbackend.entity.job;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_opportunities")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "how_to_apply", nullable = false, columnDefinition = "TEXT")
    private String howToApply;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "is_remote")
    private Boolean isRemote = false;

    @Column(name = "job_type", length = 50)
    private String jobType;                   // FULL_TIME, PART_TIME, FREELANCE...

    @Column(name = "experience_level", length = 50)
    private String experienceLevel;           // JUNIOR, MID, SENIOR...

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "posted_by_user_id", nullable = false)
    private Long postedByUserId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

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
}