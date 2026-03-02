package com.nexus.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired private JavaMailSender mailSender;
    @Value("${nexus.mail.from:somosnexusapp@gmail.com}") private String fromEmail;
    @Value("${nexus.frontend.url:http://localhost:4200}") private String frontendUrl;

    // --- PLANTILLA MAESTRA HTML (Diseño Premium Nexus) ---
    private final String HTML_WRAPPER = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus App</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    background-color: #f3f4f6;
                    margin: 0;
                    padding: 40px 20px;
                    -webkit-font-smoothing: antialiased;
                }
                .wrapper {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 16px;
                    overflow: hidden;
                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
                }
                .header {
                    background-color: #121212;
                    padding: 32px 40px;
                    text-align: center;
                    border-bottom: 4px solid #0a84ff;
                }
                .header h1 {
                    margin: 0;
                    color: #ffffff;
                    font-size: 28px;
                    letter-spacing: 2px;
                    font-weight: 800;
                }
                .content {
                    padding: 40px;
                    color: #374151;
                    line-height: 1.6;
                    font-size: 16px;
                }
                .content h2 {
                    color: #111827;
                    font-size: 22px;
                    margin-top: 0;
                    margin-bottom: 20px;
                }
                .code-container {
                    background-color: #eff6ff;
                    border: 1px solid #bfdbfe;
                    border-radius: 12px;
                    padding: 24px;
                    text-align: center;
                    margin: 32px 0;
                }
                .code-box {
                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                    font-size: 36px;
                    font-weight: 700;
                    color: #0a84ff;
                    letter-spacing: 8px;
                    margin: 0;
                }
                .btn {
                    display: inline-block;
                    background-color: #0a84ff;
                    color: #ffffff !important;
                    text-decoration: none;
                    padding: 16px 32px;
                    border-radius: 8px;
                    font-weight: 600;
                    font-size: 16px;
                    text-align: center;
                    margin: 24px 0;
                    transition: background-color 0.2s;
                }
                .data-card {
                    background-color: #f9fafb;
                    border-radius: 12px;
                    padding: 20px;
                    margin: 24px 0;
                    border: 1px solid #e5e7eb;
                }
                .data-row {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 12px;
                    border-bottom: 1px solid #e5e7eb;
                    padding-bottom: 12px;
                }
                .data-row:last-child {
                    margin-bottom: 0;
                    border-bottom: none;
                    padding-bottom: 0;
                }
                .footer {
                    background-color: #f9fafb;
                    padding: 32px 40px;
                    text-align: center;
                    font-size: 13px;
                    color: #6b7280;
                    border-top: 1px solid #e5e7eb;
                }
                .footer p { margin: 8px 0; }
                .text-muted { color: #6b7280; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="wrapper">
                <div class="header">
                    <h1>NEXUS</h1>
                </div>
                <div class="content">
                    %s
                </div>
                <div class="footer">
                    <p>Has recibido este correo electrónico porque estás registrado en Nexus App.</p>
                    <p>Por favor, no respondas a este mensaje, es un envío automático.</p>
                    <p style="margin-top: 16px; font-weight: 600;">&copy; 2026 Nexus App S.L. Todos los derechos reservados.</p>
                </div>
            </div>
        </body>
        </html>
        """;

    @Async
    public void enviarEmail(String to, String subject, String body) {
        try { 
            SimpleMailMessage m = new SimpleMailMessage(); 
            m.setFrom(fromEmail); 
            m.setTo(to); 
            m.setSubject(subject); 
            m.setText(body); 
            mailSender.send(m); 
            System.out.println("✅ [NEXUS EMAIL] Correo texto plano enviado a: " + to);
        } catch(Exception e) {
            System.err.println("❌ [NEXUS EMAIL] Error enviando a " + to + ": " + e.getMessage());
        }
    }

    @Async
    public void enviarEmailHtml(String to, String subject, String htmlContent) {
        try { 
            MimeMessage m = mailSender.createMimeMessage(); 
            MimeMessageHelper h = new MimeMessageHelper(m, true, "UTF-8"); 
            h.setFrom(fromEmail); 
            h.setTo(to); 
            h.setSubject(subject); 
            
            // Inyectamos el contenido específico dentro de la plantilla maestra
            String finalHtml = String.format(HTML_WRAPPER, htmlContent);
            h.setText(finalHtml, true); 
            
            mailSender.send(m); 
            System.out.println("✅ [NEXUS EMAIL] Correo HTML enviado con éxito a: " + to);
        } catch(Exception e) {
            System.err.println("❌ [NEXUS EMAIL] Falla crítica al enviar a " + to + ". Causa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── 1. REGISTRO (Verificación de correo) ──────────────────────────────────
    @Async
    public void enviarVerificacion(String to, String username, String codigo) {
        String contenido = 
            "<h2>¡Te damos la bienvenida a Nexus!</h2>" +
            "<p>Hola <b>" + username + "</b>,</p>" +
            "<p>Estamos encantados de tenerte con nosotros. Para empezar a comprar y vender con total seguridad, necesitamos verificar tu dirección de correo electrónico.</p>" +
            "<div class='code-container'>" +
            "   <p style='margin-top:0; color:#6b7280; font-size:14px; text-transform:uppercase;'>TU CÓDIGO DE VERIFICACIÓN</p>" +
            "   <p class='code-box'>" + codigo + "</p>" +
            "</div>" +
            "<p class='text-muted'>Este código caducará en 30 minutos por motivos de seguridad.</p>";
            
        enviarEmailHtml(to, "Verifica tu cuenta en Nexus", contenido);
    }

    // ── 2. RESETEAR CONTRASEÑA ────────────────────────────────────────────────
    @Async
    public void enviarResetPassword(String to, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        String contenido = 
            "<h2>Restablecimiento de contraseña</h2>" +
            "<p>Hemos recibido una solicitud para cambiar la contraseña de tu cuenta de Nexus asociada a este correo.</p>" +
            "<div style='text-align: center;'>" +
            "   <a href='" + link + "' class='btn'>Cambiar mi contraseña</a>" +
            "</div>" +
            "<p>Si el botón no funciona, copia y pega el siguiente enlace en tu navegador:</p>" +
            "<p style='word-break: break-all; font-size: 13px; color: #0a84ff;'>" + link + "</p>" +
            "<p class='text-muted' style='margin-top: 32px; border-top: 1px solid #eee; padding-top: 16px;'>" +
            "Si no has solicitado este cambio, tu cuenta sigue siendo segura y puedes ignorar este mensaje.</p>";

        enviarEmailHtml(to, "Restablece tu contraseña - Nexus", contenido);
    }

    // ── 3. AUTENTICACIÓN EN DOS PASOS (2FA) ───────────────────────────────────
    @Async
    public void enviarOtp2FA(String to, String otp, String motivo) {
        String contenido = 
            "<h2>Alerta de Seguridad (2FA)</h2>" +
            "<p>Se ha detectado un intento de <b>" + motivo + "</b>.</p>" +
            "<p>Para confirmar que eres tú, introduce el siguiente código de un solo uso en la aplicación:</p>" +
            "<div class='code-container'>" +
            "   <p class='code-box'>" + otp + "</p>" +
            "</div>" +
            "<p class='text-muted'>Este código expirará en 10 minutos. <b>IMPORTANTE: Nunca compartas este código con nadie</b>, ni siquiera con empleados de Nexus.</p>";

        enviarEmailHtml(to, "Código de seguridad 2FA - Nexus", contenido);
    }

    @Async 
    public void enviarOtpDosFactores(String to, String otp) { 
        enviarOtp2FA(to, otp, "iniciar sesión en tu cuenta"); 
    }

    // ── 4. CONFIRMACIÓN DE COMPRA ─────────────────────────────────────────────
    @Async
    public void enviarConfirmacionCompra(String to, String titulo, Double precio) {
        String contenido = 
            "<h2>¡Pago confirmado! 🎉</h2>" +
            "<p>Tu compra se ha procesado correctamente. El vendedor ha sido notificado y preparará tu envío lo antes posible.</p>" +
            "<div class='data-card'>" +
            "   <div class='data-row'>" +
            "       <span style='color:#6b7280'>Artículo:</span>" +
            "       <span style='font-weight:600'>" + titulo + "</span>" +
            "   </div>" +
            "   <div class='data-row'>" +
            "       <span style='color:#6b7280'>Total pagado:</span>" +
            "       <span style='font-weight:700; color:#0a84ff; font-size:18px'>" + String.format("%.2f €", precio) + "</span>" +
            "   </div>" +
            "</div>" +
            "<p>Podrás hacer el seguimiento desde el apartado 'Mis Compras' en tu perfil.</p>";

        enviarEmailHtml(to, "Recibo de tu compra: " + titulo, contenido);
    }

    // ── 5. NOTIFICACIÓN DE ENVÍO ──────────────────────────────────────────────
    @Async
    public void enviarNotificacionEnvio(String to, String titulo, String tracking, String transportista) {
        String trackingInfo = "";
        if (tracking != null && !tracking.isEmpty()) {
            trackingInfo = 
            "<div class='data-row'>" +
            "   <span style='color:#6b7280'>Transportista:</span>" +
            "   <span style='font-weight:600'>" + transportista + "</span>" +
            "</div>" +
            "<div class='data-row'>" +
            "   <span style='color:#6b7280'>Nº Seguimiento:</span>" +
            "   <span style='font-weight:600; font-family:monospace'>" + tracking + "</span>" +
            "</div>";
        }
            
        String contenido = 
            "<h2>Tu paquete está en camino 📦</h2>" +
            "<p>¡Buenas noticias! El vendedor acaba de enviar tu artículo.</p>" +
            "<div class='data-card'>" +
            "   <div class='data-row'>" +
            "       <span style='color:#6b7280'>Artículo:</span>" +
            "       <span style='font-weight:600'>" + titulo + "</span>" +
            "   </div>" + trackingInfo +
            "</div>" +
            "<p>Gracias por confiar en la comunidad de Nexus.</p>";

        enviarEmailHtml(to, "Tu pedido ha sido enviado", contenido);
    }
}