package org.fia.alumni.alumnifiauesbackend.entity.survey;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "survey_assignments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SurveyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, columnDefinition = "assignment_type_enum")
    private AssignmentType assignmentType;

    @Column(name = "career_id")
    private Long careerId;

    @Column(name = "graduation_year_start")
    private Integer graduationYearStart;

    @Column(name = "graduation_year_end")
    private Integer graduationYearEnd;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", columnDefinition = "user_type_enum")
    private UserType userType;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }

    public enum AssignmentType {
        ALL, CAREER, GRADUATION_YEAR, USER_TYPE, SPECIFIC_USERS
    }

    public enum UserType {
        GRADUADO, ESTUDIANTE, DOCENTE, ADMIN, ALUMNI
    }
}