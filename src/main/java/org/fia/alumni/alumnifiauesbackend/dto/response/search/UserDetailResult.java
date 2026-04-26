package org.fia.alumni.alumnifiauesbackend.dto.response.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailResult {

    private UserDetailDto user;
    private ProfileDetailDto profile;
    private VerificationDetailDto verification;
    private SecurityDetailDto security;
    private StatisticsDto statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDetailDto {
        private Long id;
        private String username;
        private String email;
        private String userType;
        private Boolean active;
        private Boolean emailVerified;
        private Boolean hasDisability;
        private String disabilityType;
        private String disabilityDetails;
        private Integer profileCompletionPercentage;
        private Boolean emailNotificationEnabled;
        private Boolean pushNotificationEnabled;
        private LocalDateTime accountDeactivatedAt;
        private String deactivationReason;
        private LocalDateTime lastLogin;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String registeredWith;
        private Long sourceGraduateId;
        private LocalDateTime passwordChangedAt;
        private Boolean passwordMustChange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileDetailDto {
        private String firstName;
        private String lastName;
        private String fullName;
        private String displayName;
        private String profilePicture;
        private String bio;
        private String phone;
        private String email;
        private String studentId;
        private String identityDocument;
        private Integer graduationYear;
        private Integer admissionYear;
        private Double gpa;
        private String careerName;
        private String universityName;
        private String address;
        private String city;
        private String countryName;
        private String linkedinUrl;
        private String websiteUrl;
        private Map<String, Object> privacySettings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VerificationDetailDto {
        private String status;
        private Long verifiedBy;
        private Boolean nameMatch;
        private Boolean studentIdMatch;
        private Boolean documentMatch;
        private Integer matchScore;
        private String observations;
        private LocalDateTime startedAt;
        private LocalDateTime resolvedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SecurityDetailDto {
        private Boolean mfaEnabled;
        private LocalDateTime mfaEnabledAt;
        private LocalDateTime passwordChangedAt;
        private Boolean passwordMustChange;
        private Integer failedLoginAttempts;
        private LocalDateTime lastFailedLoginAttempt;
        private LocalDateTime lockedUntil;
        private LocalDateTime lastSuccessfulLogin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatisticsDto {
        private Integer connectionCount;
        private Integer followerCount;
        private Integer followingCount;
        private Integer totalPosts;
        private Integer totalComments;
        private Integer totalReactions;
        private Integer eventsAttended;
        private Integer skillsCount;
        private Integer endorsementsReceived;
        private Integer recommendationsReceived;
        private Integer workExperienceCount;
        private Integer postgraduateStudiesCount;
        private Integer certificationsCount;
        private Boolean isConnection;
        private String connectionStatus;
        private Boolean isFollowing;
        private Boolean isFollower;
    }
}