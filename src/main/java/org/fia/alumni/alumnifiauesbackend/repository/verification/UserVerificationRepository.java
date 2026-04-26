package org.fia.alumni.alumnifiauesbackend.repository.verification;

import org.fia.alumni.alumnifiauesbackend.entity.verification.UserVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVerificationRepository extends JpaRepository<UserVerification, Long> {

    Optional<UserVerification> findByUserId(Long userId);

    @Query("""
        SELECT v FROM UserVerification v
        WHERE v.status IN :statuses
        ORDER BY v.createdAt ASC
    """)
    Page<UserVerification> findByStatusIn(
            @Param("statuses") List<UserVerification.VerificationStatus> statuses,
            Pageable pageable
    );

    boolean existsByUserIdAndStatus(Long userId, UserVerification.VerificationStatus status);
}