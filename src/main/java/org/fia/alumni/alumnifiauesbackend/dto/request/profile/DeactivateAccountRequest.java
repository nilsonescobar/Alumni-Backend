package org.fia.alumni.alumnifiauesbackend.dto.request.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivateAccountRequest {

    @NotBlank(message = "Debe proporcionar una razón para desactivar la cuenta")
    @Size(min = 10, max = 500, message = "La razón debe tener entre 10 y 500 caracteres")
    private String reason;
}