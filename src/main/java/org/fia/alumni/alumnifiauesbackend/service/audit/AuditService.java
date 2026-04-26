package org.fia.alumni.alumnifiauesbackend.service.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.fia.alumni.alumnifiauesbackend.repository.audit.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId,
                    String username,
                    AuditLog.AuditAction action,
                    String tableName,
                    String recordId,
                    Map<String, Object> details,
                    String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .actionType(action)
                    .tableName(tableName)
                    .recordId(recordId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log for action {}: {}", action, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuth(Long userId,
                        String username,
                        AuditLog.AuditAction action,
                        Map<String, Object> details,
                        String ipAddress) {
        log(userId, username, action, null, null, details, ipAddress);
    }

    public String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri;
        return request.getRemoteAddr();
    }
}