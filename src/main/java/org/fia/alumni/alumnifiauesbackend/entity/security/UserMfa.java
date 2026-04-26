package org.fia.alumni.alumnifiauesbackend.entity.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_mfa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMfa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "secret_key", nullable = false, length = 32)
    private String secretKey;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}