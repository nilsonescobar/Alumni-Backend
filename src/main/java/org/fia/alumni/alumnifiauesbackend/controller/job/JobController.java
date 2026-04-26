package org.fia.alumni.alumnifiauesbackend.controller.job;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.job.JobOpportunity;
import org.fia.alumni.alumnifiauesbackend.repository.job.JobOpportunityRepository;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobOpportunityRepository jobRepository;

    /** GET /api/v1/jobs?page=0&size=3&sort=createdDate,desc */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobOpportunity> jobsPage = jobRepository.findActiveJobs(LocalDateTime.now(), pageable);
        return ResponseEntity.ok(buildPageResponse(jobsPage));
    }

    /** GET /api/v1/jobs/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(j -> ResponseEntity.ok(Map.of("success", true, "data", toDto(j))))
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/v1/jobs */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createJob(
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        JobOpportunity job = mapToJob(body, null);
        job.setPostedByUserId(userId);
        JobOpportunity saved = jobRepository.save(job);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", "Oferta de empleo creada");
        r.put("data", toDto(saved));
        return ResponseEntity.ok(r);
    }

    /** PUT /api/v1/jobs/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateJob(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        return jobRepository.findById(id).map(existing -> {
            mapToJob(body, existing);
            JobOpportunity saved = jobRepository.save(existing);
            return ResponseEntity.ok(Map.of("success", true, "data", toDto(saved)));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/v1/jobs/{id} — hard delete (no tiene active flag) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable Long id) {
        return jobRepository.findById(id).map(job -> {
            jobRepository.delete(job);
            return ResponseEntity.ok(Map.<String, Object>of("success", true, "message", "Oferta eliminada"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── helpers ──────────────────────────────────────────────

    private Map<String, Object> buildPageResponse(Page<JobOpportunity> page) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("data", page.getContent().stream().map(this::toDto).toList());
        r.put("page", page.getNumber());
        r.put("size", page.getSize());
        r.put("totalElements", page.getTotalElements());
        r.put("totalPages", page.getTotalPages());
        return r;
    }

    private Map<String, Object> toDto(JobOpportunity j) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", j.getId());
        dto.put("title", j.getTitle());
        dto.put("companyName", j.getCompanyName());
        dto.put("description", j.getDescription());
        dto.put("requirements", j.getRequirements());
        dto.put("howToApply", j.getHowToApply());
        dto.put("city", j.getCity());
        dto.put("isRemote", j.getIsRemote());
        dto.put("jobType", j.getJobType());
        dto.put("experienceLevel", j.getExperienceLevel());
        dto.put("salaryRange", j.getSalaryRange());
        dto.put("expiresAt", j.getExpiresAt());
        dto.put("createdAt", j.getCreatedAt());
        return dto;
    }

    private JobOpportunity mapToJob(Map<String, Object> body, JobOpportunity existing) {
        JobOpportunity j = existing != null ? existing : JobOpportunity.builder().isRemote(false).build();
        if (body.containsKey("title"))           j.setTitle((String) body.get("title"));
        if (body.containsKey("companyName"))     j.setCompanyName((String) body.get("companyName"));
        if (body.containsKey("description"))     j.setDescription((String) body.get("description"));
        if (body.containsKey("requirements"))    j.setRequirements((String) body.get("requirements"));
        if (body.containsKey("howToApply"))      j.setHowToApply((String) body.get("howToApply"));
        if (body.containsKey("city"))            j.setCity((String) body.get("city"));
        if (body.containsKey("isRemote"))        j.setIsRemote((Boolean) body.get("isRemote"));
        if (body.containsKey("jobType"))         j.setJobType((String) body.get("jobType"));
        if (body.containsKey("experienceLevel")) j.setExperienceLevel((String) body.get("experienceLevel"));
        if (body.containsKey("salaryRange"))     j.setSalaryRange((String) body.get("salaryRange"));
        if (body.containsKey("expiresAt") && body.get("expiresAt") != null)
            j.setExpiresAt(LocalDateTime.parse((String) body.get("expiresAt")));
        return j;
    }
}