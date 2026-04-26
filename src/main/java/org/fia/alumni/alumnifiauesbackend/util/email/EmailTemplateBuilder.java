package org.fia.alumni.alumnifiauesbackend.util.email;

public class EmailTemplateBuilder {

    private static final String UES_RED        = "#8b0e13";
    private static final String UES_RED_DARK   = "#6b0a0f";
    private static final String UES_RED_LIGHT  = "#b01018";
    private static final String WHITE          = "#ffffff";
    private static final String GRAY_BG        = "#f5f5f5";
    private static final String GRAY_LIGHT     = "#f8f9fa";
    private static final String GRAY_BORDER    = "#e8e8e8";
    private static final String TEXT_DARK      = "#1f2937";
    private static final String TEXT_MID       = "#4b5563";
    private static final String TEXT_LIGHT     = "#9ca3af";

    private EmailTemplateBuilder() {}

    public static String build(String title, String fullName, String content,
                               String buttonText, String buttonUrl,
                               String extraInfo, String warning) {
        return String.format("""
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: %s;
                             margin: 0; padding: 20px; background-color: %s;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: %s;
                                border-radius: 10px; overflow: hidden;
                                box-shadow: 0 4px 20px rgba(139,14,19,0.15);">
                        %s
                        <div style="padding: 35px 30px;">
                            <p style="margin-bottom: 20px;">
                                Estimado/a <strong style="color: %s;">%s</strong>,
                            </p>
                            <p style="margin-bottom: 20px;">%s</p>

                            <div style="text-align: center; margin: 35px 0;">
                                <a href="%s"
                                   style="display: inline-block; background: linear-gradient(135deg, %s, %s);
                                          color: %s; padding: 14px 32px; text-decoration: none;
                                          border-radius: 6px; font-weight: bold; font-size: 15px;
                                          box-shadow: 0 3px 10px rgba(139,14,19,0.3);">
                                    %s
                                </a>
                            </div>

                            %s
                            %s
                            %s
                        </div>
                    </div>
                </body>
                </html>
                """,
                TEXT_DARK, GRAY_BG, WHITE,
                buildHeader(title),
                UES_RED, fullName,
                content,
                buttonUrl, UES_RED, UES_RED_LIGHT, WHITE,
                buttonText,
                extraInfo.isEmpty() ? "" :
                        "<p style=\"font-size: 14px; color:" + TEXT_MID + ";\">" + extraInfo + "</p>",
                warning.isEmpty() ? "" :
                        "<p style=\"margin-top: 20px;\"><strong style=\"color:" + UES_RED + ";\">" + warning + "</strong></p>",
                buildFooter("Equipo Alumni FIA UES")
        );
    }

    public static String buildHeader(String title) {
        return String.format("""
                <div style="background: linear-gradient(135deg, %s, %s);
                            color: %s; text-align: center; padding: 35px 20px; position: relative;">
                    <div style="margin-bottom: 15px;">
                        <img src="https://alumnifiarepository.blob.core.windows.net/alumni-images/logos/Logo_UES-removebg-preview.png"
                             alt="Logo UES"
                             style="height: 70px; width: auto; filter: brightness(0) invert(1);" />
                    </div>
                    <h2 style="margin: 0; color: %s; font-size: 22px; font-weight: 700; letter-spacing: 0.5px;">%s</h2>
                    <p style="margin: 6px 0 0 0; font-size: 13px; color: rgba(255,255,255,0.8);">
                        Alumni FIA - Universidad de El Salvador
                    </p>
                </div>
                <div style="height: 4px; background: linear-gradient(90deg, %s, %s, %s);"></div>
                """,
                UES_RED, UES_RED_DARK,
                WHITE,
                WHITE, title,
                UES_RED_DARK, UES_RED, UES_RED_LIGHT
        );
    }

