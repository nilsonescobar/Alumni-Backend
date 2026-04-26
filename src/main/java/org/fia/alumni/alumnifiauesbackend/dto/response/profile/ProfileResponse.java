package org.fia.alumni.alumnifiauesbackend.dto.response.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String email;
    private String username;
    private User.UserType userType;
    private Boolean active;
    private Boolean emailVerified;

    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String profilePicture;
    private String bio;

    private String studentId;
    private String identityDocument;
    private Integer graduationYear;
    private BigDecimal graduationGpa;

    private CareerInfo career;
    private CountryInfo country;

    private String linkedinUrl;
    private String websiteUrl;
    private String address;
    private String city;

    private Integer profileCompletionPercentage;
    private Map<String, Object> privacySettings;

    private Boolean emailNotificationEnabled;
    private Boolean pushNotificationEnabled;

    private Boolean hasDisability;
    private User.DisabilityType disabilityType;
    private String disabilityDetails;

    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareerInfo {
        private Long id;
        private String name;
        private String code;
        private UniversityInfo university;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniversityInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryInfo {
        private Long id;
        private String name;
        private String isoCode;
    }
}