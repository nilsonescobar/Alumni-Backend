package org.fia.alumni.alumnifiauesbackend.service.email;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.util.email.EmailTemplateFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final int[] RETRY_DELAYS               = {1000, 2000, 5000, 10000, 15000};
    private static final int   DEFAULT_MAX_RETRIES        = 3;
    private static final int   VERIFICATION_MAX_RETRIES   = 5;
    private static final int   PASSWORD_RESET_MAX_RETRIES = 4;
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final EmailClient emailClient;

    @Value("${azure.communication.sender-address}")
    private String senderAddress;

    @Value("${azure.communication.sender-name}")
    private String senderName;

    @Value("${frontend.url}")
    private String frontendUrl;

    public void sendVerificationEmail(String to, String firstName, String verificationToken) {
        try {
            String url  = obfuscatedUrl(frontendUrl + "/auth/signup/verify-account", verificationToken, to);
            String html = EmailTemplateFactory.verification(firstName, url);
            sendWithRetry(message(to, firstName, "🔐 Verifica tu cuenta - Alumni FIA UES", html),
                    "verificación", to, VERIFICATION_MAX_RETRIES);
        } catch (Exception e) {
            log.error("Falló email de verificación a: {}. Error: {}", to, e.getMessage());
            throw new RuntimeException("Falló el envío de email de verificación", e);
        }
    }

    public void sendWelcomeEmail(String to, String firstName) {
        try {
            String html = EmailTemplateFactory.welcome(firstName, frontendUrl + "/auth/login");
            sendWithRetry(message(to, firstName, "🎉 ¡Bienvenido a Alumni FIA UES!", html),
                    "bienvenida", to, VERIFICATION_MAX_RETRIES);
        } catch (Exception e) {
            log.error("Falló email de bienvenida a: {}. Error: {}", to, e.getMessage());
            throw new RuntimeException("Falló el envío de email de bienvenida", e);
        }
    }

    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        try {
            String url  = obfuscatedUrl(frontendUrl + "/auth/reset-password", resetToken, null);
            String html = EmailTemplateFactory.passwordReset(username, url);
            sendWithRetry(message(to, username, "🔑 Restablecer Contraseña - Alumni FIA UES", html),
                    "restablecimiento-contraseña", to, PASSWORD_RESET_MAX_RETRIES);
        } catch (Exception e) {
            log.error("Falló email de restablecimiento a: {}. Error: {}", to, e.getMessage());
            throw new RuntimeException("Falló el envío de email de restablecimiento de contraseña", e);
        }
    }

    public void sendPasswordChangedEmail(String to, String username) {
        sendPasswordChangeNotification(to, username, PasswordChangeType.MANUAL_CHANGE, null, null);
    }

    public void sendPasswordChangeNotification(String to, String username,
                                               PasswordChangeType changeType,
                                               String ipAddress, String userAgent) {
        try {
            String html = EmailTemplateFactory.passwordChanged(
                    username, changeType, ipAddress, userAgent,
                    frontendUrl + "/auth/login",
                    frontendUrl + "/auth/login/forgot-my-password"
            );
            sendAsync(message(to, username, "🔒 Contraseña Cambiada - Alumni FIA UES", html),
                    "notificación-cambio-contraseña", to)
                    .whenComplete((r, t) -> {
                        if (t != null)
                            log.warn("Notificación de cambio de contraseña falló para {}, pero el cambio fue exitoso", to);
                    });
        } catch (Exception e) {
            log.error("Error preparando notificación de cambio de contraseña para: {}. Error: {}", to, e.getMessage());
        }
    }

    public void sendVerificationApprovedEmail(String toEmail, String fullName) {
        try {
            String html = EmailTemplateFactory.verificationApproved(fullName, frontendUrl + "/auth/login");
            sendAsync(message(toEmail, fullName, "✅ Verificación Aprobada - Alumni FIA UES", html),
                    "verificación-aprobada", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de verificación aprobada: {}", e.getMessage());
        }
    }

    public void sendVerificationRejectedEmail(String toEmail, String fullName, String observaciones) {
        try {
            String html = EmailTemplateFactory.verificationRejected(
                    fullName, observaciones,
                    frontendUrl + "/contact",
                    "soporte@alumni-fia.edu.sv"
            );
            sendAsync(message(toEmail, fullName, "❌ Verificación Pendiente - Alumni FIA UES", html),
                    "verificación-rechazada", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de verificación rechazada: {}", e.getMessage());
        }
    }

    public void sendProfileUpdateNotification(String toEmail, String fullName) {
        try {
            String html = EmailTemplateFactory.profileUpdate(fullName, frontendUrl + "/auth/login");
            sendAsync(message(toEmail, fullName, "🔄 Perfil Actualizado - Alumni FIA UES", html),
                    "actualización-perfil", toEmail);
        } catch (Exception e) {
            log.error("Error enviando notificación de actualización de perfil: {}", e.getMessage());
        }
    }

    public void sendAccountDeactivationConfirmation(String toEmail, String fullName, String reason) {
        try {
            String html = EmailTemplateFactory.accountDeactivation(
                    fullName, reason, frontendUrl + "/auth/reactivate");
            sendAsync(message(toEmail, fullName, "⚠️ Cuenta Desactivada - Alumni FIA UES", html),
                    "desactivación-cuenta", toEmail);
        } catch (Exception e) {
            log.error("Error enviando confirmación de desactivación: {}", e.getMessage());
        }
    }

    public void sendAccountReactivationEmail(String toEmail, String fullName, String token) {
        try {
            String url  = obfuscatedUrl(frontendUrl + "/auth/reactivate-account", token, toEmail);
            String html = EmailTemplateFactory.accountReactivation(fullName, url);
            sendWithRetry(message(toEmail, fullName, "🔄 Reactivar tu cuenta - Alumni FIA UES", html),
                    "reactivación-cuenta", toEmail, PASSWORD_RESET_MAX_RETRIES);
        } catch (Exception e) {
            log.error("Falló email de reactivación a: {}. Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Falló el envío de email de reactivación", e);
        }
    }

    public void sendAccountReactivationConfirmation(String toEmail, String fullName,
                                                    String userAgent, String ipAddress) {
        try {
            String html = EmailTemplateFactory.reactivationConfirmation(
                    fullName, userAgent, ipAddress,
                    frontendUrl + "/auth/login",
                    frontendUrl + "/contact"
            );
            sendAsync(message(toEmail, fullName, "✅ Cuenta Reactivada - Alumni FIA UES", html),
                    "confirmación-reactivación", toEmail)
                    .whenComplete((r, t) -> {
                        if (t != null)
                            log.warn("Confirmación de reactivación falló para {}, pero la reactivación fue exitosa", toEmail);
                    });
        } catch (Exception e) {
            log.error("Error enviando confirmación de reactivación a: {}. Error: {}", toEmail, e.getMessage());
        }
    }

    public void sendNewEventNotification(String toEmail, String fullName, String eventTitle,
                                         String eventDescription, String eventUrl) {
        try {
            String html = EmailTemplateFactory.eventNotification(fullName, eventTitle, eventDescription, eventUrl);
            sendAsync(message(toEmail, fullName, "📣 ¡Nuevo Evento! " + eventTitle, html),
                    "notificacion-nuevo-evento", toEmail);
        } catch (Exception e) {
            log.error("Error enviando notificación de nuevo evento a {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendEventRegistrationConfirmation(String toEmail, String fullName,
                                                  String eventTitle, String eventUrl) {
        try {
            String html = EmailTemplateFactory.eventRegistrationConfirmation(fullName, eventTitle, eventUrl);
            sendAsync(message(toEmail, fullName, "✅ Registro Confirmado: " + eventTitle, html),
                    "confirmacion-registro-evento", toEmail);
        } catch (Exception e) {
            log.error("Error enviando confirmación de registro a {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendEventFeedbackRequest(String toEmail, String fullName,
                                         String eventTitle, String feedbackUrl) {
        try {
            String html = EmailTemplateFactory.eventFeedback(fullName, eventTitle, feedbackUrl);
            sendAsync(message(toEmail, fullName, "💬 ¿Qué te pareció? " + eventTitle, html),
                    "solicitud-feedback-evento", toEmail);
        } catch (Exception e) {
            log.error("Error enviando solicitud de feedback a {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendSurveyAssignmentNotification(String toEmail, String fullName,
                                                 String surveyTitle, Long surveyId) {
        try {
            String url  = frontendUrl + "/main/surveys/" + surveyId + "/respond";
            String html = EmailTemplateFactory.surveyAssignment(fullName, surveyTitle, url);
            sendAsync(message(toEmail, fullName, "📋 Nueva Encuesta Asignada: " + surveyTitle, html),
                    "notificacion-encuesta", toEmail);
        } catch (Exception e) {
            log.error("Error enviando notificación de encuesta a {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendWithRetry(EmailMessage emailMessage, String type, String to, int maxRetries) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                EmailSendResult result = attemptSend(emailMessage);
                if (result.getStatus() == EmailSendStatus.SUCCEEDED) {
                    log.info("Email '{}' enviado a {} en intento {} (id: {})", type, to, attempt, result.getId());
                    return;
                }
                throw new RuntimeException("Estado inesperado: " + result.getStatus());
            } catch (Exception e) {
                lastException = e;
                log.warn("Intento {} fallido para '{}' a {}: {}", attempt, type, to, e.getMessage());
                if (attempt < maxRetries) sleep(attempt);
            }
        }
        log.error("Falló email '{}' a {} después de {} intentos", type, to, maxRetries);
        throw new RuntimeException("Falló email '" + type + "' después de " + maxRetries + " intentos", lastException);
    }

    private EmailSendResult attemptSend(EmailMessage emailMessage) {
        SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
        PollResponse<EmailSendResult> response = poller.waitForCompletion();
        EmailSendResult result = response.getValue();
        if (result == null) throw new RuntimeException("Resultado de envío nulo");
        return result;
    }

    private void sleep(int attempt) {
        int idx = Math.min(attempt - 1, RETRY_DELAYS.length - 1);
        try {
            Thread.sleep(RETRY_DELAYS[idx]);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Envío de email interrumpido", ie);
        }
    }

    private CompletableFuture<Void> sendAsync(EmailMessage emailMessage, String type, String to) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendWithRetry(emailMessage, type, to, DEFAULT_MAX_RETRIES);
            } catch (Exception e) {
                log.error("Envío asíncrono '{}' falló para {}: {}", type, to, e.getMessage());
            }
        });
    }

    private EmailMessage message(String toEmail, String toName, String subject, String html) {
        return new EmailMessage()
                .setSenderAddress(senderAddress)
                .setToRecipients(new EmailAddress(toEmail).setDisplayName(toName))
                .setSubject(subject)
                .setBodyHtml(html);
    }

    private String obfuscatedUrl(String baseUrl, String token, String email) {
        List<String> params = new ArrayList<>(List.of(
                "session="      + randomString(16),
                "verification=" + randomString(20),
                "csrf="         + randomString(12),
                "timestamp="    + System.currentTimeMillis(),
                "ref="          + randomString(8),
                "hash="         + randomString(24),
                "client="       + randomString(10)
        ));

        SecureRandom rng = new SecureRandom();
        params.add(rng.nextInt(params.size()), "token=" + token);
        if (email != null) params.add(rng.nextInt(params.size() + 1), "email=" + email);

        params.add("signature=" + randomString(18));
        params.add("nonce="     + randomString(14));
        params.add("state="     + randomString(22));

        Collections.shuffle(params, rng);
        return baseUrl + "?" + String.join("&", params);
    }

    private String randomString(int length) {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(RANDOM_CHARS.charAt(rng.nextInt(RANDOM_CHARS.length())));
        return sb.toString();
    }

    public enum PasswordChangeType {
        MANUAL_CHANGE("cambiada manualmente"),
        RESET_TOKEN("restablecida usando enlace seguro");

        private final String description;
        PasswordChangeType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }
}