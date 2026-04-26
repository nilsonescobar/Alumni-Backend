package org.fia.alumni.alumnifiauesbackend.controller.user;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.response.ApiResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.user.OutstandingAlumniResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.user.UserAdminProfileResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.user.UserPublicProfileResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.user.UserVerifierProfileResponse;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.security.SecurityUtils;
import org.fia.alumni.alumnifiauesbackend.service.user.OutstandingAlumniService;
import org.fia.alumni.alumnifiauesbackend.service.user.UserProfileQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileQueryService queryService;
    private final OutstandingAlumniService outstandingAlumniService;
    private final UserRepository userRepository;

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<ApiResponse<UserPublicProfileResponse>> getPublicProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Perfil obtenido",
                queryService.getPublicProfile(userId)));
    }

    @GetMapping("/admin/users/{userId}")
    public ResponseEntity<ApiResponse<UserAdminProfileResponse>> getAdminView(
            @PathVariable Long userId) {
        validateRole(User.UserType.ADMIN);
        return ResponseEntity.ok(ApiResponse.success("Perfil admin obtenido",
                queryService.getAdminProfile(userId)));
    }

    @GetMapping("/verifier/users/{userId}")
    public ResponseEntity<ApiResponse<UserVerifierProfileResponse>> getVerifierView(
            @PathVariable Long userId) {
        validateRoleAny(User.UserType.VERIFIER, User.UserType.ADMIN);
        return ResponseEntity.ok(ApiResponse.success("Perfil verificador obtenido",
                queryService.getVerifierProfile(userId)));
    }

    @GetMapping("/outstanding")
    public ResponseEntity<ApiResponse<List<OutstandingAlumniResponse>>> getOutstanding() {
        return ResponseEntity.ok(ApiResponse.success("Alumnos destacados",
                outstandingAlumniService.getAll()));
    }

    private void validateRole(User.UserType required) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (user.getUserType() != required) {
            throw new BadRequestException("Acceso denegado — se requiere rol " + required.name());
        }
    }

    private void validateRoleAny(User.UserType... roles) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        for (User.UserType role : roles) {
            if (user.getUserType() == role) return;
        }
        throw new BadRequestException("Acceso denegado");
    }
}