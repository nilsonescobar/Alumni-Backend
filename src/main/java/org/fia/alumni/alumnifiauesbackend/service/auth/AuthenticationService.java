package org.fia.alumni.alumnifiauesbackend.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.dto.request.auth.LoginRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.auth.LoginResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.search.UserSearchResult;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Career;
import org.fia.alumni.alumnifiauesbackend.entity.catalog.Country;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.security.RefreshToken;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CareerRepository;
import org.fia.alumni.alumnifiauesbackend.repository.catalog.CountryRepository;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.security.RefreshTokenRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.security.jwt.JwtTokenProvider;
import org.fia.alumni.alumnifiauesbackend.service.security.MfaService;
import org.fia.alumni.alumnifiauesbackend.service.security.RateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MfaService mfaService;
    private final RateLimitService rateLimitService;
    private final CareerRepository careerRepository;
    private final CountryRepository countryRepository;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String ipAddress = getClientIp(httpRequest);

        log.info("Login attempt for email: {} from IP: {}", email, ipAddress);

        rateLimitService.checkRateLimit(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    rateLimitService.recordFailedAttempt(email);
                    return new BadRequestException("Credenciales inválidas");
                });

        if (!user.getActive()) {
            rateLimitService.recordFailedAttempt(email);
            throw new BadRequestException("Tu cuenta está desactivada. Contacta al administrador");
        }

        if (!user.getEmailVerified()) {
            rateLimitService.recordFailedAttempt(email);
            throw new BadRequestException("Debes verificar tu correo electrónico antes de iniciar sesión");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            rateLimitService.recordFailedAttempt(email);
            log.warn("Failed login attempt for email: {} from IP: {}", email, ipAddress);
            throw new BadRequestException("Credenciales inválidas");
        }

        boolean mfaEnabled = mfaService.isMfaEnabled(user.getId());
        if (mfaEnabled) {
            if (request.getMfaCode() == null || request.getMfaCode().isBlank()) {
                String tempToken = jwtTokenProvider.generateToken(
                        user.getEmail(), user.getId(), "MFA_TEMP");
                return LoginResponse.builder()
                        .mfaRequired(true)
                        .mfaTempToken(tempToken)
                        .build();
            }
            if (!mfaService.verifyMfaCode(user.getId(), request.getMfaCode())) {
                rateLimitService.recordFailedAttempt(email);
                throw new BadRequestException("Código MFA inválido");
            }
        }

        if (needsPasswordChange(user)) {
            user.setPasswordMustChange(true);
            userRepository.save(user);
        }

        refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateToken(
                user.getEmail(), user.getId(), user.getUserType().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getEmail(), user.getId());

        saveRefreshToken(user.getId(), refreshToken);

        rateLimitService.recordSuccessfulLogin(email);

        Profile profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .userType(user.getUserType().name())
                .emailVerified(user.getEmailVerified())
                .mfaEnabled(mfaEnabled)
                .build();

        log.info("Login successful for user: {} from IP: {}", user.getEmail(), ipAddress);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(userInfo)
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshTokenStr) {
        log.info("Refresh token request");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BadRequestException("Token de refresco inválido"));

        if (!refreshToken.isValid()) {
            throw new BadRequestException("Token de refresco expirado o revocado");
        }

        String email = jwtTokenProvider.extractEmail(refreshTokenStr);
        if (!jwtTokenProvider.validateToken(refreshTokenStr, email)) {
            throw new BadRequestException("Token de refresco inválido");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String newAccessToken = jwtTokenProvider.generateToken(
                user.getEmail(), user.getId(), user.getUserType().name());

        Profile profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        boolean mfaEnabled = mfaService.isMfaEnabled(user.getId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .userType(user.getUserType().name())
                .emailVerified(user.getEmailVerified())
                .mfaEnabled(mfaEnabled)
                .build();

        log.info("Token refreshed for user: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(userInfo)
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public void logout(String accessToken, String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BadRequestException("Token de refresco inválido"));

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        log.info("User logged out — refresh token revoked");
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
        log.info("All sessions revoked for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public UserSearchResult getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        boolean mfaEnabled = mfaService.isMfaEnabled(user.getId());

        UserSearchResult.UserSummaryDto userDto = UserSearchResult.UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .active(user.isActive())
                .hasDisability(user.getHasDisability())
                .userType(user.getUserType().name())
                .mfaEnabled(mfaEnabled)
                .build();

        String careerName = null;
        String universityName = null;
        if (profile.getCareerId() != null) {
            Career career = careerRepository.findById(profile.getCareerId()).orElse(null);
            if (career != null) {
                careerName = career.getName();
                if (career.getUniversity() != null) {
                    universityName = career.getUniversity().getName();
                }
            }
        }

        String countryName = null;
        if (profile.getCountryId() != null) {
            Country country = countryRepository.findById(profile.getCountryId()).orElse(null);
            if (country != null) {
                countryName = country.getName();
            }
        }

        UserSearchResult.ProfileSummaryDto profileDto = UserSearchResult.ProfileSummaryDto.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .fullName(profile.getFullName())
                .profilePicture(profile.getProfilePicture())
                .bio(profile.getBio())
                .graduationYear(profile.getGraduationYear())
                .careerName(careerName)
                .universityName(universityName)
                .city(profile.getCity())
                .countryName(countryName)
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .linkedinUrl(profile.getLinkedinUrl())
                .websiteUrl(profile.getWebsiteUrl())
                .studentId(profile.getStudentId())
                .identityDocument(profile.getIdentityDocument())
                .privacySettings(profile.getPrivacySettings())
                .profileCompletionPercentage(user.getProfileCompletionPercentage())
                .connectionCount(0)
                .isConnection(false)
                .connectionRequestPending(false)
                .build();

        return UserSearchResult.builder()
                .user(userDto)
                .profile(profileDto)
                .build();
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now());
        log.info("Expired/revoked refresh tokens cleaned up");
    }

    private void saveRefreshToken(Long userId, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private boolean needsPasswordChange(User user) {
        if (user.getPasswordChangedAt() == null) return false;
        return user.getPasswordChangedAt().isBefore(LocalDateTime.now().minusDays(90));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty())
            return xForwardedFor.split(",")[0].trim();
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;
        return request.getRemoteAddr();
    }

    @Transactional
    public LoginResponse completeMfaLogin(Long userId) {
        log.info("Completing MFA login for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!user.getActive()) {
            throw new BadRequestException("Tu cuenta está desactivada. Contacta al administrador");
        }

        Profile profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateToken(
                user.getEmail(), user.getId(), user.getUserType().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(
                user.getEmail(), user.getId());

        saveRefreshToken(user.getId(), refreshTokenStr);

        rateLimitService.recordSuccessfulLogin(user.getEmail());

        boolean mfaEnabled = mfaService.isMfaEnabled(user.getId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .userType(user.getUserType().name())
                .emailVerified(user.getEmailVerified())
                .mfaEnabled(mfaEnabled)
                .build();

        log.info("MFA Login successful for user: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(userInfo)
                .mfaRequired(false)
                .mfaTempToken(null)
                .build();
    }
}