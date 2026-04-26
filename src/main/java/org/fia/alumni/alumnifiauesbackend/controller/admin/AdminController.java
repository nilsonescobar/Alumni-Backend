package org.fia.alumni.alumnifiauesbackend.controller.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.audit.TimelineEventDTO;
import org.fia.alumni.alumnifiauesbackend.dto.response.ApiResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.user.OutstandingAlumniResponse;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.security.SecurityUtils;
import org.fia.alumni.alumnifiauesbackend.service.admin.AdminService;
import org.fia.alumni.alumnifiauesbackend.service.admin.AdminTimelineService;
import org.fia.alumni.alumnifiauesbackend.service.user.AdminUserActionService;
import org.fia.alumni.alumnifiauesbackend.service.user.OutstandingAlumniService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminTimelineService adminTimelineService;
    private final AdminUserActionService adminUserActionService;
    private final OutstandingAlumniService outstandingAlumniService;
    private final UserRepository userRepository;

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<String>> updateUserType(
            @PathVariable Long userId,
            @RequestBody UpdateUserTypeRequest request) {
        validateAdmin();
        String result = adminService.updateUserType(userId, request.getUserType());
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/users/{userId}/disable-mfa")
    public ResponseEntity<ApiResponse<String>> disableMfa(@PathVariable Long userId) {
        validateAdmin();
        Long adminId = SecurityUtils.getCurrentUserId();
        String result = adminUserActionService.disableUserMfa(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/users/{userId}/password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable Long userId,
            @RequestBody ResetPasswordRequest request) {
        validateAdmin();
        Long adminId = SecurityUtils.getCurrentUserId();
        String result = adminUserActionService.resetUserPassword(userId, request.getNewPassword(), adminId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivate(
            @PathVariable Long userId,
            @RequestBody DeactivateRequest request) {
        validateAdmin();
        Long adminId = SecurityUtils.getCurrentUserId();
        String result = adminUserActionService.deactivateUser(userId, request.getReason(), adminId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/users/{userId}/reactivate")
    public ResponseEntity<ApiResponse<String>> reactivate(@PathVariable Long userId) {
        validateAdmin();
        Long adminId = SecurityUtils.getCurrentUserId();
        String result = adminUserActionService.reactivateUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/outstanding/{userId}")
    public ResponseEntity<ApiResponse<OutstandingAlumniResponse>> highlight(
            @PathVariable Long userId,
            @RequestBody HighlightRequest request) {
        validateAdminOrDirector();
        Long adminId = SecurityUtils.getCurrentUserId();
        OutstandingAlumniResponse result = outstandingAlumniService.highlight(
                userId, request.getReason(), request.getReferenceUrl(), adminId);
        return ResponseEntity.ok(ApiResponse.success("Alumno destacado exitosamente", result));
    }

    @DeleteMapping("/outstanding/{userId}")
    public ResponseEntity<ApiResponse<String>> removeHighlight(@PathVariable Long userId) {
        validateAdminOrDirector();
        Long adminId = SecurityUtils.getCurrentUserId();
        String result = outstandingAlumniService.remove(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @GetMapping("/users/{userId}/timeline")
    public ResponseEntity<ApiResponse<List<TimelineEventDTO>>> getUserTimeline(
            @PathVariable Long userId) {
        validateAdmin();
        List<TimelineEventDTO> timeline = adminTimelineService.getComprehensiveTimelineForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Timeline obtenido para usuario " + userId, timeline));
    }

    private void validateAdmin() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (user.getUserType() != User.UserType.ADMIN) {
            throw new BadRequestException("Acceso denegado — se requiere rol ADMIN");
        }
    }

    private void validateAdminOrDirector() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (user.getUserType() != User.UserType.ADMIN && user.getUserType() != User.UserType.DIRECTOR) {
            throw new BadRequestException("Acceso denegado");
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateUserTypeRequest {
        private String userType;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank
        private String newPassword;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DeactivateRequest {
        @NotBlank
        private String reason;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class HighlightRequest {
        @NotBlank
        private String reason;
        private String referenceUrl;
    }
}