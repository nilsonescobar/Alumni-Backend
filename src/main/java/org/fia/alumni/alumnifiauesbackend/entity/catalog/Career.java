package org.fia.alumni.alumnifiauesbackend.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "careers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Career {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", insertable = false, updatable = false)
    private University university;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}