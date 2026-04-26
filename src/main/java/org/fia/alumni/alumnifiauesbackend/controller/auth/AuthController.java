package org.fia.alumni.alumnifiauesbackend.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.dto.request.auth.*;
import org.fia.alumni.alumnifiauesbackend.dto.response.auth.LoginResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.auth.RegisterResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.search.UserSearchResult;
import org.fia.alumni.alumnifiauesbackend.service.auth.AuthService;
import org.fia.alumni.alumnifiauesbackend.service.auth.AuthenticationService;
import org.fia.alumni.alumnifiauesbackend.service.auth.PasswordService;
import org.fia.alumni.alumnifiauesbackend.service.security.MfaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationService authenticationService;
    private final PasswordService passwordService;
    private final MfaService mfaService;

    // ==================== REGISTRO ====================

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());

        RegisterResponse response = authService.register(request);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        log.info("Email verification request received");

        String message = authService.verifyEmail(token);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    // ==================== LOGIN ====================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Login request received for email: {}", request.getEmail());

        LoginResponse response = authenticationService.login(request, httpRequest);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", response);

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        LoginResponse response = authenticationService.refreshToken(refreshToken);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", response);

        return ResponseEntity.ok(responseBody);
    }

    // Alias: el frontend llama /auth/refresh-token
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshTokenAlias(@RequestBody Map<String, String> request) {
        return refreshToken(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request
    ) {
        String accessToken = authHeader.substring(7); // Remove "Bearer "
        String refreshToken = request.get("refreshToken");

        authenticationService.logout(accessToken, refreshToken);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sesión cerrada exitosamente");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutAll(
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String accessToken = authHeader.substring(7); // Remove "Bearer "

        authenticationService.logoutAll(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Todas las sesiones han sido cerradas");

        return ResponseEntity.ok(response);
    }

    // ==================== PASSWORD RESET ====================

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        log.info("Forgot password request for email: {}", request.getEmail());

        String message = passwordService.forgotPassword(request.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        log.info("Reset password request received");

        String message = passwordService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        String message = passwordService.changePassword(
                userId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordService.validateResetToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("valid", isValid);

        return ResponseEntity.ok(response);
    }

    // ==================== MFA ====================

    @PostMapping("/mfa/enable")
    public ResponseEntity<Map<String, Object>> enableMfa(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Map<String, Object> mfaData = mfaService.enableMfa(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mfaData);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<Map<String, Object>> verifyMfa(
            @Valid @RequestBody VerifyMfaRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        boolean verified = mfaService.verifyAndEnableMfa(userId, request.getCode());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "MFA habilitado exitosamente");
        response.put("verified", verified);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Map<String, Object>> disableMfa(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        mfaService.disableMfa(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "MFA deshabilitado exitosamente");

        return ResponseEntity.ok(response);
    }


    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        UserSearchResult userProfile = authenticationService.getCurrentUser(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userProfile);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/validate-login")
    public ResponseEntity<Map<String, Object>> validateLoginMfa(
            @Valid @RequestBody VerifyMfaRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        boolean isCodeValid = mfaService.verifyMfaCode(userId, request.getCode());

        if (!isCodeValid) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("status", "INVALID_MFA_CODE");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        LoginResponse response = authenticationService.completeMfaLogin(userId);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("status", "SUCCESS");
        responseBody.put("data", response);

        return ResponseEntity.ok(responseBody);
    }


}