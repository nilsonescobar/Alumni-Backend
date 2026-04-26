package org.fia.alumni.alumnifiauesbackend.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.entity.security.LoginAttempt;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.repository.security.LoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final LoginAttemptRepository loginAttemptRepository;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_HOURS = 1;

    @Transactional(readOnly = true)
    public void checkRateLimit(String email) {
        loginAttemptRepository.findByEmail(email).ifPresent(attempt -> {
            if (attempt.isLocked()) {
                long minutesRemaining = java.time.Duration
                        .between(LocalDateTime.now(), attempt.getLockedUntil())
                        .toMinutes();

                throw new BadRequestException(
                        String.format("Cuenta bloqueada. Intenta de nuevo en %d minutos", minutesRemaining + 1)
                );
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String email) {
        LoginAttempt attempt = loginAttemptRepository.findByEmail(email)
                .orElse(LoginAttempt.builder().email(email).failedAttempts(0).build());

        attempt.incrementFailedAttempts();

        if (attempt.getFailedAttempts() >= MAX_ATTEMPTS) {
            attempt.lock(LOCK_DURATION_HOURS);
        }

        loginAttemptRepository.saveAndFlush(attempt);
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String email) {
        return loginAttemptRepository.findByEmail(email)
                .map(LoginAttempt::isLocked)
                .orElse(false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(String email) {
        loginAttemptRepository.findByEmail(email).ifPresent(attempt -> {
            attempt.resetFailedAttempts();
            loginAttemptRepository.saveAndFlush(attempt);
        });
    }

    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime now = LocalDateTime.now();
        loginAttemptRepository.unlockExpiredAccounts(now);
        loginAttemptRepository.deleteOldUnlockedAttempts(now.minusDays(30));
    }
}