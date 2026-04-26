package org.fia.alumni.alumnifiauesbackend.service.admin;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.audit.TimelineEventDTO;
import org.fia.alumni.alumnifiauesbackend.entity.activity.UserActivity;
import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.fia.alumni.alumnifiauesbackend.repository.activity.UserActivityRepository;
import org.fia.alumni.alumnifiauesbackend.repository.audit.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminTimelineService {

    private final AuditLogRepository auditLogRepository;
    private final UserActivityRepository activityRepository;

    public List<TimelineEventDTO> getComprehensiveTimelineForUser(Long userId) {
        List<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<UserActivity> activities = activityRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return Stream.concat(
                        auditLogs.stream().map(this::fromAudit),
                        activities.stream().map(this::fromActivity)
                )
                .sorted(Comparator.comparing(TimelineEventDTO::getTimestamp).reversed())
                .toList();
    }

    private TimelineEventDTO fromAudit(AuditLog log) {
        TimelineEventDTO dto = new TimelineEventDTO();
        dto.setEventType("AUDIT");
        dto.setSubType(log.getActionType().name());
        dto.setTimestamp(log.getCreatedAt());
        dto.setActor(log.getUsername() != null ? log.getUsername() : "Sistema");
        dto.setDescription(String.format("Acción: %s. Detalles: %s",
                log.getActionType().name(),
                log.getDetails() != null ? log.getDetails().toString() : "N/A"));
        dto.setPublic(false);
        return dto;
    }

    private TimelineEventDTO fromActivity(UserActivity activity) {
        TimelineEventDTO dto = new TimelineEventDTO();
        dto.setEventType("ACTIVITY");
        dto.setSubType(activity.getActivityType());
        dto.setTimestamp(activity.getCreatedAt());
        dto.setActor("Usuario");
        dto.setPublic(Boolean.TRUE.equals(activity.getIsPublic()));
        dto.setDescription(String.format("Actividad: %s. Contexto: %s",
                activity.getActivityType(),
                activity.getContext() != null ? activity.getContext().toString() : "N/A"));
        return dto;
    }
}