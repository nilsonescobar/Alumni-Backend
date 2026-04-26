package org.fia.alumni.alumnifiauesbackend.service.user;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.response.user.*;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Career;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Country;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.entity.verification.UserVerification;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CareerRepository;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CountryRepository;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.repository.verification.UserVerificationRepository;
import org.fia.alumni.alumnifiauesbackend.service.security.MfaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserProfileQueryService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CareerRepository careerRepository;
    private final CountryRepository countryRepository;
    private final UserVerificationRepository verificationRepository;
    private final MfaService mfaService;

    // ── Vista pública (graduado viendo a otro graduado) ──────
    @Transactional(readOnly = true)
    public UserPublicProfileResponse getPublicProfile(Long targetUserId) {
        User user = findActiveUser(targetUserId);
        Profile profile = findProfile(targetUserId);
        Map<String, Object> privacy = profile.getPrivacySettings();

        // Verificar visibilidad general
        String visibility = getPrivacySetting(privacy, "profile_visibility", "alumni_only");
        if ("private".equalsIgnoreCase(visibility)) {
            // Solo retorna lo mínimo
            return UserPublicProfileResponse.builder()
                    .id(user.getId())
                    .userType(user.getUserType().name())
                    .firstName(profile.getFirstName())
                    .lastName(profile.getLastName())
                    .fullName(profile.getFullName())
                    .profilePicture(profile.getProfilePicture())
                    .build();
        }

        boolean mfaEnabled = mfaService.isMfaEnabled(targetUserId);
        String careerName = getCareerName(profile.getCareerId());
        String universityName = getUniversityName(profile.getCareerId());
        String countryName = getCountryName(profile.getCountryId());

        return UserPublicProfileResponse.builder()
                .id(user.getId())
                .userType(user.getUserType().name())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .fullName(profile.getFullName())
                .profilePicture(profile.getProfilePicture())
                .careerName(careerName)
                .universityName(universityName)
                .city(profile.getCity())
                .countryName(countryName)
                .mfaEnabled(mfaEnabled)
                // Controlados por privacySettings
                .email(isVisible(privacy, "show_email") ? user.getEmail() : null)
                .phone(isVisible(privacy, "show_phone") ? profile.getPhone() : null)
                .graduationYear(isVisible(privacy, "show_graduation_year") ? profile.getGraduationYear() : null)
                .gpa(isVisible(privacy, "show_gpa") ? toDouble(profile.getGraduationGpa()) : null)
                .bio(profile.getBio())
                .linkedinUrl(profile.getLinkedinUrl())
                .websiteUrl(profile.getWebsiteUrl())
                .showConnections(isVisible(privacy, "show_connections"))
                .allowConnectionRequests(isVisible(privacy, "allow_connection_requests"))
                .isConnection(false) // Se calculará con connections table
                .build();
    }

    // ── Vista admin ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserAdminProfileResponse getAdminProfile(Long targetUserId) {
        User user = findUser(targetUserId);
        Profile profile = findProfile(targetUserId);
        boolean mfaEnabled = mfaService.isMfaEnabled(targetUserId);

        UserVerification verification = verificationRepository
                .findByUserId(targetUserId).orElse(null);

        String careerName = getCareerName(profile.getCareerId());
        String universityName = getUniversityName(profile.getCareerId());
        String countryName = getCountryName(profile.getCountryId());

        return UserAdminProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .userType(user.getUserType().name())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .profileCompletionPercentage(user.getProfileCompletionPercentage())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .studentId(profile.getStudentId())
                .identityDocument(profile.getIdentityDocument())
                .hasDisability(user.getHasDisability())
                .disabilityType(user.getDisabilityType() != null ? user.getDisabilityType().name() : null)
                .disabilityDetails(user.getDisabilityDetails())
                .deactivationReason(user.getDeactivationReason())
                .accountDeactivatedAt(user.getAccountDeactivatedAt())
                .mfaEnabled(mfaEnabled)
                .passwordMustChange(user.getPasswordMustChange())
                .passwordChangedAt(user.getPasswordChangedAt())
                .verificationStatus(verification != null ? verification.getStatus().name() : "NOT_STARTED")
                .matchScore(verification != null ? verification.getMatchScore() : null)
                .verificationObservations(verification != null ? verification.getObservations() : null)
                .profilePicture(profile.getProfilePicture())
                .careerName(careerName)
                .universityName(universityName)
                .graduationYear(profile.getGraduationYear())
                .admissionYear(profile.getAdmissionYear())
                .gpa(toDouble(profile.getGraduationGpa()))
                .bio(profile.getBio())
                .phone(profile.getPhone())
                .city(profile.getCity())
                .countryName(countryName)
                .linkedinUrl(profile.getLinkedinUrl())
                .websiteUrl(profile.getWebsiteUrl())
                .build();
    }

    // ── Vista verificador ────────────────────────────────────
    @Transactional(readOnly = true)
    public UserVerifierProfileResponse getVerifierProfile(Long targetUserId) {
        User user = findUser(targetUserId);
        Profile profile = findProfile(targetUserId);

        UserVerification verification = verificationRepository
                .findByUserId(targetUserId).orElse(null);

        return UserVerifierProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userType(user.getUserType().name())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .studentId(profile.getStudentId())
                .identityDocument(profile.getIdentityDocument())
                .profilePicture(profile.getProfilePicture())
                .careerName(getCareerName(profile.getCareerId()))
                .universityName(getUniversityName(profile.getCareerId()))
                .graduationYear(profile.getGraduationYear())
                .admissionYear(profile.getAdmissionYear())
                .gpa(toDouble(profile.getGraduationGpa()))
                .verificationStatus(verification != null ? verification.getStatus().name() : "NOT_STARTED")
                .matchScore(verification != null ? verification.getMatchScore() : null)
                .verificationObservations(verification != null ? verification.getObservations() : null)
                .verificationStartedAt(verification != null ? verification.getStartedAt() : null)
                .verificationResolvedAt(verification != null ? verification.getResolvedAt() : null)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────
    private boolean isVisible(Map<String, Object> privacy, String key) {
        if (privacy == null) return false;
        Object val = privacy.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);
        return false;
    }

    private String getPrivacySetting(Map<String, Object> privacy, String key, String defaultVal) {
        if (privacy == null) return defaultVal;
        Object val = privacy.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private Double toDouble(java.math.BigDecimal bd) {
        return bd != null ? bd.doubleValue() : null;
    }

    private String getCareerName(Long careerId) {
        if (careerId == null) return null;
        return careerRepository.findById(careerId).map(Career::getName).orElse(null);
    }

    private String getUniversityName(Long careerId) {
        if (careerId == null) return null;
        return careerRepository.findById(careerId)
                .filter(c -> c.getUniversity() != null)
                .map(c -> c.getUniversity().getName())
                .orElse(null);
    }

    private String getCountryName(Long countryId) {
        if (countryId == null) return null;
        return countryRepository.findById(countryId).map(Country::getName).orElse(null);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private User findActiveUser(Long userId) {
        User user = findUser(userId);
        if (!user.isActive()) throw new BadRequestException("Este usuario no está disponible");
        return user;
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));
    }
}