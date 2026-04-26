package org.fia.alumni.alumnifiauesbackend.repository.survey;

import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);

    Optional<SurveyResponse> findBySurveyIdAndUserId(Long surveyId, Long userId);

    Page<SurveyResponse> findBySurveyId(Long surveyId, Pageable pageable);

    long countBySurveyIdAndStatus(Long surveyId, SurveyResponse.ResponseStatus status);
}