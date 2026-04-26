package org.fia.alumni.alumnifiauesbackend.dto.response.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.post.PostReaction;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String content;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserSummary user;
    private List<MediaInfo> media;
    private List<String> tags;

    private Long reactionCount;
    private Long commentCount;
    private PostReaction.ReactionType currentUserReaction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String profilePicture;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaInfo {
        private Long id;
        private String url;
        private String type;
        private Integer displayOrder;
    }
}