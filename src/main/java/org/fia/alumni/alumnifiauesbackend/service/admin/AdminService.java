package org.fia.alumni.alumnifiauesbackend.service.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.service.audit.AuditService;
import org.fia.alumni.alumnifiauesbackend.service.security.MfaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MfaService mfaService;
    private final AuditService auditService;
    public record UpdateUserTypeRequest(String userType) {}

    @Transactional

    public String updateUserType(Long targetUserId, String newUserType) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        User.UserType parsedType;
        try {
            parsedType = User.UserType.valueOf(newUserType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de usuario inválido: " + newUserType);
        }

        User.UserType previousType = target.getUserType();

        if (previousType == parsedType) {
            throw new BadRequestException("El usuario ya tiene ese rol asignado");
        }

        target.setUserType(parsedType);
        userRepository.save(target);

        auditService.log(
                target.getId(),
                target.getEmail(),
                AuditLog.AuditAction.ADMIN_ROLE_CHANGE,
                "users",
                String.valueOf(targetUserId),
                Map.of(
                        "previousRole", previousType.name(),
                        "newRole", parsedType.name()
                ),
                null
        );

        log.info("User {} role changed from {} to {}", targetUserId, previousType, parsedType);
        return String.format("Rol actualizado de %s a %s para el usuario %d",
                previousType.name(), parsedType.name(), targetUserId);
    }

    @Transactional
    public String disableMfa(Long targetUserId, HttpServletRequest request) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!mfaService.isMfaEnabled(targetUserId)) {
            throw new BadRequestException("El usuario no tiene MFA habilitado");
        }

        mfaService.disableMfa(targetUserId);

        auditService.log(
                target.getId(),
                target.getEmail(),
                AuditLog.AuditAction.ADMIN_MFA_DISABLE,
                "user_mfa",
                String.valueOf(targetUserId),
                Map.of("reason", "Deshabilitado por administrador"),
                auditService.extractIp(request)
        );

        log.info("MFA disabled by admin for user {}", targetUserId);
        return "MFA deshabilitado exitosamente para el usuario " + targetUserId;
    }
}