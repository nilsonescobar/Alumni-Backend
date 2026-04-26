package org.fia.alumni.alumnifiauesbackend.repository.security;

import org.fia.alumni.alumnifiauesbackend.entity.security.UserMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMfaRepository extends JpaRepository<UserMfa, Long> {
    Optional<UserMfa> findByUserId(Long userId);
    boolean existsByUserIdAndIsEnabledTrue(Long userId);

}