package org.fia.alumni.alumnifiauesbackend.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "user_type", nullable = false)
    private UserType userType = UserType.GRADUATE;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "token_expiration")
    private LocalDateTime tokenExpiration;

    @Column(name = "registered_with", length = 20)
    private String registeredWith;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_must_change")
    private Boolean passwordMustChange = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "source_graduate_id")
    private Long sourceGraduateId;

    @Column(name = "email_notification_enabled")
    private Boolean emailNotificationEnabled = true;

    @Column(name = "push_notification_enabled")
    private Boolean pushNotificationEnabled = true;

    @Column(name = "profile_completion_percentage")
    private Integer profileCompletionPercentage = 0;

    @Column(name = "has_disability", nullable = false)
    private Boolean hasDisability = false;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "disability_type")
    private DisabilityType disabilityType;

    @Column(name = "disability_details", length = 255)
    private String disabilityDetails;

    @Column(name = "deactivation_reason", columnDefinition = "TEXT")
    private String deactivationReason;

    @Column(name = "account_deactivated_at")
    private LocalDateTime accountDeactivatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Profile profile;

    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    public void deactivate(String reason) {
        this.active = false;
        this.deactivationReason = reason;
        this.accountDeactivatedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.active = true;
        this.deactivationReason = null;
        this.accountDeactivatedAt = null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (passwordChangedAt == null) passwordChangedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UserType {
        GRADUATE, ADMIN, VERIFIER, DIRECTOR
    }

    public enum DisabilityType {
        PHYSICAL, VISUAL, AUDITORY, INTELLECTUAL, PSYCHOSOCIAL, MULTIPLE, OTHER
    }
}