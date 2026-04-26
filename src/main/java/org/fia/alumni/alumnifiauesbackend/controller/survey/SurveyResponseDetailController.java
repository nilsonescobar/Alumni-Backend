package org.fia.alumni.alumnifiauesbackend.controller.survey;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.survey.Survey;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyRepository;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyResponseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/surveys/responses")
@RequiredArgsConstructor
public class SurveyResponseDetailController {

    private final SurveyResponseRepository responseRepository;
    private final SurveyRepository surveyRepository;

    @GetMapping("/{responseId}")
    public ResponseEntity<Map<String, Object>> getResponseDetail(@PathVariable Long responseId) {
        return responseRepository.findById(responseId).map(response -> {
            Survey survey = surveyRepository.findById(response.getSurveyId()).orElseThrow();

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", response.getId());
            dto.put("surveyId", response.getSurveyId());
            dto.put("surveyTitle", survey.getTitle());
            dto.put("status", response.getStatus());
            dto.put("responseJson", response.getResponseJson());
            dto.put("totalTimeSeconds", response.getTotalTimeSeconds());
            dto.put("startedAt", response.getStartedAt());
            dto.put("completedAt", response.getCompletedAt());

            if (response.getUserId() != null) {
                Map<String, Object> userDto = new LinkedHashMap<>();
                userDto.put("id", response.getUserId());
                userDto.put("nombres", "Usuario");
                userDto.put("apellidos", "Registrado");
                userDto.put("correo", "usuario@ejemplo.com");
                userDto.put("tipoUsuario", "GRADUADO");
                dto.put("user", userDto);
            }

            return ResponseEntity.ok(Map.<String, Object>of("success", true, "data", dto));
        }).orElse(ResponseEntity.notFound().build());
    }
}