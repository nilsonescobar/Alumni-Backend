package org.fia.alumni.alumnifiauesbackend.service.search;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.search.UserSearchRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.search.UserAdminListResult;
import org.fia.alumni.alumnifiauesbackend.dto.response.search.UserDetailResult;
import org.fia.alumni.alumnifiauesbackend.dto.response.search.UserSearchListResult;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Career;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Country;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.security.LoginAttempt;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CareerRepository;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CountryRepository;
import org.fia.alumni.alumnifiauesbackend.repository.search.UserSearchRepository;
import org.fia.alumni.alumnifiauesbackend.repository.security.LoginAttemptRepository;
import org.fia.alumni.alumnifiauesbackend.repository.security.UserMfaRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;
    private final UserRepository userRepository;
    private final CareerRepository careerRepository;
    private final CountryRepository countryRepository;
    private final UserMfaRepository userMfaRepository;
    private final LoginAttemptRepository loginAttemptsRepository;

    private static final Set<String> GRADUATE_PUBLIC_FIELDS = Set.of(
            "id", "firstName", "lastName", "fullName", "profilePicture", "careerName",
            "universityName", "graduationYear", "city", "countryName", "displayName"
    );

    private static final Set<String> GRADUATE_PRIVACY_CONTROLLED_FIELDS = Set.of(
            "email", "phone", "bio", "linkedinUrl", "websiteUrl", "gpa"
    );

    @Transactional(readOnly = true)
    public Page<?> searchUsers(UserSearchRequest request, Long currentUserId, String userType, Pageable pageable) {
        Page<User> users = userSearchRepository.searchUsers(
                request.getQuery(),
                request.getCareerId(),
                request.getGraduationYear(),
                request.getCountryId(),
                request.getCity(),
                request.getHasDisability(),
                pageable
        );

        if ("ADMIN".equals(userType)) {
            return users.map(this::mapToAdminListResult);
        }

        return users.map(this::mapToListResult);
    }

    @Transactional(readOnly = true)
    public UserDetailResult getUserById(Long userIdToSearch, Long currentUserId, String userType) {
        User user = userRepository.findById(userIdToSearch)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        boolean isSelf = user.getId().equals(currentUserId);

        return switch (userType) {
            case "ADMIN" -> mapToAdminDetail(user, isSelf);
            case "DIRECTOR" -> mapToDirectorDetail(user, isSelf);
            case "VERIFIER" -> mapToVerifierDetail(user, isSelf);
            default -> mapToGraduateDetail(user, isSelf);
        };
    }

    @Transactional(readOnly = true)
    public Page<?> getUsersByGraduationYear(Integer year, Long currentUserId, String userType, Pageable pageable) {
        Page<User> users = userSearchRepository.findByGraduationYear(year, pageable);

        if ("ADMIN".equals(userType)) {
            return users.map(this::mapToAdminListResult);
        }

        return users.map(this::mapToListResult);
    }

    @Transactional(readOnly = true)
    public Page<?> getUsersByCareer(Long careerId, Long currentUserId, String userType, Pageable pageable) {
        Page<User> users = userSearchRepository.findByCareer(careerId, pageable);

        if ("ADMIN".equals(userType)) {
            return users.map(this::mapToAdminListResult);
        }

        return users.map(this::mapToListResult);
    }

    private UserSearchListResult mapToListResult(User user) {
        Profile profile = user.getProfile();

        return UserSearchListResult.builder()
                .id(user.getId())
                .firstName(profile != null ? profile.getFirstName() : null)
                .lastName(profile != null ? profile.getLastName() : null)
                .profilePicture(profile != null ? profile.getProfilePicture() : null)
                .careerName(profile != null ? getCareerName(profile.getCareerId()) : null)
                .graduationYear(profile != null ? profile.getGraduationYear() : null)
                .city(profile != null ? profile.getCity() : null)
                .countryName(profile != null ? getCountryName(profile.getCountryId()) : null)
                .userType(user.getUserType().name())
                .build();
    }

    private UserAdminListResult mapToAdminListResult(User user) {
        Profile profile = user.getProfile();

        Boolean mfaEnabled = userMfaRepository.findByUserId(user.getId())
                .map(mfa -> mfa.getIsEnabled())
                .orElse(false);

        LoginAttempt loginAttempts = loginAttemptsRepository.findByEmail(user.getEmail())
                .orElse(null);

        return UserAdminListResult.builder()
                .id(user.getId())
                .firstName(profile != null ? profile.getFirstName() : null)
                .lastName(profile != null ? profile.getLastName() : null)
                .email(user.getEmail())
                .profilePicture(profile != null ? profile.getProfilePicture() : null)
                .careerName(profile != null ? getCareerName(profile.getCareerId()) : null)
                .graduationYear(profile != null ? profile.getGraduationYear() : null)
                .city(profile != null ? profile.getCity() : null)
                .countryName(profile != null ? getCountryName(profile.getCountryId()) : null)
                .userType(user.getUserType().name())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .mfaEnabled(mfaEnabled)
                .lastLogin(user.getLastLogin())
                .passwordChangedAt(user.getPasswordChangedAt())
                .passwordMustChange(user.getPasswordMustChange())
                .failedLoginAttempts(loginAttempts != null ? loginAttempts.getFailedAttempts() : 0)
                .lockedUntil(loginAttempts != null ? loginAttempts.getLockedUntil() : null)
                .profileCompletionPercentage(user.getProfileCompletionPercentage())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserDetailResult mapToGraduateDetail(User user, boolean isSelf) {
        Profile profile = user.getProfile();

        UserDetailResult.UserDetailDto userDto = UserDetailResult.UserDetailDto.builder()
                .id(user.getId())
                .userType(user.getUserType().name())
                .build();

        if (profile == null) {
            return UserDetailResult.builder().user(userDto).build();
        }

        UserDetailResult.ProfileDetailDto profileDto = UserDetailResult.ProfileDetailDto.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .fullName(profile.getFullName())
                .displayName(profile.getDisplayName())
                .profilePicture(profile.getProfilePicture())
                .careerName(getCareerName(profile.getCareerId()))
                .universityName(getUniversityName(profile.getCareerId()))
                .graduationYear(profile.getGraduationYear())
                .city(profile.getCity())
                .countryName(getCountryName(profile.getCountryId()))
                .build();

        if (isSelf) {
            profileDto.setEmail(user.getEmail());
            profileDto.setPhone(profile.getPhone());
            profileDto.setBio(profile.getBio());
            profileDto.setLinkedinUrl(profile.getLinkedinUrl());
            profileDto.setWebsiteUrl(profile.getWebsiteUrl());
            profileDto.setGpa(profile.getGraduationGpa() != null ? profile.getGraduationGpa().doubleValue() : null);
            profileDto.setPrivacySettings(profile.getPrivacySettings());
            userDto.setEmail(user.getEmail());
            userDto.setActive(user.getActive());
            userDto.setEmailVerified(user.getEmailVerified());
            userDto.setProfileCompletionPercentage(user.getProfileCompletionPercentage());
        } else {
            applyPrivacySettings(userDto, profileDto, profile.getPrivacySettings(), user.getEmail());
        }

        return UserDetailResult.builder()
                .user(userDto)
                .profile(profileDto)
                .build();
    }

    private UserDetailResult mapToVerifierDetail(User user, boolean isSelf) {
        Profile profile = user.getProfile();

        UserDetailResult.UserDetailDto userDto = UserDetailResult.UserDetailDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .userType(user.getUserType().name())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .hasDisability(user.getHasDisability())
                .disabilityType(user.getDisabilityType() != null ? user.getDisabilityType().name() : null)
                .disabilityDetails(user.getDisabilityDetails())
                .profileCompletionPercentage(user.getProfileCompletionPercentage())
                .build();

        if (profile == null) {
            return UserDetailResult.builder().user(userDto).build();
        }

        UserDetailResult.ProfileDetailDto profileDto = UserDetailResult.ProfileDetailDto.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .fullName(profile.getFullName())
                .displayName(profile.getDisplayName())
                .profilePicture(profile.getProfilePicture())
                .bio(profile.getBio())
                .email(user.getEmail())
                .phone(profile.getPhone())
                .studentId(profile.getStudentId())
                .identityDocument(profile.getIdentityDocument())
                .graduationYear(profile.getGraduationYear())
                .admissionYear(profile.getAdmissionYear())
                .gpa(profile.getGraduationGpa() != null ? profile.getGraduationGpa().doubleValue() : null)
                .careerName(getCareerName(profile.getCareerId()))
                .universityName(getUniversityName(profile.getCareerId()))
                .address(profile.getAddress())
                .city(profile.getCity())
                .countryName(getCountryName(profile.getCountryId()))
                .linkedinUrl(profile.getLinkedinUrl())
                .websiteUrl(profile.getWebsiteUrl())
                .build();

        if (isSelf) {
            profileDto.setPrivacySettings(profile.getPrivacySettings());
        }

        return UserDetailResult.builder()
                .user(userDto)
                .profile(profileDto)
                .build();
    }

    private UserDetailResult mapToDirectorDetail(User user, boolean isSelf) {
        Profile profile = user.getProfile();

        UserDetailResult.UserDetailDto userDto = UserDetailResult.UserDetailDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .userType(user.getUserType().name())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .hasDisability(user.getHasDisability())
                .disabilityType(user.getDisabilityType() != null ? user.getDisabilityType().name() : null)
                .disabilityDetails(user.getDisabilityDetails())
                .profileCompletionPercentage(user.getProfileCompletionPercentage())
                .emailNotificationEnabled(user.getEmailNotificationEnabled())
                .pushNotificationEnabled(user.getPushNotificationEnabled())
                .accountDeactivatedAt(user.getAccountDeactivatedAt())
                .deactivationReason(user.getDeactivationReason())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .registeredWith(user.getRegisteredWith())
                .sourceGraduateId(user.getSourceGraduateId())
                .build();

        if (profile == null) {
            return UserDetailResult.builder().user(userDto).build();
        }

        UserDetailResult.ProfileDetailDto profileDto = UserDetailResult.ProfileDetailDto.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .fullName(profile.getFullName())
                .displayName(profile.getDisplayName())
                .profilePicture(profile.getProfilePicture())
                .bio(profile.getBio())
                .email(user.getEmail())
                .phone(profile.getPhone())
                .studentId(profile.getStudentId())
                .identityDocument(profile.getIdentityDocument())
                .graduationYear(profile.getGraduationYear())
                .admissionYear(profile.getAdmissionYear())
                .gpa(profile.getGraduationGpa() != null ? profile.getGraduationGpa().doubleValue() : null)
                .careerName(getCareerName(profile.getCareerId()))
                .universityName(getUniversityName(profile.getCareerId()))
                .address(profile.getAddress())
                .city(profile.getCity())
                .countryName(getCountryName(profile.getCountryId()))
                .linkedinUrl(profile.getLinkedinUrl())
                .websiteUrl(profile.getWebsiteUrl())
                .build();

        if (isSelf) {
            profileDto.setPrivacySettings(profile.getPrivacySettings());
        }

        return UserDetailResult.builder()
                .user(userDto)
                .profile(profileDto)
                .build();
    }

    private UserDetailResult mapToAdminDetail(User user, boolean isSelf) {
        UserDetailResult directorDetail = mapToDirectorDetail(user, isSelf);

        UserDetailResult.UserDetailDto userDto = directorDetail.getUser();
        userDto.setPasswordChangedAt(user.getPasswordChangedAt());
        userDto.setPasswordMustChange(user.getPasswordMustChange());

        return directorDetail;
    }

    private void applyPrivacySettings(UserDetailResult.UserDetailDto userDto,
                                      UserDetailResult.ProfileDetailDto profileDto,
                                      Map<String, Object> privacySettings,
                                      String userEmail) {
        if (privacySettings == null) {
            return;
        }

        Object visibility = privacySettings.get("profileVisibility");
        if ("PRIVATE".equalsIgnoreCase(String.valueOf(visibility))) {
            return;
        }

        Boolean showEmail = getBooleanSetting(privacySettings, "show_email");
        if (Boolean.TRUE.equals(showEmail)) {
            profileDto.setEmail(userEmail);
        }

        Boolean showPhone = getBooleanSetting(privacySettings, "show_phone");
        if (Boolean.TRUE.equals(showPhone) && profileDto.getPhone() != null) {
        } else {
            profileDto.setPhone(null);
        }

        Boolean showGpa = getBooleanSetting(privacySettings, "show_gpa");
        if (Boolean.TRUE.equals(showGpa) && profileDto.getGpa() != null) {
        } else {
            profileDto.setGpa(null);
        }

        if (profileDto.getBio() != null) {
        } else {
            profileDto.setBio(null);
        }

        if (profileDto.getLinkedinUrl() != null) {
        } else {
            profileDto.setLinkedinUrl(null);
        }

        if (profileDto.getWebsiteUrl() != null) {
        } else {
            profileDto.setWebsiteUrl(null);
        }
    }

    private Boolean getBooleanSetting(Map<String, Object> settings, String key) {
        Object value = settings.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
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
}