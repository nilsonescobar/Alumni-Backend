package org.fia.alumni.alumnifiauesbackend.repository.post;

import org.fia.alumni.alumnifiauesbackend.entity.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.media " +
            "WHERE p.deletedAt IS NULL " +
            "AND p.isPublic = true " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findPublicFeed(Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.media " +
            "WHERE p.user.id = :userId " +
            "AND p.deletedAt IS NULL " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.media " +
            "WHERE p.id = :postId " +
            "AND p.deletedAt IS NULL")
    Optional<Post> findByIdWithDetails(@Param("postId") Long postId);

    @Query("SELECT COUNT(pr) FROM PostReaction pr WHERE pr.post.id = :postId")
    Long countReactionsByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.deletedAt IS NULL")
    Long countCommentsByPostId(@Param("postId") Long postId);
}