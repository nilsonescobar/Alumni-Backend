package org.fia.alumni.alumnifiauesbackend.repository.catalog;

import org.fia.alumni.alumnifiauesbackend.entity.catalog.Career;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerRepository extends JpaRepository<Career, Long> {

    List<Career> findByUniversityId(Long universityId);
}