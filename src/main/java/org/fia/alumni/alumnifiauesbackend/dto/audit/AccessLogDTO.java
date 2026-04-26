package org.fia.alumni.alumnifiauesbackend.dto.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AccessLogDTO {
    private Long userId;
    private String username;
    private String actionType;
    private String ipAddress;
    private LocalDateTime createdAt;
}