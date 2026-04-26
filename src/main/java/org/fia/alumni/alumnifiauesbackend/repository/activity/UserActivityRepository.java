package org.fia.alumni.alumnifiauesbackend.repository.activity;

import org.fia.alumni.alumnifiauesbackend.entity.activity.UserActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    List<UserActivity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<UserActivity> findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<UserActivity> findByUserIdOrderByCreatedAtDesc(Long userId);

}