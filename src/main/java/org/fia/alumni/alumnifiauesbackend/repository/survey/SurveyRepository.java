package org.fia.alumni.alumnifiauesbackend.repository.survey;

import org.fia.alumni.alumnifiauesbackend.entity.survey.Survey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    @Query(value = "SELECT DISTINCT s.* FROM surveys s " +
            "CROSS JOIN users u " +
            "LEFT JOIN profiles p ON u.id = p.user_id " +
            "LEFT JOIN survey_assignments sa ON s.id = sa.survey_id " +
            "LEFT JOIN survey_assigned_users sau ON sa.id = sau.assignment_id AND sau.user_id = u.id " +
            "WHERE cast(s.status as text) = 'ACTIVE' " +
            "  AND u.id = :userId " +
            "  AND (s.start_date IS NULL OR s.start_date <= :now) " +
            "  AND (s.end_date IS NULL OR s.end_date >= :now) " +
            "  AND u.active = true " +
            "  AND (" +
            "    cast(sa.assignment_type as text) = 'ALL' " +
            "    OR (cast(sa.assignment_type as text) = 'CAREER' AND sa.career_id = p.career_id) " +
            "    OR (cast(sa.assignment_type as text) = 'GRADUATION_YEAR' AND p.graduation_year BETWEEN sa.graduation_year_start AND sa.graduation_year_end) " +
            "    OR (cast(sa.assignment_type as text) = 'USER_TYPE' AND cast(sa.user_type as text) = cast(u.user_type as text)) " +
            "    OR (cast(sa.assignment_type as text) = 'SPECIFIC_USERS' AND sau.user_id = u.id) " +
            "  )",
            countQuery = "SELECT COUNT(DISTINCT s.id) FROM surveys s " +
                    "CROSS JOIN users u " +
                    "LEFT JOIN profiles p ON u.id = p.user_id " +
                    "LEFT JOIN survey_assignments sa ON s.id = sa.survey_id " +
                    "LEFT JOIN survey_assigned_users sau ON sa.id = sau.assignment_id AND sau.user_id = u.id " +
                    "WHERE cast(s.status as text) = 'ACTIVE' AND u.id = :userId AND (s.start_date IS NULL OR s.start_date <= :now) AND (s.end_date IS NULL OR s.end_date >= :now) AND u.active = true AND (cast(sa.assignment_type as text) = 'ALL' OR (cast(sa.assignment_type as text) = 'CAREER' AND sa.career_id = p.career_id) OR (cast(sa.assignment_type as text) = 'GRADUATION_YEAR' AND p.graduation_year BETWEEN sa.graduation_year_start AND sa.graduation_year_end) OR (cast(sa.assignment_type as text) = 'USER_TYPE' AND cast(sa.user_type as text) = cast(u.user_type as text)) OR (cast(sa.assignment_type as text) = 'SPECIFIC_USERS' AND sau.user_id = u.id))",
            nativeQuery = true)
    Page<Survey> findAvailableForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now, Pageable pageable);

    Page<Survey> findByStatusOrderByCreatedAtDesc(Survey.SurveyStatus status, Pageable pageable);
}