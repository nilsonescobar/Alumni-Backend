package org.fia.alumni.alumnifiauesbackend.repository.outstanding;

import org.fia.alumni.alumnifiauesbackend.entity.outstanding.OutstandingAlumni;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutstandingAlumniRepository extends JpaRepository<OutstandingAlumni, Long> {

    boolean existsByUserId(Long userId);

    Optional<OutstandingAlumni> findByUserId(Long userId);

    @Query("SELECT o FROM OutstandingAlumni o ORDER BY o.recognitionDate DESC")
    List<OutstandingAlumni> findAllWithProfile();
}