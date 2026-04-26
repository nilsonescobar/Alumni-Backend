package org.fia.alumni.alumnifiauesbackend.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrivacySettingsRequest {

    @NotNull(message = "Debe especificar si mostrar el email")
    private Boolean showEmail;

    @NotNull(message = "Debe especificar si mostrar el teléfono")
    private Boolean showPhone;

    @NotNull(message = "Debe especificar si mostrar el trabajo actual")
    private Boolean showCurrentJob;

    @NotNull(message = "Debe especificar si mostrar el año de graduación")
    private Boolean showGraduationYear;

    @NotNull(message = "Debe especificar si mostrar el GPA")
    private Boolean showGpa;

    @NotNull(message = "Debe especificar la visibilidad del perfil")
    private String profileVisibility;

    @NotNull(message = "Debe especificar si mostrar las conexiones")
    private Boolean showConnections;

    @NotNull(message = "Debe especificar si permitir solicitudes de conexión")
    private Boolean allowConnectionRequests;
}