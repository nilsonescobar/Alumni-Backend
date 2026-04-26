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
public class UpdateNotificationSettingsRequest {

    @NotNull(message = "Debe especificar si habilitar notificaciones por email")
    private Boolean emailNotificationEnabled;

    @NotNull(message = "Debe especificar si habilitar notificaciones push")
    private Boolean pushNotificationEnabled;
}