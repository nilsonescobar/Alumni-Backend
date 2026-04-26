package org.fia.alumni.alumnifiauesbackend.dto.request.verification;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerificationDecisionRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotNull(message = "Debe especificar si aprueba o rechaza")
    private Boolean approved;

    private Boolean nameMatch;
    private Boolean studentIdMatch;
    private Boolean documentMatch;

    private String observations;
}