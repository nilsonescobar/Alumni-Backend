package org.fia.alumni.alumnifiauesbackend.dto.response.user;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class UserVerifierProfileResponse {
    private Long id;
    private String email;
    private String userType;
    private Boolean active;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    // Info de perfil completa para verificar
    private String firstName;
    private String lastName;
    private String studentId;
    private String identityDocument;
    private String profilePicture;
    private String careerName;
    private String universityName;
    private Integer graduationYear;
    private Integer admissionYear;
    private Double gpa;
    // Estado verificación (solo lectura)
    private String verificationStatus;
    private Integer matchScore;
    private String verificationObservations;
    private LocalDateTime verificationStartedAt;
    private LocalDateTime verificationResolvedAt;
}