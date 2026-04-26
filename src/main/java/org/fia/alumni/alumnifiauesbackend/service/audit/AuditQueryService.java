package org.fia.alumni.alumnifiauesbackend.service.audit;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.fia.alumni.alumnifiauesbackend.dto.audit.AccessLogDTO;
import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.fia.alumni.alumnifiauesbackend.repository.audit.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    private static final List<AuditLog.AuditAction> ACCESS_ACTIONS = List.of(
            AuditLog.AuditAction.LOGIN_SUCCESS,
            AuditLog.AuditAction.LOGIN_SUCCESS_MFA,
            AuditLog.AuditAction.LOGIN_FAILED,
            AuditLog.AuditAction.LOGOUT
    );

    @Transactional(readOnly = true)
    public List<AccessLogDTO> findAccessLogs(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findAccessLogs(start, end, ACCESS_ACTIONS)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AccessLogDTO> findAccessLogsByUser(Long userId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findAccessLogsByUser(userId, start, end, ACCESS_ACTIONS)
                .stream().map(this::toDto).toList();
    }


    public ByteArrayInputStream exportAccessLogsToCsv(List<AccessLogDTO> logs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(
                new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader("userId", "username", "actionType", "ipAddress", "createdAt"))) {
            for (AccessLogDTO log : logs) {
                printer.printRecord(
                        log.getUserId(),
                        log.getUsername(),
                        log.getActionType(),
                        log.getIpAddress(),
                        log.getCreatedAt()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generando CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private AccessLogDTO toDto(AuditLog log) {
        return AccessLogDTO.builder()
                .userId(log.getUserId())
                .username(log.getUsername())
                .actionType(log.getActionType().name())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}