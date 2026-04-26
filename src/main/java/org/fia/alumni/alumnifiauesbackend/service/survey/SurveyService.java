package org.fia.alumni.alumnifiauesbackend.service.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.survey.Survey;
import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyResponse;
import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyResponseDetail;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyRepository;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyResponseDetailRepository;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyResponseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository responseRepository;
    private final SurveyResponseDetailRepository detailRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableSurveys(int page, int size, Long userId) {
        Page<Survey> p = surveyRepository.findAvailableForUser(userId, LocalDateTime.now(), PageRequest.of(page, size));

        List<Map<String, Object>> mappedSurveys = p.getContent().stream().map(s -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", s.getId());
            dto.put("title", s.getTitle());
            dto.put("description", s.getDescription());
            dto.put("startDate", s.getStartDate());
            dto.put("endDate", s.getEndDate());
            dto.put("isAnonymous", s.getIsAnonymous());

            String userStatus = "pendiente";
            Optional<SurveyResponse> resp = responseRepository.findBySurveyIdAndUserId(s.getId(), userId);
            if (resp.isPresent()) {
                userStatus = resp.get().getStatus() == SurveyResponse.ResponseStatus.COMPLETED ? "completada" : "en_progreso";
            }
            dto.put("userStatus", userStatus);
            return dto;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", mappedSurveys);
        response.put("page", p.getNumber());
        response.put("size", p.getSize());
        response.put("totalElements", p.getTotalElements());
        response.put("totalPages", p.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllSurveys(String status, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Survey> p;
        if (status != null && !status.isEmpty()) {
            Survey.SurveyStatus s = Survey.SurveyStatus.valueOf(status.toUpperCase());
            p = surveyRepository.findByStatusOrderByCreatedAtDesc(s, pageable);
        } else {
            p = surveyRepository.findAll(pageable);
        }
        return buildPageResponse(p, userId);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getSurveyById(Long id, Long userId) {
        return surveyRepository.findById(id).map(s -> {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", toDto(s, userId));
            return response;
        });
    }

    @Transactional
    public Map<String, Object> createSurvey(Map<String, Object> body, Long userId) {
        Survey survey = mapToSurvey(body, null);
        survey.setCreatedBy(userId);
        Survey saved = surveyRepository.save(survey);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Encuesta creada");
        response.put("data", toDto(saved, userId));
        return response;
    }

    @Transactional
    public Optional<Map<String, Object>> updateSurvey(Long id, Map<String, Object> body, Long userId) {
        return surveyRepository.findById(id).map(existing -> {
            mapToSurvey(body, existing);
            Survey saved = surveyRepository.save(existing);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", toDto(saved, userId));
            return response;
        });
    }

    @Transactional
    public Optional<Map<String, Object>> deleteSurvey(Long id) {
        return surveyRepository.findById(id).map(s -> {
            s.setStatus(Survey.SurveyStatus.ARCHIVED);
            surveyRepository.save(s);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Encuesta archivada");
            return response;
        });
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> publishSurvey(Long id, Long userId) {
        return surveyRepository.findById(id).map(s -> {
            s.setStatus(Survey.SurveyStatus.ACTIVE);
            if (s.getStartDate() == null) s.setStartDate(LocalDateTime.now());
            Survey saved = surveyRepository.save(s);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Encuesta publicada");
            response.put("data", toDto(saved, userId));
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    public Optional<Map<String, Object>> closeSurvey(Long id, Long userId) {
        return surveyRepository.findById(id).map(s -> {
            s.setStatus(Survey.SurveyStatus.CLOSED);
            s.setEndDate(LocalDateTime.now());
            Survey saved = surveyRepository.save(s);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Encuesta cerrada");
            response.put("data", toDto(saved, userId));
            return response;
        });
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> submitResponse(Long id, Map<String, Object> body, Long userId) {
        Survey survey = surveyRepository.findById(id).orElse(null);
        if (survey == null) return ResponseEntity.notFound().build();

        if (survey.getStatus() != Survey.SurveyStatus.ACTIVE) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "La encuesta no está activa");
            return ResponseEntity.badRequest().body(err);
        }

        boolean saveAsDraft = Boolean.TRUE.equals(body.get("saveAsDraft"));
        boolean completed = !saveAsDraft;

        SurveyResponse response = responseRepository.findBySurveyIdAndUserId(id, userId).orElse(null);

        if (completed && !Boolean.TRUE.equals(survey.getAllowMultipleResponses())
                && response != null && response.getStatus() == SurveyResponse.ResponseStatus.COMPLETED) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "Ya respondiste esta encuesta");
            return ResponseEntity.badRequest().body(err);
        }

        LocalDateTime now = LocalDateTime.now();
        if (response == null) {
            response = SurveyResponse.builder()
                    .surveyId(id)
                    .userId(userId)
                    .startedAt(now)
                    .build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> answersMap = (Map<String, Object>) body.get("answers");
        String responseJsonStr = "{}";
        try {
            if (answersMap != null) {
                responseJsonStr = objectMapper.writeValueAsString(answersMap);
            }
        } catch (Exception ignored) {}

        Integer timeSeconds = body.get("totalTimeSeconds") instanceof Number n ? n.intValue() : null;

        response.setResponseJson(responseJsonStr);
        response.setTotalTimeSeconds(timeSeconds);
        response.setStatus(completed ? SurveyResponse.ResponseStatus.COMPLETED : SurveyResponse.ResponseStatus.IN_PROGRESS);

        if (completed) {
            if (response.getStartedAt() != null && now.isBefore(response.getStartedAt())) {
                response.setCompletedAt(response.getStartedAt());
            } else {
                response.setCompletedAt(now);
            }
        } else {
            response.setCompletedAt(null);
        }

        SurveyResponse saved = responseRepository.save(response);

        if (completed && answersMap != null) {
            detailRepository.deleteByResponseId(saved.getId());
            answersMap.forEach((qId, answerVal) -> {
                SurveyResponseDetail detail = SurveyResponseDetail.builder()
                        .responseId(saved.getId())
                        .surveyId(id)
                        .questionId(qId)
                        .questionText(qId)
                        .questionType(answerVal instanceof java.util.List ? "checkbox" : "text")
                        .answerValue(answerVal.toString())
                        .build();
                detailRepository.save(detail);
            });
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", completed ? "Encuesta enviada exitosamente" : "Borrador guardado");
        res.put("data", saved);
        return ResponseEntity.ok(res);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getResponses(Long id, int page, int size) {
        Page<SurveyResponse> p = responseRepository.findBySurveyId(id, PageRequest.of(page, size));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", p.getContent().stream().map(this::toResponseDto).toList());
        response.put("totalCompleted", responseRepository.countBySurveyIdAndStatus(id, SurveyResponse.ResponseStatus.COMPLETED));
        response.put("page", p.getNumber());
        response.put("size", p.getSize());
        response.put("totalElements", p.getTotalElements());
        response.put("totalPages", p.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyResponse(Long id, Long userId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);

        responseRepository.findBySurveyIdAndUserId(id, userId).ifPresentOrElse(
                res -> response.put("data", toResponseDto(res)),
                () -> response.put("data", null)
        );

        return response;
    }

    private Map<String, Object> buildPageResponse(Page<Survey> page, Long userId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", page.getContent().stream().map(s -> toDto(s, userId)).toList());
        response.put("page", page.getNumber());
        response.put("size", page.getSize());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        return response;
    }

    private Map<String, Object> toDto(Survey s, Long userId) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", s.getId());
        dto.put("title", s.getTitle());
        dto.put("description", s.getDescription());
        dto.put("status", s.getStatus());
        dto.put("startDate", s.getStartDate());
        dto.put("endDate", s.getEndDate());
        dto.put("isAnonymous", s.getIsAnonymous());
        dto.put("allowMultipleResponses", s.getAllowMultipleResponses());
        dto.put("jsonSchema", s.getJsonSchema());
        dto.put("createdAt", s.getCreatedAt());
        dto.put("totalResponses", responseRepository.countBySurveyIdAndStatus(s.getId(), SurveyResponse.ResponseStatus.COMPLETED));
        if (userId != null) {
            dto.put("hasResponded", responseRepository.existsBySurveyIdAndUserId(s.getId(), userId));
        }
        return dto;
    }

    private Map<String, Object> toResponseDto(SurveyResponse r) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", r.getId());
        dto.put("surveyId", r.getSurveyId());
        dto.put("userId", r.getUserId());
        dto.put("status", r.getStatus());
        dto.put("responseJson", r.getResponseJson());
        dto.put("totalTimeSeconds", r.getTotalTimeSeconds());
        dto.put("startedAt", r.getStartedAt());
        dto.put("completedAt", r.getCompletedAt());
        try {
            dto.put("answers", objectMapper.readValue(r.getResponseJson(), Map.class));
        } catch (Exception ignored) {}
        return dto;
    }

    private Survey mapToSurvey(Map<String, Object> body, Survey existing) {
        Survey s = existing != null ? existing : Survey.builder().build();
        if (body.containsKey("title")) s.setTitle((String) body.get("title"));
        if (body.containsKey("description")) s.setDescription((String) body.get("description"));
        if (body.containsKey("jsonSchema")) s.setJsonSchema((String) body.get("jsonSchema"));
        if (body.containsKey("isAnonymous")) s.setIsAnonymous((Boolean) body.get("isAnonymous"));
        if (body.containsKey("allowMultipleResponses")) s.setAllowMultipleResponses((Boolean) body.get("allowMultipleResponses"));
        if (body.containsKey("startDate") && body.get("startDate") != null) s.setStartDate(LocalDateTime.parse((String) body.get("startDate")));
        if (body.containsKey("endDate") && body.get("endDate") != null) s.setEndDate(LocalDateTime.parse((String) body.get("endDate")));
        return s;
    }
}