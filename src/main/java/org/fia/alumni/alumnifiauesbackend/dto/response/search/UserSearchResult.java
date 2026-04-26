package org.fia.alumni.alumnifiauesbackend.dto.response.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSearchResult {

    private UserSummaryDto user;
    private ProfileSummaryDto profile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String email;
        private Boolean active;
        private Boolean hasDisability;
        private String userType;
        private Boolean mfaEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileSummaryDto {
        private String firstName;
        private String lastName;
        private String fullName;
        private String profilePicture;
        private String bio;
        private Double gpa;

        private Integer graduationYear;
        private String careerName;
        private String universityName;
        private String city;
        private String countryName;
        private String phone;
        private String address;
        private String linkedinUrl;
        private String websiteUrl;
        private String studentId;
        private String identityDocument;
        private Map<String, Object> privacySettings;
        private Integer profileCompletionPercentage;
        private Integer connectionCount;
        private Boolean isConnection;
        private Boolean connectionRequestPending;
    }
}