package org.fia.alumni.alumnifiauesbackend.entity.survey;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "survey_responses")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "user_id")
    private Long userId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "response_status_enum")
    private ResponseStatus status = ResponseStatus.IN_PROGRESS;

    // Respuestas completas en JSON (misma estructura que json_schema del survey)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", nullable = false, columnDefinition = "JSONB")
    private String responseJson;

    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
        if (status == null) status = ResponseStatus.IN_PROGRESS;
    }

    public enum ResponseStatus { IN_PROGRESS, COMPLETED }
}