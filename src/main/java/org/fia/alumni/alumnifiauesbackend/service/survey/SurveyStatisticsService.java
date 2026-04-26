package org.fia.alumni.alumnifiauesbackend.service.survey;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.statistics.QuestionStatisticsDto;
import org.fia.alumni.alumnifiauesbackend.dto.statistics.SurveyStatisticsDto;
import org.fia.alumni.alumnifiauesbackend.dto.surveys.AnswerDistributionDto;
import org.fia.alumni.alumnifiauesbackend.entity.survey.Survey;
import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyResponse;
import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyResponseDetail;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyRepository;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyResponseDetailRepository;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyResponseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyStatisticsService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository responseRepository;
    private final SurveyResponseDetailRepository detailRepository;

    @Transactional(readOnly = true)
    public SurveyStatisticsDto getStatistics(Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId).orElseThrow();
        List<SurveyResponse> responses = responseRepository.findBySurveyId(surveyId, Pageable.unpaged()).getContent();
        List<SurveyResponseDetail> details = detailRepository.findBySurveyId(surveyId);

        long totalResponses = responses.size();
        long completedResponses = responses.stream().filter(r -> r.getStatus() == SurveyResponse.ResponseStatus.COMPLETED).count();
        long inProgressResponses = totalResponses - completedResponses;
        double completionRate = totalResponses == 0 ? 0 : ((double) completedResponses / totalResponses) * 100;

        double avgTime = responses.stream()
                .filter(r -> r.getTotalTimeSeconds() != null && r.getStatus() == SurveyResponse.ResponseStatus.COMPLETED)
                .mapToInt(SurveyResponse::getTotalTimeSeconds)
                .average().orElse(0.0);

        Map<String, List<SurveyResponseDetail>> detailsByQuestion = details.stream()
                .collect(Collectors.groupingBy(SurveyResponseDetail::getQuestionId));

        List<QuestionStatisticsDto> questionStats = detailsByQuestion.entrySet().stream().map(entry -> {
            String qId = entry.getKey();
            List<SurveyResponseDetail> qDetails = entry.getValue();
            String qType = qDetails.get(0).getQuestionType();
            String qText = qDetails.get(0).getQuestionText();
            long totalAnswers = qDetails.size();

            List<AnswerDistributionDto> distribution = new ArrayList<>();
            List<String> textAnswers = new ArrayList<>();

            if ("text".equals(qType) || "comment".equals(qType)) {
                textAnswers = qDetails.stream().map(SurveyResponseDetail::getAnswerValue).toList();
            } else {
                Map<String, Long> answerCounts = qDetails.stream()
                        .collect(Collectors.groupingBy(SurveyResponseDetail::getAnswerValue, Collectors.counting()));

                distribution = answerCounts.entrySet().stream()
                        .map(e -> new AnswerDistributionDto(e.getKey(), e.getValue(), ((double) e.getValue() / totalAnswers) * 100))
                        .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                        .toList();
            }

            return QuestionStatisticsDto.builder()
                    .questionId(qId)
                    .questionText(qText)
                    .questionType(qType)
                    .totalAnswers(totalAnswers)
                    .answerDistribution(distribution)
                    .textAnswers(textAnswers)
                    .build();
        }).toList();

        return SurveyStatisticsDto.builder()
                .surveyId(surveyId)
                .surveyTitle(survey.getTitle())
                .totalResponses(totalResponses)
                .completedResponses(completedResponses)
                .inProgressResponses(inProgressResponses)
                .completionRate(completionRate)
                .averageTimeSeconds(avgTime)
                .questionStatistics(questionStats)
                .build();
    }
}