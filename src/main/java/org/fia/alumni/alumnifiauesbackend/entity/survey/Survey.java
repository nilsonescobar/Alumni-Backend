package org.fia.alumni.alumnifiauesbackend.entity.survey;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "surveys")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // El formulario completo en formato SurveyJS/JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_schema", nullable = false, columnDefinition = "JSONB")
    private String jsonSchema;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "survey_status_enum")
    private SurveyStatus status = SurveyStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Column(name = "allow_multiple_responses")
    private Boolean allowMultipleResponses = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = SurveyStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SurveyStatus { DRAFT, ACTIVE, CLOSED, ARCHIVED }
}