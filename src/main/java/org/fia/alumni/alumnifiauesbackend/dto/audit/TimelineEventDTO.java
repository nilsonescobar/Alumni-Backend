package org.fia.alumni.alumnifiauesbackend.dto.audit;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimelineEventDTO {
    private String eventType;
    private String subType;
    private LocalDateTime timestamp;
    private String actor;
    private String description;
    private boolean isPublic;
}