    public static String buildFooter(String team) {
        return String.format("""
                <div style="border-top: 2px solid %s; padding-top: 20px; margin-top: 30px;">
                    <p style="margin: 0; font-size: 14px; color: %s;">
                        Saludos cordiales,<br>
                        <strong style="color: %s;">%s</strong>
                    </p>
                    <p style="margin: 10px 0 0 0; font-size: 12px; color: %s;">
                        © 2025 Alumni FIA-UES. Universidad de El Salvador.<br>
                        Este es un correo automático, por favor no responder.
                    </p>
                </div>
                """,
                UES_RED, TEXT_MID,
                UES_RED, team,
                TEXT_LIGHT
        );
    }

    public static String buildInfoBox(String title, String titleColor, String bgColor,
                                      String borderColor, String content) {
        return String.format("""
                <div style="background-color: %s; padding: 15px; border-radius: 6px;
                            border-left: 4px solid %s; margin: 20px 0;">
                    <h4 style="margin: 0 0 10px 0; color: %s;">%s</h4>
                    <div style="color: %s; font-size: 14px;">%s</div>
                </div>
                """,
                bgColor, borderColor, titleColor, title, TEXT_DARK, content);
    }

    public static String buildWarningBox(String title, String content) {
        return buildInfoBox(title, "#856404", "#fff8e6", "#f59e0b", content);
    }

    public static String buildSuccessBox(String title, String content) {
        return buildInfoBox(title, "#155724", "#d4edda", "#36d399", content);
    }

    public static String buildDangerBox(String title, String content) {
        return buildInfoBox(title, "#721c24", "#f8d7da", "#f87272", content);
    }

    public static String buildDetailsList(String... items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                <div style="background-color: %s; padding: 15px; border-radius: 6px;
                            border-left: 4px solid %s; margin: 20px 0;">
                    <ul style="margin: 0; padding-left: 20px; color: %s;">
                """, "#f5f5f5", "#8b0e13", "#1f2937"));
        for (String item : items) {
            if (item != null && !item.isEmpty()) {
                sb.append("<li style=\"margin-bottom: 6px;\">").append(item).append("</li>");
            }
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    public static String buildButtonRow(String... buttons) {
        StringBuilder sb = new StringBuilder(
                "<div style=\"text-align: center; margin: 30px 0; display: flex; " +
                        "justify-content: center; gap: 12px; flex-wrap: wrap;\">");
        for (String btn : buttons) {
            sb.append(btn);
        }
        sb.append("</div>");
        return sb.toString();
    }

    public static String buildPrimaryButton(String label, String url) {
        return String.format(
                "<a href=\"%s\" style=\"display: inline-block; background: linear-gradient(135deg, %s, %s); " +
                        "color: %s; padding: 12px 24px; text-decoration: none; border-radius: 6px; " +
                        "font-weight: bold; margin: 4px;\">%s</a>",
                url, "#8b0e13", "#b01018", "#ffffff", label);
    }

    public static String buildSecondaryButton(String label, String url) {
        return String.format(
                "<a href=\"%s\" style=\"display: inline-block; background-color: %s; " +
                        "color: %s; padding: 12px 24px; text-decoration: none; border-radius: 6px; " +
                        "font-weight: bold; margin: 4px; border: 2px solid %s;\">%s</a>",
                url, "#ffffff", "#8b0e13", "#8b0e13", label);
    }

    public static String buildDangerButton(String label, String url) {
        return String.format(
                "<a href=\"%s\" style=\"display: inline-block; background-color: %s; " +
                        "color: %s; padding: 12px 24px; text-decoration: none; border-radius: 6px; " +
                        "font-weight: bold; margin: 4px;\">%s</a>",
                url, "#f87272", "#ffffff", label);
    }

    public static String buildSuccessButton(String label, String url) {
        return String.format(
                "<a href=\"%s\" style=\"display: inline-block; background-color: %s; " +
                        "color: %s; padding: 12px 24px; text-decoration: none; border-radius: 6px; " +
                        "font-weight: bold; margin: 4px;\">%s</a>",
                url, "#36d399", "#ffffff", label);
    }
}