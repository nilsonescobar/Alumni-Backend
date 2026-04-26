package org.fia.alumni.alumnifiauesbackend.service.activity;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.activity.UserActivity;
import org.fia.alumni.alumnifiauesbackend.repository.activity.UserActivityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final UserActivityRepository activityRepository;

    @Transactional
    public void record(Long userId, String activityType) {
        record(userId, activityType, null, true);
    }

    @Transactional
    public void record(Long userId, String activityType, Map<String, Object> context, boolean isPublic) {
        activityRepository.save(
                UserActivity.builder()
                        .userId(userId)
                        .activityType(activityType)
                        .context(context)
                        .isPublic(isPublic)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyTimeline(Long userId, int limit) {
        return activityRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(a -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", a.getId());
                    dto.put("activityType", a.getActivityType());
                    dto.put("context", a.getContext());
                    dto.put("isPublic", a.getIsPublic());
                    dto.put("createdAt", a.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}