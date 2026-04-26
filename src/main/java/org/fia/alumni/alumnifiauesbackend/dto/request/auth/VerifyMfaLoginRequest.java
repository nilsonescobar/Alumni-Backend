package org.fia.alumni.alumnifiauesbackend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyMfaLoginRequest {

    @NotBlank(message = "El token temporal es obligatorio")
    private String mfaToken;

    @NotBlank(message = "El código MFA es obligatorio")
    private String code;
}