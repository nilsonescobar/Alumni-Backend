package org.fia.alumni.alumnifiauesbackend.dto.request.profile;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    // Nombre para mostrar — libre de modificar, no afecta firstName/lastName
    @Size(max = 150, message = "El nombre a mostrar no puede exceder 150 caracteres")
    private String displayName;

    @Size(max = 15, message = "El teléfono no puede exceder 15 caracteres")
    @Pattern(regexp = "^[+]?[0-9\\s-()]*$", message = "Formato de teléfono inválido")
    private String phone;

    @Size(max = 500, message = "La biografía no puede exceder 500 caracteres")
    private String bio;

    @Size(max = 255, message = "La URL de LinkedIn no puede exceder 255 caracteres")
    @Pattern(regexp = "^(https?://)?(www\\.)?linkedin\\.com/.*$", message = "URL de LinkedIn inválida")
    private String linkedinUrl;

    @Size(max = 255, message = "La URL del sitio web no puede exceder 255 caracteres")
    @Pattern(regexp = "^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*$", message = "URL de sitio web inválida")
    private String websiteUrl;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String address;

    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    private String city;

    private Long countryId;

    private Long careerId;
}