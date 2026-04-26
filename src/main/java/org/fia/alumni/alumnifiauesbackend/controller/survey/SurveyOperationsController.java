package org.fia.alumni.alumnifiauesbackend.controller.survey;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.survey.AssignSurveyRequest;
import org.fia.alumni.alumnifiauesbackend.service.survey.SurveyAssignmentService;
import org.fia.alumni.alumnifiauesbackend.service.survey.SurveyStatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyOperationsController {

    private final SurveyAssignmentService assignmentService;
    private final SurveyStatisticsService statisticsService;

    @PostMapping("/{id}/assignments")
    public ResponseEntity<Map<String, Object>> assignSurvey(@PathVariable Long id, @RequestBody AssignSurveyRequest request) {
        assignmentService.assignSurvey(id, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Encuesta asignada"));
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<Map<String, Object>> getAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", assignmentService.getAssignments(id)));
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    public ResponseEntity<Map<String, Object>> removeAssignment(@PathVariable Long id, @PathVariable Long assignmentId) {
        assignmentService.removeAssignment(assignmentId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Asignación eliminada"));
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", statisticsService.getStatistics(id)));
    }

    @GetMapping("/{id}/statistics/export")
    public ResponseEntity<Map<String, Object>> exportStatistics(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", statisticsService.getStatistics(id)));
    }
}