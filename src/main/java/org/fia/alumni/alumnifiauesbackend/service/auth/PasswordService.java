package org.fia.alumni.alumnifiauesbackend.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.entity.security.PasswordResetToken;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.security.PasswordResetTokenRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.service.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public String forgotPassword(String email) {
        log.info("Password reset requested for email: {}", email);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Si existe una cuenta con este correo, recibirás un email con instrucciones"
                ));

        // Invalidate all previous tokens for this user
        tokenRepository.invalidateAllByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Send email
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetUrl);

        log.info("Password reset email sent to: {}", email);

        return "Si existe una cuenta con este correo, recibirás un email con instrucciones para restablecer tu contraseña";
    }

    @Transactional
    public String resetPassword(String token, String newPassword, String confirmPassword) {
        log.info("Password reset attempt with token");

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }

        // Find token
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token inválido o expirado"));

        // Validate token
        if (!resetToken.isValid()) {
            throw new BadRequestException("Token inválido o expirado");
        }

        // Get user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordMustChange(false);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        // Send confirmation email
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getUsername());

        log.info("Password reset successful for user: {}", user.getEmail());

        return "Contraseña restablecida exitosamente. Ya puedes iniciar sesión con tu nueva contraseña";
    }

    @Transactional
    public String changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        log.info("Password change requested for user: {}", userId);

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta");
        }

        // Check if new password is different
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("La nueva contraseña debe ser diferente a la actual");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordMustChange(false);
        userRepository.save(user);

        // Send confirmation email
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getUsername());

        log.info("Password changed successfully for user: {}", userId);

        return "Contraseña cambiada exitosamente";
    }

    public boolean validateResetToken(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Expired password reset tokens cleaned up");
    }
}