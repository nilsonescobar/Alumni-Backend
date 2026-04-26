package org.fia.alumni.alumnifiauesbackend.repository.survey;

import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyResponseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SurveyResponseDetailRepository extends JpaRepository<SurveyResponseDetail, Long> {
    List<SurveyResponseDetail> findBySurveyId(Long surveyId);
    void deleteByResponseId(Long responseId);
}