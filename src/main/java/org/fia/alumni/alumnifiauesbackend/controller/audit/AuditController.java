package org.fia.alumni.alumnifiauesbackend.controller.audit;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.audit.AccessLogDTO;
import org.fia.alumni.alumnifiauesbackend.security.SecurityUtils;
import org.fia.alumni.alumnifiauesbackend.security.jwt.JwtTokenProvider;
import org.fia.alumni.alumnifiauesbackend.service.audit.AuditQueryService;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping("/access-logs")
    public ResponseEntity<List<AccessLogDTO>> getAccessLogs(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        validateAdminOrDirector(request);
        return ResponseEntity.ok(auditQueryService.findAccessLogs(startDate, endDate));
    }

    @GetMapping("/access-logs/export/csv")
    public ResponseEntity<InputStreamResource> exportAccessLogs(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        validateAdminOrDirector(request);
        List<AccessLogDTO> logs = auditQueryService.findAccessLogs(startDate, endDate);
        ByteArrayInputStream csv = auditQueryService.exportAccessLogsToCsv(logs);

        return ResponseEntity.ok()
                .headers(csvHeaders("access_logs.csv"))
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(csv));
    }

    @GetMapping("/access-logs/by-user/{userId}")
    public ResponseEntity<List<AccessLogDTO>> getByUser(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        validateAdminOrDirector(request);
        return ResponseEntity.ok(auditQueryService.findAccessLogsByUser(userId, startDate, endDate));
    }

    @GetMapping("/access-logs/by-user/{userId}/export/csv")
    public ResponseEntity<InputStreamResource> exportByUser(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        validateAdminOrDirector(request);
        List<AccessLogDTO> logs = auditQueryService.findAccessLogsByUser(userId, startDate, endDate);
        ByteArrayInputStream csv = auditQueryService.exportAccessLogsToCsv(logs);

        return ResponseEntity.ok()
                .headers(csvHeaders("access_logs_user_" + userId + ".csv"))
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(csv));
    }

    private void validateAdminOrDirector(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (user.getUserType() != User.UserType.ADMIN &&
                user.getUserType() != User.UserType.DIRECTOR) {
            throw new BadRequestException("Acceso denegado");
        }
    }

    private HttpHeaders csvHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);
        return headers;
    }
}