package org.fia.alumni.alumnifiauesbackend.dto.response.user;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class UserAdminProfileResponse {
    // Todo lo público
    private Long id;
    private String username;
    private String email;
    private String userType;
    private Boolean active;
    private Boolean emailVerified;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private Integer profileCompletionPercentage;
    // Info sensible
    private String firstName;
    private String lastName;
    private String studentId;
    private String identityDocument;
    private Boolean hasDisability;
    private String disabilityType;
    private String disabilityDetails;
    private String deactivationReason;
    private LocalDateTime accountDeactivatedAt;
    // Seguridad
    private Boolean mfaEnabled;
    private Boolean passwordMustChange;
    private LocalDateTime passwordChangedAt;
    // Verificación
    private String verificationStatus;
    private Integer matchScore;
    private String verificationObservations;
    // Profile
    private String profilePicture;
    private String careerName;
    private String universityName;
    private Integer graduationYear;
    private Integer admissionYear;
    private Double gpa;
    private String bio;
    private String phone;
    private String city;
    private String countryName;
    private String linkedinUrl;
    private String websiteUrl;
}