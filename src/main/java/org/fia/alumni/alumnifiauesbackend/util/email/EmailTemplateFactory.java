package org.fia.alumni.alumnifiauesbackend.util.email;

import org.fia.alumni.alumnifiauesbackend.service.email.EmailService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailTemplateFactory {

    private static final String DATE_PATTERN = "dd MMM yyyy 'a las' HH:mm:ss";

    private EmailTemplateFactory() {}

    public static String verification(String fullName, String verificationUrl) {
        return EmailTemplateBuilder.build(
                "Verificación de Cuenta",
                fullName,
                "Gracias por registrarte en Alumni FIA UES. Por favor verifica tu dirección de correo electrónico:",
                "✉️ Verificar Correo",
                verificationUrl,
                "O copia este enlace: " + verificationUrl,
                "Expira en 24 horas"
        );
    }

    public static String welcome(String fullName, String loginUrl) {
        return EmailTemplateBuilder.build(
                "¡Bienvenido a Alumni FIA UES!",
                fullName,
                "¡Tu email ha sido verificado exitosamente! 🎉<br>Bienvenido a la comunidad Alumni FIA UES.",
                "🚀 Acceder a la Plataforma",
                loginUrl,
                "", ""
        );
    }

    public static String passwordReset(String fullName, String resetUrl) {
        return EmailTemplateBuilder.build(
                "Restablecer Contraseña",
                fullName,
                "Has solicitado restablecer tu contraseña de Alumni FIA UES.<br>" +
                        "Haz clic en el botón a continuación para crear una nueva contraseña:",
                "🔑 Restablecer Contraseña",
                resetUrl,
                "⚠️ Este enlace expira en 1 hora por seguridad.",
                "Si no solicitaste este cambio, ignora este correo y tu contraseña permanecerá sin cambios."
        );
    }

    public static String passwordChanged(String fullName,
                                         EmailService.PasswordChangeType changeType,
                                         String ipAddress, String userAgent,
                                         String loginUrl, String resetUrl) {
        String changeDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
        String changeMethod = changeType == EmailService.PasswordChangeType.MANUAL_CHANGE
                ? "a través de configuración de cuenta"
                : "utilizando enlace de restablecimiento";
        String deviceInfo = buildDeviceInfo(ipAddress, userAgent);

        String detailsList = EmailTemplateBuilder.buildDetailsList(
                "<strong>Fecha y Hora:</strong> " + changeDateTime,
                "<strong>Método:</strong> Contraseña " + changeMethod,
                deviceInfo
        );

        String securityBox = EmailTemplateBuilder.buildWarningBox(
                "🔒 Aviso de Seguridad",
                "Si no realizaste este cambio, contacta inmediatamente al soporte."
        );

        String buttons = EmailTemplateBuilder.buildButtonRow(
                EmailTemplateBuilder.buildPrimaryButton("Iniciar Sesión", loginUrl),
                EmailTemplateBuilder.buildDangerButton("No Fui Yo", resetUrl)
        );

        String body = String.format("""
                <p style="margin-bottom: 20px;">Estimado/a <strong style="color: #8b0e13;">%s</strong>,</p>
                <p style="margin-bottom: 20px;">Tu contraseña de Alumni FIA UES fue
                    <strong>%s</strong> exitosamente %s.</p>
                %s
                %s
                %s
                %s
                """,
                fullName,
                changeType.getDescription(),
                changeMethod,
                detailsList,
                securityBox,
                buttons,
                EmailTemplateBuilder.buildFooter("Equipo de Seguridad Alumni FIA UES")
        );

        return wrapBody(EmailTemplateBuilder.buildHeader("Contraseña Cambiada"), body);
    }

    public static String verificationApproved(String fullName, String loginUrl) {
        return EmailTemplateBuilder.build(
                "Verificación Exitosa",
                fullName,
                "¡Tu cuenta ha sido verificada exitosamente! Ya puedes acceder a todas las funcionalidades.",
                "🚀 Acceder a la Plataforma",
                loginUrl, "", ""
        );
    }

    public static String verificationRejected(String fullName, String observaciones,
                                              String contactUrl, String supportEmail) {
        String obs = (observaciones != null && !observaciones.isBlank())
                ? observaciones
                : "Se requiere verificar coincidencia de datos.";

        String obsBox = EmailTemplateBuilder.buildWarningBox("Observaciones:", obs);
        String deadlineBox = EmailTemplateBuilder.buildDangerBox(
                "Plazo: 7 días",
                "Después de este plazo tu cuenta será desactivada temporalmente."
        );
        String buttons = EmailTemplateBuilder.buildButtonRow(
                EmailTemplateBuilder.buildPrimaryButton("Contactar Soporte", contactUrl),
                EmailTemplateBuilder.buildSecondaryButton("Enviar Email", "mailto:" + supportEmail)
        );

        String body = String.format("""
                <p>Estimado/a <strong style="color: #8b0e13;">%s</strong>,</p>
                <p>Se requiere documentación adicional para completar tu verificación.</p>
                %s
                %s
                %s
                %s
                """,
                fullName, obsBox, deadlineBox, buttons,
                EmailTemplateBuilder.buildFooter("Equipo de Verificación Alumni FIA UES")
        );

        return wrapBody(EmailTemplateBuilder.buildHeader("Verificación Pendiente"), body);
    }

    public static String profileUpdate(String fullName, String loginUrl) {
        return EmailTemplateBuilder.build(
                "Perfil Actualizado",
                fullName,
                "Tu perfil en Alumni FIA UES ha sido actualizado exitosamente. " +
                        "Si no realizaste estos cambios, contacta inmediatamente a soporte.",
                "🔐 Revisar Cuenta",
                loginUrl, "", ""
        );
    }

    public static String accountDeactivation(String fullName, String reason, String reactivationUrl) {
        String reasonBox = EmailTemplateBuilder.buildDangerBox(
                "Razón de desactivación:",
                reason
        );
        String buttons = EmailTemplateBuilder.buildButtonRow(
                EmailTemplateBuilder.buildSuccessButton("🔄 Reactivar Cuenta", reactivationUrl)
        );

        String body = String.format("""
                <p>Estimado/a <strong style="color: #8b0e13;">%s</strong>,</p>
                <p>Tu cuenta en Alumni FIA UES ha sido <strong style="color: #f87272;">desactivada</strong>
                   por la siguiente razón:</p>
                %s
                <p>Puedes reactivar tu cuenta en cualquier momento.</p>
                %s
                %s
                """,
                fullName, reasonBox, buttons,
                EmailTemplateBuilder.buildFooter("Equipo Alumni FIA UES")
        );

        return wrapBody(EmailTemplateBuilder.buildHeader("Cuenta Desactivada"), body);
    }

    public static String accountReactivation(String fullName, String reactivationUrl) {
        return EmailTemplateBuilder.build(
                "Reactivar Cuenta",
                fullName,
                "Has solicitado reactivar tu cuenta de Alumni FIA UES.<br>" +
                        "Haz clic en el botón a continuación para reactivar tu cuenta:",
                "🔄 Reactivar Cuenta",
                reactivationUrl,
                "⚠️ Este enlace expira en 24 horas por seguridad.",
                "Si no solicitaste esta reactivación, puedes ignorar este correo."
        );
    }

    public static String reactivationConfirmation(String fullName, String userAgent,
                                                  String ipAddress, String loginUrl,
                                                  String supportUrl) {
        String changeDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
        String deviceInfo = buildDeviceInfo(ipAddress, userAgent);

        String successBox = EmailTemplateBuilder.buildSuccessBox(
                "✅ Cuenta Activa",
                "Ya puedes acceder a todas las funcionalidades de la plataforma."
        );
        String detailsList = EmailTemplateBuilder.buildDetailsList(
                "<strong>Fecha y Hora:</strong> " + changeDateTime,
                "<strong>Método:</strong> Enlace de reactivación por email",
                deviceInfo
        );
        String securityBox = EmailTemplateBuilder.buildWarningBox(
                "🔒 Aviso de Seguridad",
                "Si no solicitaste esta reactivación, contacta inmediatamente al soporte."
        );
        String buttons = EmailTemplateBuilder.buildButtonRow(
                EmailTemplateBuilder.buildSuccessButton("🚀 Iniciar Sesión", loginUrl),
                EmailTemplateBuilder.buildDangerButton("🚨 No Fui Yo", supportUrl)
        );

        String body = String.format("""
                <p style="margin-bottom: 20px;">Estimado/a <strong style="color: #8b0e13;">%s</strong>,</p>
                <p style="margin-bottom: 20px;">Tu cuenta de Alumni FIA UES ha sido
                    <strong style="color: #36d399;">reactivada exitosamente</strong>. 🎉</p>
                %s
                %s
                %s
                %s
                %s
                """,
                fullName, successBox, detailsList, securityBox, buttons,
                EmailTemplateBuilder.buildFooter("Equipo de Seguridad Alumni FIA UES")
        );

        return wrapBody(EmailTemplateBuilder.buildHeader("Cuenta Reactivada"), body);
    }

    public static String eventNotification(String fullName, String eventTitle,
                                           String eventDescription, String eventUrl) {
        String shortDescription = eventDescription.length() > 150
                ? eventDescription.substring(0, 147) + "..."
                : eventDescription;

        return EmailTemplateBuilder.build(
                "¡Nuevo Evento!",
                fullName,
                String.format("¡Te invitamos a un nuevo evento: <strong>%s</strong>!<br><br>%s",
                        eventTitle, shortDescription),
                "Ver Detalles y Registrarse",
                eventUrl,
                "No te pierdas esta oportunidad de conectar y aprender.",
                ""
        );
    }

    public static String eventRegistrationConfirmation(String fullName, String eventTitle, String eventUrl) {
        return EmailTemplateBuilder.build(
                "Registro Confirmado",
                fullName,
                String.format("¡Tu registro para el evento <strong>%s</strong> ha sido confirmado! 🎉<br>" +
                        "Te esperamos.", eventTitle),
                "Ver Detalles del Evento",
                eventUrl,
                "Puedes gestionar tus registros desde tu perfil.",
                ""
        );
    }

    public static String eventFeedback(String fullName, String eventTitle, String feedbackUrl) {
        return EmailTemplateBuilder.build(
                "¡Gracias por Asistir!",
                fullName,
                String.format("Gracias por participar en <strong>%s</strong>. " +
                        "Nos encantaría conocer tu opinión.", eventTitle),
                "Dejar Feedback (1 min)",
                feedbackUrl,
                "Tu feedback es vital para mejorar futuros eventos.",
                ""
        );
    }

    public static String surveyAssignment(String fullName, String surveyTitle, String surveyUrl) {
        return EmailTemplateBuilder.build(
                "Nueva Encuesta Disponible",
                fullName,
                String.format("Has sido seleccionado para participar en la encuesta: <strong>%s</strong>.<br>" +
                        "Tu opinión y experiencia son muy importantes para nuestra comunidad Alumni.", surveyTitle),
                "Completar Encuesta",
                surveyUrl,
                "Te tomará solo unos minutos. Tus respuestas nos ayudan a mejorar.",
                ""
        );
    }

    private static String buildDeviceInfo(String ipAddress, String userAgent) {
        StringBuilder sb = new StringBuilder();
        if (ipAddress != null && !ipAddress.isBlank()) {
            sb.append("<strong>Dirección IP:</strong> ").append(ipAddress);
        }
        if (userAgent != null && !userAgent.isBlank()) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("<strong>Dispositivo:</strong> ").append(extractBrowserInfo(userAgent));
        }
        return sb.toString();
    }

    private static String extractBrowserInfo(String userAgent) {
        if (userAgent.contains("Chrome"))  return "Navegador Chrome";
        if (userAgent.contains("Firefox")) return "Navegador Firefox";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Navegador Safari";
        if (userAgent.contains("Edge"))    return "Microsoft Edge";
        return "Navegador Desconocido";
    }

    private static String wrapBody(String header, String bodyContent) {
        return String.format("""
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #1f2937;
                             margin: 0; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff;
                                border-radius: 10px; overflow: hidden;
                                box-shadow: 0 4px 20px rgba(139,14,19,0.15);">
                        %s
                        <div style="padding: 35px 30px;">
                            %s
                        </div>
                    </div>
                </body>
                </html>
                """, header, bodyContent);
    }
}