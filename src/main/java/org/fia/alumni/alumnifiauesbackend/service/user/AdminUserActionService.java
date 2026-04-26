package org.fia.alumni.alumnifiauesbackend.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.service.audit.AuditService;
import org.fia.alumni.alumnifiauesbackend.service.security.MfaService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserActionService {

    private final UserRepository userRepository;
    private final MfaService mfaService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String resetUserPassword(Long targetUserId, String newPassword, Long adminId) {
        User target = findUser(targetUserId);
        User admin = findUser(adminId);

        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("La contraseña debe tener al menos 8 caracteres");
        }

        target.setPassword(passwordEncoder.encode(newPassword));
        target.setPasswordMustChange(true); // Forzar cambio en próximo login
        userRepository.save(target);

        auditService.log(adminId, admin.getEmail(),
                AuditLog.AuditAction.PASSWORD_CHANGE,
                "users", String.valueOf(targetUserId),
                Map.of("action", "admin_password_reset", "targetUser", target.getEmail()),
                null);

        log.info("Admin {} reset password for user {}", adminId, targetUserId);
        return "Contraseña restablecida. El usuario deberá cambiarla en su próximo login.";
    }

    @Transactional
    public String disableUserMfa(Long targetUserId, Long adminId) {
        User admin = findUser(adminId);

        if (!mfaService.isMfaEnabled(targetUserId)) {
            throw new BadRequestException("El usuario no tiene MFA habilitado");
        }

        mfaService.disableMfa(targetUserId);

        auditService.log(adminId, admin.getEmail(),
                AuditLog.AuditAction.ADMIN_MFA_DISABLE,
                "user_mfa", String.valueOf(targetUserId),
                Map.of("reason", "Deshabilitado por administrador"),
                null);

        log.info("Admin {} disabled MFA for user {}", adminId, targetUserId);
        return "MFA deshabilitado exitosamente";
    }

    @Transactional
    public String deactivateUser(Long targetUserId, String reason, Long adminId) {
        User target = findUser(targetUserId);
        User admin = findUser(adminId);

        if (targetUserId.equals(adminId)) {
            throw new BadRequestException("No puedes desactivar tu propia cuenta");
        }

        if (!target.isActive()) {
            throw new BadRequestException("La cuenta ya está desactivada");
        }

        target.deactivate(reason);
        userRepository.save(target);

        auditService.log(adminId, admin.getEmail(),
                AuditLog.AuditAction.ACCOUNT_DEACTIVATED,
                "users", String.valueOf(targetUserId),
                Map.of("reason", reason, "targetUser", target.getEmail()),
                null);

        log.info("Admin {} deactivated user {}", adminId, targetUserId);
        return "Cuenta desactivada exitosamente";
    }

    @Transactional
    public String reactivateUser(Long targetUserId, Long adminId) {
        User target = findUser(targetUserId);
        User admin = findUser(adminId);

        if (target.isActive()) {
            throw new BadRequestException("La cuenta ya está activa");
        }

        target.reactivate();
        userRepository.save(target);

        auditService.log(adminId, admin.getEmail(),
                AuditLog.AuditAction.UPDATE,
                "users", String.valueOf(targetUserId),
                Map.of("action", "reactivate", "targetUser", target.getEmail()),
                null);

        log.info("Admin {} reactivated user {}", adminId, targetUserId);
        return "Cuenta reactivada exitosamente";
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}