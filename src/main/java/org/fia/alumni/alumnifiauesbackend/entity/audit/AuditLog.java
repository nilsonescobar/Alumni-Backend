package org.fia.alumni.alumnifiauesbackend.entity.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action_type", nullable = false)
    private AuditAction actionType;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "record_id", length = 255)
    private String recordId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "ip_address")
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum AuditAction {
        INSERT, UPDATE, DELETE,
        LOGIN_SUCCESS, LOGIN_SUCCESS_MFA, LOGIN_FAILED,
        LOGOUT, PASSWORD_CHANGE, PASSWORD_CHANGE_FAILED,
        PASSWORD_RESET_REQUESTED, MFA_ENABLED, MFA_SETUP_INITIATED,
        MFA_VERIFICATION_FAILED, MFA_DISABLED, ADMIN_MFA_DISABLE,
        ADMIN_ROLE_CHANGE, VERIFICATION_INITIATED, VERIFICATION_APPROVED,
        VERIFICATION_REJECTED, PROFILE_IMAGE_UPDATED, PROFILE_UPDATED,
        ACCOUNT_DEACTIVATED, SURVEY_CREATED, SURVEY_PUBLISHED,
        SURVEY_CLOSED, SURVEY_RESPONDED, SURVEY_ASSIGNED, SURVEY_MODIFIED,
        CONNECTION_REQUEST, CONNECTION_ACCEPTED, POST_CREATED, POST_DELETED,
        CONTENT_REPORTED, REPORT_REVIEWED
    }
}