package org.fia.alumni.alumnifiauesbackend.entity.education;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "postgraduate_studies")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PostgraduateStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "degree_title", nullable = false, length = 150)
    private String degreeTitle;

    @Column(name = "university_id")
    private Long universityId;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "admission_year")
    private Integer admissionYear;

    @Column(name = "completion_year")
    private Integer completionYear;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "funding")
    private Funding funding = Funding.SELF_FUNDED;

    @Column(name = "degree_image", length = 255)
    private String degreeImage;

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

    public enum Funding {
        SCHOLARSHIP, SELF_FUNDED
    }
}