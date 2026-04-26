package org.fia.alumni.alumnifiauesbackend.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.dto.request.auth.RegisterRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.auth.RegisterResponse;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.user.Graduate;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ConflictException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.GraduateRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.service.email.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final GraduateRepository graduateRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        validateIdentifiers(request);

        validatePasswordMatch(request);

        validateEmailNotExists(request.getEmail());

        Graduate graduate = findGraduate(request);

        validateNames(request, graduate);

        validateGraduateNotRegistered(graduate.getId());

        User user = createUser(request, graduate);

        createProfile(user, graduate, request);

        emailService.sendVerificationEmail(
                user.getEmail(),
                graduate.getFirstName(),
                user.getVerificationToken()
        );

        log.info("Registration successful for user: {}", user.getEmail());

        return RegisterResponse.builder()
                .message("Registro exitoso. Por favor verifica tu correo electrónico.")
                .email(user.getEmail())
                .emailSent(true)
                .build();
    }

    @Transactional
    public String verifyEmail(String token) {
        log.info("Verifying email with token: {}", token);

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Token de verificación inválido o expirado"));

        if (user.getTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El token de verificación ha expirado");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiration(null);
        userRepository.save(user);

        Profile profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        emailService.sendWelcomeEmail(user.getEmail(), profile.getFirstName());

        log.info("Email verified successfully for user: {}", user.getEmail());

        return "Email verificado exitosamente. Ya puedes iniciar sesión.";
    }

    private void validateIdentifiers(RegisterRequest request) {
        if ((request.getStudentId() == null || request.getStudentId().isBlank()) &&
                (request.getIdentityDocument() == null || request.getIdentityDocument().isBlank())) {
            throw new BadRequestException("Debes proporcionar al menos el Carnet o el DUI");
        }
    }

    private void validatePasswordMatch(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }
    }

    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("El correo electrónico ya está registrado");
        }
    }

    private Graduate findGraduate(RegisterRequest request) {
        String identifier = request.getStudentId() != null ?
                request.getStudentId() : request.getIdentityDocument();

        return graduateRepository.findByStudentIdOrIdentityDocument(identifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró registro de graduado con el identificador proporcionado"
                ));
    }

    private void validateNames(RegisterRequest request, Graduate graduate) {
        String normalizedRequestFirst = normalize(request.getFirstName());
        String normalizedRequestLast = normalize(request.getLastName());
        String normalizedGraduateFirst = normalize(graduate.getFirstName());
        String normalizedGraduateLast = normalize(graduate.getLastName());

        if (!normalizedRequestFirst.equals(normalizedGraduateFirst) ||
                !normalizedRequestLast.equals(normalizedGraduateLast)) {
            throw new BadRequestException(
                    "Los nombres proporcionados no coinciden con los registros de graduación"
            );
        }
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("á", "a")
                .replaceAll("é", "e")
                .replaceAll("í", "i")
                .replaceAll("ó", "o")
                .replaceAll("ú", "u")
                .replaceAll("ñ", "n")
                .trim();
    }

    private void validateGraduateNotRegistered(Long graduateId) {
        if (userRepository.existsBySourceGraduateId(graduateId)) {
            throw new ConflictException(
                    "Este graduado ya tiene una cuenta registrada en el sistema"
            );
        }
    }

    private User createUser(RegisterRequest request, Graduate graduate) {
        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(User.UserType.GRADUATE)
                .active(true)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .tokenExpiration(LocalDateTime.now().plusHours(24))
                .registeredWith(request.getStudentId() != null ? "student_id" : "identity_document")
                .sourceGraduateId(graduate.getId())
                .passwordChangedAt(LocalDateTime.now())
                .passwordMustChange(false)
                .build();

        return userRepository.save(user);
    }

    private void createProfile(User user, Graduate graduate, RegisterRequest request) {
        Profile profile = Profile.builder()
                .user(user)
                .firstName(graduate.getFirstName())
                .lastName(graduate.getLastName())
                .studentId(graduate.getStudentId())
                .identityDocument(graduate.getIdentityDocument())
                .graduationYear(graduate.getGraduationYear())
                .graduationGpa(graduate.getGpa())
                .careerId(graduate.getCareerId())
                .build();

        profileRepository.save(profile);
    }
}