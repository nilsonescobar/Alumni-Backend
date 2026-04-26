package org.fia.alumni.alumnifiauesbackend.repository.user;

import org.fia.alumni.alumnifiauesbackend.entity.user.Graduate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GraduateRepository extends JpaRepository<Graduate, Long> {

    Optional<Graduate> findByStudentId(String studentId);

    Optional<Graduate> findByIdentityDocument(String identityDocument);

    @Query("SELECT g FROM Graduate g WHERE g.studentId = :identifier OR g.identityDocument = :identifier")
    Optional<Graduate> findByStudentIdOrIdentityDocument(@Param("identifier") String identifier);
}