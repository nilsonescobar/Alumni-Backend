package org.fia.alumni.alumnifiauesbackend.service.profile;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.profile.*;
import org.fia.alumni.alumnifiauesbackend.dto.response.profile.ProfileResponse;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Career;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Country;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CareerRepository;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CountryRepository;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CareerRepository careerRepository;
    private final CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        return mapToResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getPublicProfile(Long userId) {
        User user = findUserById(userId);

        if (!user.isActive()) {
            throw new BadRequestException("Este usuario no está disponible");
        }

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        return mapToResponse(user, profile);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }

        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        if (request.getLinkedinUrl() != null) {
            profile.setLinkedinUrl(request.getLinkedinUrl());
        }

        if (request.getWebsiteUrl() != null) {
            profile.setWebsiteUrl(request.getWebsiteUrl());
        }

        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }

        if (request.getCity() != null) {
            profile.setCity(request.getCity());
        }

        if (request.getCountryId() != null) {
            if (!countryRepository.existsById(request.getCountryId())) {
                throw new BadRequestException("País no encontrado");
            }
            profile.setCountryId(request.getCountryId());
        }

        if (request.getCareerId() != null) {
            if (!careerRepository.existsById(request.getCareerId())) {
                throw new BadRequestException("Carrera no encontrada");
            }
            profile.setCareerId(request.getCareerId());
        }

        Profile updatedProfile = profileRepository.save(profile);
        updateProfileCompletionPercentage(user, updatedProfile);

        return mapToResponse(user, updatedProfile);
    }

    @Transactional
    public String updateProfilePicture(Long userId, String pictureUrl) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        profile.setProfilePicture(pictureUrl);
        profileRepository.save(profile);

        return pictureUrl;
    }

    @Transactional
    public Map<String, Object> updatePrivacySettings(Long userId, UpdatePrivacySettingsRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        Map<String, Object> privacySettings = new HashMap<>();
        privacySettings.put("show_email", request.getShowEmail());
        privacySettings.put("show_phone", request.getShowPhone());
        privacySettings.put("show_current_job", request.getShowCurrentJob());
        privacySettings.put("show_graduation_year", request.getShowGraduationYear());
        privacySettings.put("show_gpa", request.getShowGpa());
        privacySettings.put("profile_visibility", request.getProfileVisibility());
        privacySettings.put("show_connections", request.getShowConnections());
        privacySettings.put("allow_connection_requests", request.getAllowConnectionRequests());

        profile.setPrivacySettings(privacySettings);
        profileRepository.save(profile);

        return privacySettings;
    }

    @Transactional
    public void updateNotificationSettings(Long userId, UpdateNotificationSettingsRequest request) {
        User user = findUserById(userId);

        user.setEmailNotificationEnabled(request.getEmailNotificationEnabled());
        user.setPushNotificationEnabled(request.getPushNotificationEnabled());

        userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(Long userId, DeactivateAccountRequest request) {
        User user = findUserById(userId);

        if (!user.isActive()) {
            throw new BadRequestException("La cuenta ya está desactivada");
        }

        user.deactivate(request.getReason());
        userRepository.save(user);
    }

    @Transactional
    public void reactivateAccount(Long userId) {
        User user = findUserById(userId);

        if (user.isActive()) {
            throw new BadRequestException("La cuenta ya está activa");
        }

        user.reactivate();
        userRepository.save(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private void updateProfileCompletionPercentage(User user, Profile profile) {
        int totalFields = 15;
        int completedFields = 0;

        if (profile.getPhone() != null && !profile.getPhone().isBlank()) completedFields++;
        if (profile.getProfilePicture() != null && !profile.getProfilePicture().isBlank()) completedFields++;
        if (profile.getBio() != null && !profile.getBio().isBlank()) completedFields++;
        if (profile.getStudentId() != null && !profile.getStudentId().isBlank()) completedFields++;
        if (profile.getIdentityDocument() != null && !profile.getIdentityDocument().isBlank()) completedFields++;
        if (profile.getGraduationYear() != null) completedFields++;
        if (profile.getGraduationGpa() != null) completedFields++;
        if (profile.getCareerId() != null) completedFields++;
        if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) completedFields++;
        if (profile.getWebsiteUrl() != null && !profile.getWebsiteUrl().isBlank()) completedFields++;
        if (profile.getAddress() != null && !profile.getAddress().isBlank()) completedFields++;
        if (profile.getCity() != null && !profile.getCity().isBlank()) completedFields++;
        if (profile.getCountryId() != null) completedFields++;
        if (user.getEmailVerified()) completedFields++;
        completedFields++;

        int percentage = (completedFields * 100) / totalFields;
        user.setProfileCompletionPercentage(percentage);
        userRepository.save(user);
    }

    private ProfileResponse mapToResponse(User user, Profile profile) {
        ProfileResponse.CareerInfo careerInfo = null;
        if (profile.getCareerId() != null) {
            Career career = careerRepository.findById(profile.getCareerId()).orElse(null);
            if (career != null) {
                careerInfo = ProfileResponse.CareerInfo.builder()
                        .id(career.getId())
                        .name(career.getName())
                        .code(career.getCode())
                        .build();
            }
        }

        ProfileResponse.CountryInfo countryInfo = null;
        if (profile.getCountryId() != null) {
            Country country = countryRepository.findById(profile.getCountryId()).orElse(null);
            if (country != null) {
                countryInfo = ProfileResponse.CountryInfo.builder()
                        .id(country.getId())
                        .name(country.getName())
                        .isoCode(country.getIsoCode())
                        .build();
            }
        }

        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .userType(user.getUserType())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .profilePicture(profile.getProfilePicture())
                .bio(profile.getBio())
                .studentId(profile.getStudentId())
                .identityDocument(profile.getIdentityDocument())
                .graduationYear(profile.getGraduationYear())
                .graduationGpa(profile.getGraduationGpa())
                .career(careerInfo)
                .country(countryInfo)
                .linkedinUrl(profile.getLinkedinUrl())
                .websiteUrl(profile.getWebsiteUrl())
                .address(profile.getAddress())
                .city(profile.getCity())
                .profileCompletionPercentage(user.getProfileCompletionPercentage())
                .privacySettings(profile.getPrivacySettings())
                .emailNotificationEnabled(user.getEmailNotificationEnabled())
                .pushNotificationEnabled(user.getPushNotificationEnabled())
                .hasDisability(user.getHasDisability())
                .disabilityType(user.getDisabilityType())
                .disabilityDetails(user.getDisabilityDetails())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}