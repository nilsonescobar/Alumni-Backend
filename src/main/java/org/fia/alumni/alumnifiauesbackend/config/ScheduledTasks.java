package org.fia.alumni.alumnifiauesbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.service.auth.AuthenticationService;
import org.fia.alumni.alumnifiauesbackend.service.auth.PasswordService;
import org.fia.alumni.alumnifiauesbackend.service.security.RateLimitService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {

    private final AuthenticationService authenticationService;
    private final PasswordService passwordService;
    private final RateLimitService rateLimitService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        authenticationService.cleanupExpiredTokens();
        passwordService.cleanupExpiredTokens();
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanupOldLoginAttempts() {
        rateLimitService.cleanupOldAttempts();
    }
}