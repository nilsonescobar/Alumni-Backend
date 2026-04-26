package org.fia.alumni.alumnifiauesbackend.dto.statistics;

import lombok.Builder;
import lombok.Data;
import org.fia.alumni.alumnifiauesbackend.dto.surveys.AnswerDistributionDto;

import java.util.List;

@Data
@Builder
public class QuestionStatisticsDto {
    private String questionId;
    private String questionText;
    private String questionType;
    private long totalAnswers;
    private List<AnswerDistributionDto> answerDistribution;
    private List<String> textAnswers;
}