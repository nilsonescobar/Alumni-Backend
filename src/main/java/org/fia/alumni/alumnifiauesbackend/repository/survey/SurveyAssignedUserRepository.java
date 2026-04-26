package org.fia.alumni.alumnifiauesbackend.repository.survey;

import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyAssignedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SurveyAssignedUserRepository extends JpaRepository<SurveyAssignedUser, Long> {
    List<SurveyAssignedUser> findByAssignmentId(Long assignmentId);
}