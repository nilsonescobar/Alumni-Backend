package org.fia.alumni.alumnifiauesbackend.repository.security;

import org.fia.alumni.alumnifiauesbackend.entity.security.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    Optional<LoginAttempt> findByEmail(String email);

    @Modifying
    @Query("UPDATE LoginAttempt l SET l.lockedUntil = null WHERE l.lockedUntil < :now")
    void unlockExpiredAccounts(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM LoginAttempt l WHERE l.updatedAt < :cutoff AND l.lockedUntil IS NULL")
    void deleteOldUnlockedAttempts(@Param("cutoff") LocalDateTime cutoff);
}