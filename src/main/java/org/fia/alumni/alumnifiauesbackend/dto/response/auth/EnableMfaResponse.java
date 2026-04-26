package org.fia.alumni.alumnifiauesbackend.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class EnableMfaResponse {
    private String secretKey;
    private String qrCodeUrl;
    private String[] backupCodes;
}
