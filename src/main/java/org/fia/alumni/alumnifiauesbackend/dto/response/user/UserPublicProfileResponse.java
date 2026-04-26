package org.fia.alumni.alumnifiauesbackend.dto.response.user;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data @Builder
public class UserPublicProfileResponse {
    private Long id;
    private String userType;
    // Siempre públicos
    private String firstName;
    private String lastName;
    private String fullName;
    private String profilePicture;
    private String careerName;
    private String universityName;
    private String city;
    private String countryName;
    // Controlados por privacySettings
    private String email;
    private String phone;
    private Integer graduationYear;
    private Double gpa;
    private String bio;
    private String linkedinUrl;
    private String websiteUrl;
    private Boolean showConnections;
    private Integer connectionCount;
    private Boolean allowConnectionRequests;
    private Boolean isConnection;
    private Boolean mfaEnabled;
}