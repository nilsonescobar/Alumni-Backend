package org.fia.alumni.alumnifiauesbackend.dto.response.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAdminListResult {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePicture;
    private String careerName;
    private Integer graduationYear;
    private String city;
    private String countryName;
    private String userType;

    private Boolean active;
    private Boolean emailVerified;
    private Boolean mfaEnabled;
    private LocalDateTime lastLogin;
    private LocalDateTime passwordChangedAt;
    private Boolean passwordMustChange;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private Integer profileCompletionPercentage;
    private LocalDateTime createdAt;
}