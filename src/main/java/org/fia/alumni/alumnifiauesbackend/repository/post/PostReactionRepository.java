package org.fia.alumni.alumnifiauesbackend.repository.post;

import org.fia.alumni.alumnifiauesbackend.entity.post.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT pr FROM PostReaction pr WHERE pr.user.id = :userId AND pr.post.id IN :postIds")
    List<PostReaction> findByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") Set<Long> postIds);

    default Map<Long, PostReaction> findUserReactionsForPosts(Long userId, Set<Long> postIds) {
        return findByUserIdAndPostIdIn(userId, postIds).stream()
                .collect(Collectors.toMap(
                        pr -> pr.getPost().getId(),
                        pr -> pr
                ));
    }

    Long countByPostId(Long postId);
}