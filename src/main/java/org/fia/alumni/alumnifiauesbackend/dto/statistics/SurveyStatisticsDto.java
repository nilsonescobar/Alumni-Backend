package org.fia.alumni.alumnifiauesbackend.dto.statistics;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SurveyStatisticsDto {
    private Long surveyId;
    private String surveyTitle;
    private long totalResponses;
    private long completedResponses;
    private long inProgressResponses;
    private double completionRate;
    private double averageTimeSeconds;
    private List<QuestionStatisticsDto> questionStatistics;
}