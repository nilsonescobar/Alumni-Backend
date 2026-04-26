package org.fia.alumni.alumnifiauesbackend.entity.survey;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "survey_assigned_users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SurveyAssignedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notified")
    private Boolean notified = false;

    @Column(name = "notification_date")
    private LocalDateTime notificationDate;
}