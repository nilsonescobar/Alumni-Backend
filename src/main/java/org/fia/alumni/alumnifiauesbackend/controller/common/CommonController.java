package org.fia.alumni.alumnifiauesbackend.controller.common;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.common.FeaturedAlumni;
import org.fia.alumni.alumnifiauesbackend.repository.common.FeaturedAlumniRepository;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final FeaturedAlumniRepository featuredAlumniRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @GetMapping("/destacados")
    public ResponseEntity<Map<String, Object>> getDestacados() {
        List<FeaturedAlumni> featured = featuredAlumniRepository.findAllByOrderByRecognitionDateDesc();

        List<Map<String, Object>> result = featured.stream().map(f -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", f.getId());
            dto.put("userId", f.getUserId());
            dto.put("reason", f.getReason());
            dto.put("recognitionDate", f.getRecognitionDate());
            dto.put("referenceUrl", f.getReferenceUrl());

            profileRepository.findById(f.getUserId()).ifPresent(p -> {
                dto.put("fullName", p.getFullName());
                dto.put("profilePicture", p.getProfilePicture());
                dto.put("bio", p.getBio());
            });
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("data", result);
        return ResponseEntity.ok(r);
    }
}