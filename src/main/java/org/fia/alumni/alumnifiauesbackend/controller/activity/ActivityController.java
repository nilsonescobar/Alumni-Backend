package org.fia.alumni.alumnifiauesbackend.controller.activity;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.response.ApiResponse;
import org.fia.alumni.alumnifiauesbackend.security.SecurityUtils;
import org.fia.alumni.alumnifiauesbackend.security.jwt.JwtTokenProvider;
import org.fia.alumni.alumnifiauesbackend.service.activity.ActivityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;



    @GetMapping("/my-timeline")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyTimeline(
            @RequestParam(defaultValue = "20") int limit) {

        Long userId = SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> timeline = activityService.getMyTimeline(userId, limit);
        return ResponseEntity.ok(ApiResponse.success("Timeline obtenido exitosamente", timeline));
    }
}