package org.fia.alumni.alumnifiauesbackend.repository.job;

import org.fia.alumni.alumnifiauesbackend.entity.job.JobOpportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JobOpportunityRepository extends JpaRepository<JobOpportunity, Long> {

    // Todos los jobs activos (no expirados)
    @Query("SELECT j FROM JobOpportunity j WHERE j.expiresAt IS NULL OR j.expiresAt > :now ORDER BY j.createdAt DESC")
    Page<JobOpportunity> findActiveJobs(LocalDateTime now, Pageable pageable);

    Page<JobOpportunity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}