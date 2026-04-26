package org.fia.alumni.alumnifiauesbackend.controller.survey;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.service.survey.SurveyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(surveyService.getAvailableSurveys(page, size, userId));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(surveyService.getAllSurveys(status, page, size, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return surveyService.getSurveyById(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(surveyService.createSurvey(body, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return surveyService.updateSurvey(id, body, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        return surveyService.deleteSurvey(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publish(
            @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return surveyService.publishSurvey(id, userId);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> close(
            @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return surveyService.closeSurvey(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/responses")
    public ResponseEntity<Map<String, Object>> submitResponse(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return surveyService.submitResponse(id, body, userId);
    }

    @GetMapping("/{id}/responses")
    public ResponseEntity<Map<String, Object>> getResponses(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(surveyService.getResponses(id, page, size));
    }

    @GetMapping("/{id}/my-response")
    public ResponseEntity<Map<String, Object>> getMyResponse(
            @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(surveyService.getMyResponse(id, userId));
    }
}