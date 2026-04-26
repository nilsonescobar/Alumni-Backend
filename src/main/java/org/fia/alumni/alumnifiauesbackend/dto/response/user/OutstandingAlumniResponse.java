package org.fia.alumni.alumnifiauesbackend.dto.response.user;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data @Builder
public class OutstandingAlumniResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private String reason;
    private LocalDate recognitionDate;
    private String referenceUrl;
}