package org.fia.alumni.alumnifiauesbackend.repository.user;

import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    boolean existsByEmail(String email);

    boolean existsBySourceGraduateId(Long sourceGraduateId);

    Page<User> findByUserTypeAndActiveTrue(User.UserType userType, Pageable pageable);

    long countByUserTypeAndActiveTrue(User.UserType userType);
}