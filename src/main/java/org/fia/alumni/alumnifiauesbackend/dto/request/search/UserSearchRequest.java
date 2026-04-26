package org.fia.alumni.alumnifiauesbackend.dto.request.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {

    private String query;
    private Long careerId;
    private Integer graduationYear;
    private Long countryId;
    private String city;
    private Boolean hasDisability;
}