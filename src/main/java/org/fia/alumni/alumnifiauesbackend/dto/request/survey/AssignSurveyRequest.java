package org.fia.alumni.alumnifiauesbackend.dto.request.survey;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import java.util.List;

@Data
public class AssignSurveyRequest {
    
    @JsonAlias({"assignmentType", "assignment_type"})
    private String assignmentType;

    @JsonAlias({"careerId", "career_id"})
    private Long careerId;

    @JsonAlias({"graduationYearStart", "graduation_year_start"})
    private Integer graduationYearStart;

    @JsonAlias({"graduationYearEnd", "graduation_year_end"})
    private Integer graduationYearEnd;

    @JsonAlias({"userType", "user_type"})
    private String userType;

    @JsonAlias({"userIds", "user_ids"})
    private List<Long> userIds;

    @JsonAlias({"sendNotification", "send_notification"})
    private Boolean sendNotification;
}
