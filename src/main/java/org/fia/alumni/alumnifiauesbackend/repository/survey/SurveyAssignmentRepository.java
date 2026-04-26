package org.fia.alumni.alumnifiauesbackend.repository.survey;

import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SurveyAssignmentRepository extends JpaRepository<SurveyAssignment, Long> {
    List<SurveyAssignment> findBySurveyId(Long surveyId);
}