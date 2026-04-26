package org.fia.alumni.alumnifiauesbackend.repository.common;

import org.fia.alumni.alumnifiauesbackend.entity.common.FeaturedAlumni;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeaturedAlumniRepository extends JpaRepository<FeaturedAlumni, Long> {
    List<FeaturedAlumni> findAllByOrderByRecognitionDateDesc();
    boolean existsByUserId(Long userId);
}