package org.fia.alumni.alumnifiauesbackend.entity.event;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    // NULL = no marcado, TRUE = asistió, FALSE = no asistió
    @Column(name = "attended")
    private Boolean attended;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @PrePersist
    protected void onCreate() {
        if (registrationDate == null) registrationDate = LocalDateTime.now();
    }
}