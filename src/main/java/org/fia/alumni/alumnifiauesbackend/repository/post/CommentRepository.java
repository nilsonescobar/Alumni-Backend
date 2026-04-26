package org.fia.alumni.alumnifiauesbackend.repository.post;

import org.fia.alumni.alumnifiauesbackend.entity.post.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.post.id = :postId " +
            "AND c.deletedAt IS NULL " +
            "ORDER BY c.createdAt ASC")
    Page<Comment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    Long countByPostIdAndDeletedAtIsNull(Long postId);
}