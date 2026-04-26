package org.fia.alumni.alumnifiauesbackend.repository.search;

import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.profile p
        WHERE u.active = true
        AND u.accountDeactivatedAt IS NULL
        AND (
            :query IS NULL OR :query = '' OR
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (:careerId IS NULL OR p.careerId = :careerId)
        AND (:graduationYear IS NULL OR p.graduationYear = :graduationYear)
        AND (:countryId IS NULL OR p.countryId = :countryId)
        AND (:city IS NULL OR :city = '' OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
        AND (:hasDisability IS NULL OR u.hasDisability = :hasDisability)
    """)
    Page<User> searchUsers(
            @Param("query") String query,
            @Param("careerId") Long careerId,
            @Param("graduationYear") Integer graduationYear,
            @Param("countryId") Long countryId,
            @Param("city") String city,
            @Param("hasDisability") Boolean hasDisability,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.profile p
        WHERE u.active = true
        AND u.accountDeactivatedAt IS NULL
        AND p.graduationYear = :year
        ORDER BY p.lastName ASC, p.firstName ASC
    """)
    Page<User> findByGraduationYear(@Param("year") Integer year, Pageable pageable);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.profile p
        WHERE u.active = true
        AND u.accountDeactivatedAt IS NULL
        AND p.careerId = :careerId
        ORDER BY p.graduationYear DESC, p.lastName ASC, p.firstName ASC
    """)
    Page<User> findByCareer(@Param("careerId") Long careerId, Pageable pageable);
}