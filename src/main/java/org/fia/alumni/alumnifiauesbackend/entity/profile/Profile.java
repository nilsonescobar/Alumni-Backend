package org.fia.alumni.alumnifiauesbackend.entity.profile;

import jakarta.persistence.*;
import lombok.*;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(name = "identity_document", length = 20)
    private String identityDocument;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "graduation_gpa", precision = 3, scale = 2)
    private BigDecimal graduationGpa;

    @Column(name = "career_id")
    private Long careerId;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "profile_picture", length = 255)
    private String profilePicture;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "admission_year")
    private Integer admissionYear;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "privacy_settings", columnDefinition = "jsonb")
    private Map<String, Object> privacySettings;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getFullName() {
        return displayName != null && !displayName.isBlank()
                ? displayName
                : firstName + " " + lastName;
    }

    @PrePersist
    protected void onCreate() {
        if (displayName == null || displayName.isBlank()) {
            displayName = firstName + " " + lastName;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}