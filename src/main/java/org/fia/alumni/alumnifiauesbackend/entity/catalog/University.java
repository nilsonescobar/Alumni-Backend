package org.fia.alumni.alumnifiauesbackend.entity.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "universities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "country_id", nullable = false)
    private Long countryId;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", insertable = false, updatable = false)
    private Country country;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}