package org.fia.alumni.alumnifiauesbackend.dto.verification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserVerificationInfo {
    // Datos del usuario
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String studentId;
    private String identityDocument;
    private LocalDateTime createdAt;

    // Datos del graduado (de graduates, solo para comparar)
    private String graduateFirstName;
    private String graduateLastName;
    private String graduateStudentId;
    private String graduateIdentityDocument;
    private Integer graduationYear;

    // Estado verificación
    private String status;
    private Boolean nameMatch;
    private Boolean studentIdMatch;
    private Boolean documentMatch;
    private Integer matchScore;
    private String observations;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
}