package org.fia.alumni.alumnifiauesbackend.dto.response.verification;

import lombok.Builder;
import lombok.Data;
import org.fia.alumni.alumnifiauesbackend.dto.verification.UserVerificationInfo;

@Data
@Builder
public class ReviewStartResponse {
    private String message;
    private UserVerificationInfo userData;
    private MatchAnalysis matchAnalysis;

    @Data
    @Builder
    public static class MatchAnalysis {
        private Boolean nameMatch;
        private Boolean studentIdMatch;
        private Boolean documentMatch;
        private Integer score;
        private Double nameSimilarity;
        private String recommendation;
    }
}