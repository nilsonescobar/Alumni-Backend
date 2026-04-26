package org.fia.alumni.alumnifiauesbackend.controller.search;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.search.UserSearchRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.search.UserDetailResult;
import org.fia.alumni.alumnifiauesbackend.security.SecurityUtils;
import org.fia.alumni.alumnifiauesbackend.service.search.UserSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class UserSearchController {

    private final UserSearchService userSearchService;

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestBody(required = false) UserSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String userType = SecurityUtils.getCurrentUserType();

        if (request == null) {
            request = new UserSearchRequest();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        Page<?> results = userSearchService.searchUsers(
                request,
                currentUserId,
                userType,
                pageable
        );

        return buildResponse(results);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String userType = SecurityUtils.getCurrentUserType();

        UserDetailResult result = userSearchService.getUserById(id, currentUserId, userType);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/by-year/{year}")
    public ResponseEntity<Map<String, Object>> getUsersByYear(
            @PathVariable Integer year,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String userType = SecurityUtils.getCurrentUserType();

        Page<?> results = userSearchService.getUsersByGraduationYear(
                year,
                currentUserId,
                userType,
                pageable
        );

        return buildResponse(results);
    }

    @GetMapping("/users/by-career/{careerId}")
    public ResponseEntity<Map<String, Object>> getUsersByCareer(
            @PathVariable Long careerId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String userType = SecurityUtils.getCurrentUserType();

        Page<?> results = userSearchService.getUsersByCareer(
                careerId,
                currentUserId,
                userType,
                pageable
        );

        return buildResponse(results);
    }

    @GetMapping("/users/quick-search")
    public ResponseEntity<Map<String, Object>> quickSearch(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String userType = SecurityUtils.getCurrentUserType();

        UserSearchRequest request = UserSearchRequest.builder()
                .query(q)
                .build();

        Page<?> results = userSearchService.searchUsers(
                request,
                currentUserId,
                userType,
                pageable
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", results.getContent());
        response.put("totalElements", results.getTotalElements());

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(Page<?> results) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", results.getContent());
        response.put("page", results.getNumber());
        response.put("size", results.getSize());
        response.put("totalElements", results.getTotalElements());
        response.put("totalPages", results.getTotalPages());
        return ResponseEntity.ok(response);
    }
}