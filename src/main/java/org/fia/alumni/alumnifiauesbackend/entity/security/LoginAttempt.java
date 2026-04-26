package org.fia.alumni.alumnifiauesbackend.entity.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    @Column(name = "last_failed_attempt")
    private LocalDateTime lastFailedAttempt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_success")
    private LocalDateTime lastSuccess;

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

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
        this.lastFailedAttempt = LocalDateTime.now();
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lastFailedAttempt = null;
        this.lockedUntil = null;
        this.lastSuccess = LocalDateTime.now();
    }

    public void lock(int hours) {
        this.lockedUntil = LocalDateTime.now().plusHours(hours);
    }
}