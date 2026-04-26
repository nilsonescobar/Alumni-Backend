package org.fia.alumni.alumnifiauesbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "alumni-platform");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> databaseConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

            response.put("status", "CONNECTED");
            response.put("database", "PostgreSQL");
            response.put("version", version);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }
}