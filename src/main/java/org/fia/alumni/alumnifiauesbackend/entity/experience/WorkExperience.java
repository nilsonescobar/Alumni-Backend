package org.fia.alumni.alumnifiauesbackend.entity.experience;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_experience")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "achievements", columnDefinition = "text[]")
    private String[] achievements;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "skills_used", columnDefinition = "varchar(100)[]")
    private String[] skillsUsed;

    @Column(name = "salary_range", length = 50)
    private String salaryRange;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @Column(name = "verified")
    private Boolean verified = false;

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