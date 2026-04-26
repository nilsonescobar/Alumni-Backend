package org.fia.alumni.alumnifiauesbackend.service.verification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.dto.request.verification.VerificationDecisionRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.verification.ReviewStartResponse;
import org.fia.alumni.alumnifiauesbackend.dto.verification.*;
import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.user.Graduate;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.entity.verification.UserVerification;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.GraduateRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.repository.verification.UserVerificationRepository;
import org.fia.alumni.alumnifiauesbackend.service.activity.ActivityService;
import org.fia.alumni.alumnifiauesbackend.service.audit.AuditService;
import org.fia.alumni.alumnifiauesbackend.service.email.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GraduateRepository graduateRepository;
    private final UserVerificationRepository verificationRepository;
    private final AuditService auditService;
    private final ActivityService activityService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public Page<UserVerificationInfo> getPendingVerifications(Pageable pageable) {
        return verificationRepository.findByStatusIn(
                List.of(UserVerification.VerificationStatus.PENDING,
                        UserVerification.VerificationStatus.IN_REVIEW),
                pageable
        ).map(this::toVerificationInfo);
    }

    @Transactional(readOnly = true)
    public UserVerificationInfo getVerificationDetails(Long userId) {
        UserVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Verificación no encontrada para el usuario"));

        return toVerificationInfo(verification);
    }

    @Transactional
    public ReviewStartResponse startReview(Long userId, Long verifierId) {
        User user = findUser(userId);
        Profile profile = findProfile(userId);

        UserVerification verification = verificationRepository.findByUserId(userId)
                .orElseGet(() -> UserVerification.builder()
                        .userId(userId)
                        .status(UserVerification.VerificationStatus.PENDING)
                        .build());

        if (verification.getStatus() == UserVerification.VerificationStatus.APPROVED) {
            throw new BadRequestException("Este usuario ya está verificado");
        }

        if (verification.getStatus() == UserVerification.VerificationStatus.IN_REVIEW) {
            throw new BadRequestException("Este usuario ya está en proceso de revisión");
        }

        // Buscar el graduado asociado para auto-calcular coincidencias
        Graduate graduate = null;
        ReviewStartResponse.MatchAnalysis matchAnalysis = null;

        if (user.getSourceGraduateId() != null) {
            graduate = graduateRepository.findById(user.getSourceGraduateId()).orElse(null);
        }

        if (graduate != null) {
            matchAnalysis = calculateMatchAnalysis(profile, graduate);
            verification.setNameMatch(matchAnalysis.getNameMatch());
            verification.setStudentIdMatch(matchAnalysis.getStudentIdMatch());
            verification.setDocumentMatch(matchAnalysis.getDocumentMatch());
            verification.setMatchScore(matchAnalysis.getScore());
        }

        verification.setStatus(UserVerification.VerificationStatus.IN_REVIEW);
        verification.setVerifiedBy(verifierId);
        verification.setStartedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        auditService.log(
                verifierId, null,
                AuditLog.AuditAction.VERIFICATION_INITIATED,
                "user_verifications", String.valueOf(userId),
                Map.of("targetUserId", userId),
                null
        );

        activityService.record(userId, "VERIFICATION_PROCESS_STARTED",
                Map.of("verifierId", verifierId), false);

        return ReviewStartResponse.builder()
                .message("Revisión iniciada exitosamente")
                .userData(toVerificationInfo(verification))
                .matchAnalysis(matchAnalysis)
                .build();
    }

    @Transactional
    public String processDecision(VerificationDecisionRequest request, Long verifierId) {
        User user = findUser(request.getUserId());
        Profile profile = findProfile(request.getUserId());

        UserVerification verification = verificationRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró proceso de verificación para este usuario"));

        if (verification.getStatus() != UserVerification.VerificationStatus.IN_REVIEW) {
            throw new BadRequestException("El usuario debe estar en revisión para tomar una decisión");
        }

        // Actualizar coincidencias con lo que el verificador confirmó
        if (request.getNameMatch() != null) verification.setNameMatch(request.getNameMatch());
        if (request.getStudentIdMatch() != null) verification.setStudentIdMatch(request.getStudentIdMatch());
        if (request.getDocumentMatch() != null) verification.setDocumentMatch(request.getDocumentMatch());
        verification.setMatchScore(calculateScore(verification));
        verification.setObservations(request.getObservations());
        verification.setVerifiedBy(verifierId);
        verification.setResolvedAt(LocalDateTime.now());

        if (request.getApproved()) {
            validateApprovalCriteria(verification);

            verification.setStatus(UserVerification.VerificationStatus.APPROVED);
            verificationRepository.save(verification);

            auditService.log(verifierId, null,
                    AuditLog.AuditAction.VERIFICATION_APPROVED,
                    "user_verifications", String.valueOf(request.getUserId()),
                    Map.of("score", verification.getMatchScore(),
                            "observations", request.getObservations() != null ? request.getObservations() : ""),
                    null);

            activityService.record(request.getUserId(), "VERIFICATION_APPROVED",
                    Map.of("observations", request.getObservations() != null ? request.getObservations() : ""), false);

            try {
                emailService.sendVerificationApprovedEmail(
                        user.getEmail(), profile.getFirstName());
            } catch (Exception e) {
                log.warn("Error sending approval email to {}: {}", user.getEmail(), e.getMessage());
            }

            return "Usuario verificado exitosamente";

        } else {
            if (request.getObservations() == null || request.getObservations().isBlank()) {
                throw new BadRequestException("Las observaciones son obligatorias al rechazar");
            }

            verification.setStatus(UserVerification.VerificationStatus.REJECTED);
            verificationRepository.save(verification);

            auditService.log(verifierId, null,
                    AuditLog.AuditAction.VERIFICATION_REJECTED,
                    "user_verifications", String.valueOf(request.getUserId()),
                    Map.of("observations", request.getObservations()),
                    null);

            activityService.record(request.getUserId(), "VERIFICATION_REJECTED",
                    Map.of("observations", request.getObservations()), false);

            try {
                emailService.sendVerificationRejectedEmail(
                        user.getEmail(), profile.getFirstName(), request.getObservations());
            } catch (Exception e) {
                log.warn("Error sending rejection email to {}: {}", user.getEmail(), e.getMessage());
            }

            return "Usuario rechazado";
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private ReviewStartResponse.MatchAnalysis calculateMatchAnalysis(Profile profile, Graduate graduate) {
        boolean nameMatch = areNamesCompatible(
                profile.getFirstName() + " " + profile.getLastName(),
                graduate.getFirstName() + " " + graduate.getLastName()
        );
        boolean studentIdMatch = profile.getStudentId() != null &&
                profile.getStudentId().equals(graduate.getStudentId());
        boolean documentMatch = profile.getIdentityDocument() != null &&
                profile.getIdentityDocument().equals(graduate.getIdentityDocument());

        double similarity = calculateSimilarity(
                profile.getFirstName() + " " + profile.getLastName(),
                graduate.getFirstName() + " " + graduate.getLastName()
        );

        int score = (nameMatch ? 40 : 0) + (studentIdMatch ? 30 : 0) + (documentMatch ? 30 : 0);

        String recommendation;
        int matches = (nameMatch ? 1 : 0) + (studentIdMatch ? 1 : 0) + (documentMatch ? 1 : 0);
        if (matches >= 2) recommendation = "RECOMENDADO PARA APROBACIÓN";
        else if (matches == 1) recommendation = "REQUIERE REVISIÓN MANUAL";
        else recommendation = "NO RECOMENDADO - DATOS NO COINCIDEN";

        return ReviewStartResponse.MatchAnalysis.builder()
                .nameMatch(nameMatch)
                .studentIdMatch(studentIdMatch)
                .documentMatch(documentMatch)
                .score(score)
                .nameSimilarity(similarity)
                .recommendation(recommendation)
                .build();
    }

    private void validateApprovalCriteria(UserVerification verification) {
        boolean hasMinimumMatch = Boolean.TRUE.equals(verification.getNameMatch()) ||
                Boolean.TRUE.equals(verification.getStudentIdMatch()) ||
                Boolean.TRUE.equals(verification.getDocumentMatch());

        if (!hasMinimumMatch) {
            throw new BadRequestException("No se puede aprobar: debe haber al menos una coincidencia válida");
        }
        if (verification.getMatchScore() < 30) {
            throw new BadRequestException("No se puede aprobar: puntuación mínima requerida es 30 (actual: "
                    + verification.getMatchScore() + ")");
        }
    }

    private int calculateScore(UserVerification v) {
        int score = 0;
        if (Boolean.TRUE.equals(v.getNameMatch())) score += 40;
        if (Boolean.TRUE.equals(v.getStudentIdMatch())) score += 30;
        if (Boolean.TRUE.equals(v.getDocumentMatch())) score += 30;
        return score;
    }

    private boolean areNamesCompatible(String name1, String name2) {
        return normalize(name1).equals(normalize(name2));
    }

    private double calculateSimilarity(String name1, String name2) {
        String n1 = normalize(name1);
        String n2 = normalize(name2);
        if (n1.equals(n2)) return 1.0;
        int maxLen = Math.max(n1.length(), n2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - (double) levenshtein(n1, n2) / maxLen;
    }

    private int levenshtein(String a, String b) {
        int[] dp = new int[b.length() + 1];
        for (int i = 0; i <= b.length(); i++) dp[i] = i;
        for (int i = 1; i <= a.length(); i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int temp = dp[j];
                dp[j] = a.charAt(i - 1) == b.charAt(j - 1) ? prev
                        : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
                prev = temp;
            }
        }
        return dp[b.length()];
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ñ","n")
                .trim();
    }

    private UserVerificationInfo toVerificationInfo(UserVerification v) {
        Profile profile = profileRepository.findById(v.getUserId()).orElse(null);
        User user = userRepository.findById(v.getUserId()).orElse(null);

        Graduate graduate = null;
        if (user != null && user.getSourceGraduateId() != null) {
            graduate = graduateRepository.findById(user.getSourceGraduateId()).orElse(null);
        }

        return UserVerificationInfo.builder()
                .userId(v.getUserId())
                .firstName(profile != null ? profile.getFirstName() : null)
                .lastName(profile != null ? profile.getLastName() : null)
                .email(user != null ? user.getEmail() : null)
                .studentId(profile != null ? profile.getStudentId() : null)
                .identityDocument(profile != null ? profile.getIdentityDocument() : null)
                .createdAt(user != null ? user.getCreatedAt() : null)
                .graduateFirstName(graduate != null ? graduate.getFirstName() : null)
                .graduateLastName(graduate != null ? graduate.getLastName() : null)
                .graduateStudentId(graduate != null ? graduate.getStudentId() : null)
                .graduateIdentityDocument(graduate != null ? graduate.getIdentityDocument() : null)
                .graduationYear(graduate != null ? graduate.getGraduationYear() : null)
                .status(v.getStatus().name())
                .nameMatch(v.getNameMatch())
                .studentIdMatch(v.getStudentIdMatch())
                .documentMatch(v.getDocumentMatch())
                .matchScore(v.getMatchScore())
                .observations(v.getObservations())
                .startedAt(v.getStartedAt())
                .resolvedAt(v.getResolvedAt())
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));
    }
}