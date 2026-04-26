package org.fia.alumni.alumnifiauesbackend.controller.alumni;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
public class AlumniController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;


    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAlumni(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Long currentUserId = authentication != null ? (Long) authentication.getPrincipal() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> usersPage = userRepository.findByUserTypeAndActiveTrue(User.UserType.GRADUATE, pageable);

        List<Map<String, Object>> results = usersPage.getContent().stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .filter(u -> {
                    if (q.isBlank()) return true;
                    Profile p = profileRepository.findById(u.getId()).orElse(null);
                    String name = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : "";
                    return name.contains(q.toLowerCase()) || u.getEmail().toLowerCase().contains(q.toLowerCase());
                })
                .map(u -> {
                    Profile p = profileRepository.findById(u.getId()).orElse(null);
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", u.getId());
                    dto.put("firstName", p != null ? p.getFirstName() : "");
                    dto.put("lastName", p != null ? p.getLastName() : "");
                    dto.put("fullName", p != null
                            ? p.getFirstName() + " " + p.getLastName()
                            : u.getEmail());
                    dto.put("profilePicture", p != null ? p.getProfilePicture() : null);
                    dto.put("bio", p != null ? p.getBio() : null);
                    dto.put("studentId", p != null ? p.getStudentId() : null);
                    dto.put("email", u.getEmail());
                    dto.put("userType", u.getUserType());
                    dto.put("active", u.getActive());
                    return dto;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", results);
        response.put("page", usersPage.getNumber());
        response.put("size", usersPage.getSize());
        response.put("totalElements", usersPage.getTotalElements());
        response.put("totalPages", usersPage.getTotalPages());
        return ResponseEntity.ok(response);
    }
}