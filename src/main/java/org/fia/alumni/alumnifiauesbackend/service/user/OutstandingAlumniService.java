package org.fia.alumni.alumnifiauesbackend.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.dto.response.user.OutstandingAlumniResponse;
import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.fia.alumni.alumnifiauesbackend.entity.outstanding.OutstandingAlumni;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.outstanding.OutstandingAlumniRepository;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.service.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutstandingAlumniService {

    private static final int MAX_OUTSTANDING = 3;

    private final OutstandingAlumniRepository outstandingAlumniRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<OutstandingAlumniResponse> getAll() {
        return outstandingAlumniRepository.findAllWithProfile()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OutstandingAlumniResponse highlight(Long targetUserId, String reason,
                                               String referenceUrl, Long adminId) {
        User admin = findUser(adminId);
        User target = findUser(targetUserId);

        if (target.getUserType() != User.UserType.GRADUATE) {
            throw new BadRequestException("Solo se pueden destacar graduados");
        }

        if (outstandingAlumniRepository.existsByUserId(targetUserId)) {
            throw new BadRequestException("Este alumno ya está destacado");
        }

        long current = outstandingAlumniRepository.count();
        if (current >= MAX_OUTSTANDING) {
            throw new BadRequestException(
                    "Ya hay " + MAX_OUTSTANDING + " alumnos destacados. " +
                            "Elimina uno antes de agregar otro.");
        }

        OutstandingAlumni outstanding = OutstandingAlumni.builder()
                .userId(targetUserId)
                .reason(reason)
                .recognitionDate(LocalDate.now())
                .awardedByUserId(adminId)
                .referenceUrl(referenceUrl)
                .build();

        outstandingAlumniRepository.save(outstanding);

        auditService.log(adminId, admin.getEmail(),
                AuditLog.AuditAction.UPDATE,
                "outstanding_alumni", String.valueOf(targetUserId),
                Map.of("action", "highlighted", "targetUser", target.getEmail(), "reason", reason),
                null);

        log.info("Admin {} highlighted user {} as outstanding alumni", adminId, targetUserId);
        return toResponse(outstanding);
    }

    @Transactional
    public String remove(Long targetUserId, Long adminId) {
        User admin = findUser(adminId);

        OutstandingAlumni outstanding = outstandingAlumniRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Este alumno no está destacado"));

        outstandingAlumniRepository.delete(outstanding);

        auditService.log(adminId, admin.getEmail(),
                AuditLog.AuditAction.DELETE,
                "outstanding_alumni", String.valueOf(targetUserId),
                Map.of("action", "removed"),
                null);

        log.info("Admin {} removed user {} from outstanding alumni", adminId, targetUserId);
        return "Alumno removido de destacados";
    }

    private OutstandingAlumniResponse toResponse(OutstandingAlumni o) {
        Profile profile = profileRepository.findById(o.getUserId()).orElse(null);
        return OutstandingAlumniResponse.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .firstName(profile != null ? profile.getFirstName() : null)
                .lastName(profile != null ? profile.getLastName() : null)
                .profilePicture(profile != null ? profile.getProfilePicture() : null)
                .reason(o.getReason())
                .recognitionDate(o.getRecognitionDate())
                .referenceUrl(o.getReferenceUrl())
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}