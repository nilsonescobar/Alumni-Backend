package org.fia.alumni.alumnifiauesbackend.dto.response.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSearchListResult {

    private Long id;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private String careerName;
    private Integer graduationYear;
    private String city;
    private String countryName;
    private String userType;
}