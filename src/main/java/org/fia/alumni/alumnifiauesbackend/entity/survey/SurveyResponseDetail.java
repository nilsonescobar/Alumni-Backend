package org.fia.alumni.alumnifiauesbackend.entity.survey;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "survey_response_details")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SurveyResponseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "answer_value", columnDefinition = "TEXT", nullable = false)
    private String answerValue;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        if (answeredAt == null) answeredAt = LocalDateTime.now();
    }
}