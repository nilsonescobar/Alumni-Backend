package org.fia.alumni.alumnifiauesbackend.controller.verification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.verification.VerificationDecisionRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.ApiResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.verification.ReviewStartResponse;
import org.fia.alumni.alumnifiauesbackend.dto.verification.UserVerificationInfo;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.security.SecurityUtils;
import org.fia.alumni.alumnifiauesbackend.service.verification.VerificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final UserRepository userRepository;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<UserVerificationInfo>>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validateVerifier();
        Page<UserVerificationInfo> result = verificationService.getPendingVerifications(
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Usuarios pendientes obtenidos", result));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<UserVerificationInfo>> getDetails(
            @PathVariable Long userId) {

        validateVerifier();
        UserVerificationInfo info = verificationService.getVerificationDetails(userId);
        return ResponseEntity.ok(ApiResponse.success("Detalles obtenidos", info));
    }

    @PostMapping("/start-review/{userId}")
    public ResponseEntity<ApiResponse<ReviewStartResponse>> startReview(
            @PathVariable Long userId) {

        validateVerifier();
        Long verifierId = SecurityUtils.getCurrentUserId();
        ReviewStartResponse result = verificationService.startReview(userId, verifierId);
        return ResponseEntity.ok(ApiResponse.success("Revisión iniciada", result));
    }

    @PostMapping("/decide")
    public ResponseEntity<ApiResponse<String>> decide(
            @Valid @RequestBody VerificationDecisionRequest request) {

        validateVerifier();
        Long verifierId = SecurityUtils.getCurrentUserId();
        String result = verificationService.processDecision(request, verifierId);
        return ResponseEntity.ok(ApiResponse.success("Decisión procesada", result));
    }

    private void validateVerifier() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (user.getUserType() != User.UserType.VERIFIER &&
                user.getUserType() != User.UserType.ADMIN) {
            throw new BadRequestException("Acceso denegado — se requiere rol VERIFIER o ADMIN");
        }
    }
}