package org.fia.alumni.alumnifiauesbackend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyMfaRequest {

    @NotBlank(message = "MFA code is required")
    @Pattern(regexp = "^\\d{6}$", message = "MFA code must be 6 digits")
    private String code;
}

