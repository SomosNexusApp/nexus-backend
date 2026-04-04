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

    // ─── PLANTILLA MAESTRA ─────────────────────────────────────────────────────
    // IMPORTANTE: NO usar String.format() con esta plantilla porque cualquier
    // carácter '%' en el htmlContent causaría MissingFormatArgumentException.
    // En su lugar se usa concatenación directa entre HTML_HEADER y HTML_FOOTER.

    private String buildHtmlEmail(String htmlContent) {
        return "<!DOCTYPE html>" +
            "<html lang=\"es\">" +
            "<head>" +
            "  <meta charset=\"UTF-8\">" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "  <title>Nexus Elite</title>" +
            "  <style>" +
            "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;" +
            "           background-color: #0f172a; margin: 0; padding: 40px 20px; -webkit-font-smoothing: antialiased; }" +
            "    .wrapper { max-width: 620px; margin: 0 auto; background-color: #ffffff;" +
            "               border-radius: 20px; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.4); }" +
            "    .header { background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 50%, #1e1b4b 100%);" +
            "              padding: 40px; text-align: center; border-bottom: 3px solid #7c3aed; }" +
            "    .header h1 { margin: 8px 0 0; color: #ffffff; font-size: 28px; letter-spacing: 6px;" +
            "                 font-weight: 900; text-transform: uppercase; }" +
            "    .header p { margin: 8px 0 0; color: #a5b4fc; font-size: 13px; letter-spacing: 2px; text-transform: uppercase; }" +
            "    .content { padding: 48px 40px; color: #1e293b; line-height: 1.8; font-size: 16px; }" +
            "    .content h2 { color: #0f172a; font-size: 26px; margin-top: 0; margin-bottom: 8px; font-weight: 800; }" +
            "    .content h3 { color: #1e293b; font-size: 19px; margin-top: 28px; margin-bottom: 12px; font-weight: 700; }" +
            "    .lead { font-size: 17px; color: #334155; margin-bottom: 24px; }" +
            "    .code-container { background: linear-gradient(135deg, #f5f3ff, #ede9fe);" +
            "                      border: 1px solid #c4b5fd; border-radius: 16px; padding: 32px;" +
            "                      text-align: center; margin: 32px 0; }" +
            "    .code-label { margin:0; color: #7c3aed; font-size: 12px; letter-spacing: 3px;" +
            "                  text-transform: uppercase; font-weight: 700; margin-bottom: 12px; }" +
            "    .code-box { font-family: 'Fira Code', monospace; font-size: 44px; font-weight: 900;" +
            "                color: #4f46e5; letter-spacing: 12px; margin: 0; }" +
            "    .btn { display: inline-block; background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%);" +
            "           color: #ffffff !important; text-decoration: none; padding: 18px 40px;" +
            "           border-radius: 12px; font-weight: 700; font-size: 16px; text-align: center;" +
            "           margin: 24px 0; box-shadow: 0 8px 24px rgba(124,58,237,0.3);" +
            "           letter-spacing: 0.5px; }" +
            "    .btn-success { background: linear-gradient(135deg, #059669 0%, #047857 100%);" +
            "                   box-shadow: 0 8px 24px rgba(5,150,105,0.3); }" +
            "    .btn-warning { background: linear-gradient(135deg, #d97706 0%, #b45309 100%);" +
            "                   box-shadow: 0 8px 24px rgba(217,119,6,0.3); }" +
            "    .data-card { background-color: #f8fafc; border-radius: 16px; padding: 28px;" +
            "                 margin: 24px 0; border: 1px solid #e2e8f0; }" +
            "    .data-row { display: flex; justify-content: space-between; align-items: center;" +
            "                margin-bottom: 14px; border-bottom: 1px solid #f1f5f9; padding-bottom: 14px; }" +
            "    .data-row:last-child { margin-bottom: 0; border-bottom: none; padding-bottom: 0; }" +
            "    .data-label { color: #64748b; font-size: 14px; }" +
            "    .data-value { font-weight: 600; color: #1e293b; font-size: 15px; }" +
            "    .data-value.highlight { color: #7c3aed; font-size: 20px; font-weight: 800; }" +
            "    .data-value.green { color: #059669; }" +
            "    .alert-box { border-radius: 12px; padding: 20px 24px; margin: 24px 0; display: flex; gap: 12px; }" +
            "    .alert-info { background: #eff6ff; border-left: 4px solid #3b82f6; }" +
            "    .alert-success { background: #f0fdf4; border-left: 4px solid #22c55e; }" +
            "    .alert-warning { background: #fffbeb; border-left: 4px solid #f59e0b; }" +
            "    .steps-list { padding: 0; list-style: none; margin: 20px 0; }" +
            "    .steps-list li { display: flex; gap: 14px; align-items: flex-start;" +
            "                     padding: 12px 0; border-bottom: 1px solid #f1f5f9; color: #334155; }" +
            "    .steps-list li:last-child { border-bottom: none; }" +
            "    .step-num { background: #7c3aed; color: white; border-radius: 50%; width: 26px; height: 26px;" +
            "                display: inline-flex; align-items: center; justify-content: center;" +
            "                font-size: 12px; font-weight: 800; flex-shrink: 0; margin-top: 1px; }" +
            "    .divider { border: none; border-top: 1px solid #e2e8f0; margin: 32px 0; }" +
            "    .text-muted { color: #64748b; font-size: 14px; line-height: 1.7; }" +
            "    .text-center { text-align: center; }" +
            "    .badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px;" +
            "             font-weight: 700; text-transform: uppercase; letter-spacing: 1px; }" +
            "    .badge-purple { background: #f5f3ff; color: #7c3aed; }" +
            "    .badge-green { background: #f0fdf4; color: #15803d; }" +
            "    .badge-orange { background: #fff7ed; color: #c2410c; }" +
            "    .footer { background: linear-gradient(135deg, #f8fafc, #f1f5f9); padding: 40px;" +
            "              text-align: center; font-size: 13px; color: #64748b; border-top: 1px solid #e2e8f0; }" +
            "    .footer-brand { font-size: 15px; font-weight: 800; color: #0f172a; letter-spacing: 2px;" +
            "                    text-transform: uppercase; margin-bottom: 8px; }" +
            "    .footer p { margin: 6px 0; }" +
            "    .footer a { color: #7c3aed; text-decoration: none; }" +
            "  </style>" +
            "</head>" +
            "<body>" +
            "  <div class=\"wrapper\">" +
            "    <div class=\"header\">" +
            "      <div style=\"display:inline-block; background:linear-gradient(135deg,#7c3aed,#4f46e5); border-radius:14px; padding:12px 18px; margin-bottom:16px;\">" +
            "        <span style=\"font-size:22px; font-weight:900; color:white; letter-spacing:3px;\">N</span>" +
            "      </div>" +
            "      <h1>NEXUS</h1>" +
            "      <p>Marketplace de confianza</p>" +
            "    </div>" +
            "    <div class=\"content\">" +
            htmlContent +
            "    </div>" +
            "    <div class=\"footer\">" +
            "      <p class=\"footer-brand\">Nexus &nbsp;·&nbsp; Elite Marketplace</p>" +
            "      <p>Has recibido este email porque tienes una cuenta en <strong>Nexus</strong>.</p>" +
            "      <p>Por favor, no respondas a este mensaje. Es un envío automático de nuestro sistema.</p>" +
            "      <p style=\"margin-top: 16px;\">" +
            "        <a href=\"" + frontendUrl + "/legal/privacidad\">Privacidad</a> &nbsp;·&nbsp; " +
            "        <a href=\"" + frontendUrl + "/legal/terminos\">Condiciones</a> &nbsp;·&nbsp; " +
            "        <a href=\"" + frontendUrl + "/ayuda\">Soporte</a>" +
            "      </p>" +
            "      <p style=\"margin-top: 16px; font-weight: 600; color: #94a3b8;\">" +
            "        &copy; 2026 Nexus App S.L. &nbsp;·&nbsp; Todos los derechos reservados." +
            "      </p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }

    @Async
    public void enviarEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(fromEmail);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            mailSender.send(m);
            System.out.println("✅ [NEXUS EMAIL] Texto plano enviado a: " + to);
        } catch (Exception e) {
            System.err.println("❌ [NEXUS EMAIL] Error enviando texto plano a " + to + ": " + e.getMessage());
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
            // IMPORTANTE: usar buildHtmlEmail() en vez de String.format para evitar
            // MissingFormatArgumentException cuando htmlContent tiene caracteres '%'
            String finalHtml = buildHtmlEmail(htmlContent);
            h.setText(finalHtml, true);
            mailSender.send(m);
            System.out.println("✅ [NEXUS EMAIL] HTML enviado a: " + to + " | Asunto: " + subject);
        } catch (Exception e) {
            System.err.println("❌ [NEXUS EMAIL] Error HTML a " + to + " [" + subject + "]: " + e.getMessage());
            e.printStackTrace();
            // Fallback a texto plano
            try {
                SimpleMailMessage fallback = new SimpleMailMessage();
                fallback.setFrom(fromEmail);
                fallback.setTo(to);
                fallback.setSubject(subject);
                fallback.setText(stripHtml(htmlContent));
                mailSender.send(fallback);
                System.out.println("✅ [NEXUS EMAIL] Fallback texto plano enviado a: " + to);
            } catch (Exception ex) {
                System.err.println("❌ [NEXUS EMAIL] Fallback también falló para " + to + ": " + ex.getMessage());
            }
        }
    }

    // ── 1. REGISTRO (Verificación de correo) ──────────────────────────────────
    @Async
    public void enviarVerificacion(String to, String username, String codigo) {
        String contenido =
            "<h2>¡Bienvenido a Nexus! 🎉</h2>" +
            "<p class=\"lead\">Hola <strong>" + escapeHtml(username) + "</strong>, nos alegra que estés aquí.</p>" +
            "<p>Para completar tu registro y empezar a comprar y vender en nuestra comunidad, necesitamos " +
            "verificar tu dirección de correo electrónico. Es un proceso rápido y seguro.</p>" +
            "<div class=\"code-container\">" +
            "  <p class=\"code-label\">Tu código de verificación</p>" +
            "  <p class=\"code-box\">" + escapeHtml(codigo) + "</p>" +
            "  <p class=\"text-muted\" style=\"margin-top: 16px;\">Este código caduca en <strong>30 minutos</strong>.</p>" +
            "</div>" +
            "<p class=\"text-muted\">Si no has creado esta cuenta, puedes ignorar este mensaje de forma segura. " +
            "Nadie podrá acceder a tu cuenta sin el código.</p>" +
            "<hr class=\"divider\">" +
            "<h3>¿Qué puedes hacer en Nexus?</h3>" +
            "<ul class=\"steps-list\">" +
            "  <li><span class=\"step-num\">1</span> <span>Compra y vende artículos de segunda mano de forma segura con pagos escrow.</span></li>" +
            "  <li><span class=\"step-num\">2</span> <span>Descubre las mejores chollos y ofertas de la comunidad Nexus.</span></li>" +
            "  <li><span class=\"step-num\">3</span> <span>Chatea con vendedores, negocia precios y sigue el estado de tus envíos en tiempo real.</span></li>" +
            "</ul>";
        enviarEmailHtml(to, "✅ Verifica tu cuenta en Nexus — " + username, contenido);
    }

    // ── 2. RESETEAR CONTRASEÑA ────────────────────────────────────────────────
    @Async
    public void enviarResetPassword(String to, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        String contenido =
            "<h2>Restablecimiento de contraseña 🔐</h2>" +
            "<p class=\"lead\">Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en Nexus.</p>" +
            "<p>Si fuiste tú, haz clic en el botón de abajo para crear una nueva contraseña. " +
            "El enlace es válido únicamente durante <strong>15 minutos</strong> por razones de seguridad.</p>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + link + "\" class=\"btn\">Cambiar mi contraseña</a>" +
            "</div>" +
            "<div class=\"alert-box alert-info\">" +
            "  <div>" +
            "    <strong>¿El botón no funciona?</strong><br>" +
            "    Copia y pega el siguiente enlace en tu navegador:<br>" +
            "    <span style=\"word-break: break-all; font-size: 13px; color: #3b82f6;\">" + link + "</span>" +
            "  </div>" +
            "</div>" +
            "<hr class=\"divider\">" +
            "<div class=\"alert-box alert-warning\">" +
            "  <div>" +
            "    <strong>⚠️ No has solicitado este cambio?</strong><br>" +
            "    Si no has iniciado esta solicitud, tu cuenta sigue siendo segura. Puedes ignorar este mensaje. " +
            "    Si sospechas actividad no autorizada, contacta con nuestro soporte inmediatamente desde " +
            "    <a href=\"" + frontendUrl + "/ayuda\" style=\"color: #d97706;\">nexus.app/ayuda</a>." +
            "  </div>" +
            "</div>";
        enviarEmailHtml(to, "Restablece tu contraseña — Nexus", contenido);
    }

    // ── 3. AUTENTICACIÓN EN DOS PASOS (2FA) ───────────────────────────────────
    @Async
    public void enviarOtp2FA(String to, String otp, String motivo) {
        String contenido =
            "<h2>Código de seguridad 2FA 🛡️</h2>" +
            "<p class=\"lead\">Se ha detectado un intento de <strong>" + escapeHtml(motivo) + "</strong> en tu cuenta.</p>" +
            "<p>Para confirmar que eres tú, introduce el siguiente código de un solo uso en la aplicación:</p>" +
            "<div class=\"code-container\">" +
            "  <p class=\"code-label\">Código de verificación de un solo uso</p>" +
            "  <p class=\"code-box\">" + escapeHtml(otp) + "</p>" +
            "  <p class=\"text-muted\" style=\"margin-top: 16px;\">Expira en <strong>10 minutos</strong>.</p>" +
            "</div>" +
            "<div class=\"alert-box alert-warning\">" +
            "  <div>" +
            "    <strong>⚠️ IMPORTANTE</strong>: Nunca compartas este código con nadie, ni siquiera con " +
            "    empleados de Nexus. Nuestro equipo jamás te pedirá este código." +
            "  </div>" +
            "</div>" +
            "<p class=\"text-muted\">Si no has iniciado este acceso, te recomendamos cambiar tu contraseña " +
            "inmediatamente desde <a href=\"" + frontendUrl + "/ajustes\">Ajustes de cuenta</a>.</p>";
        enviarEmailHtml(to, "🔐 Código seguridad 2FA — Nexus", contenido);
    }

    @Async
    public void enviarOtpDosFactores(String to, String otp) {
        enviarOtp2FA(to, otp, "iniciar sesión en tu cuenta");
    }

    // ── 4. CONFIRMACIÓN DE COMPRA ─────────────────────────────────────────────
    @Async
    public void enviarConfirmacionCompra(String to, String titulo, Double precio) {
        String contenido =
            "<h2>¡Compra confirmada! 🎉</h2>" +
            "<p class=\"lead\">Tu pago ha sido procesado y verificado con éxito. ¡Enhorabuena por tu compra!</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Artículo comprado</span>" +
            "    <span class=\"data-value\">" + escapeHtml(titulo) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Total pagado</span>" +
            "    <span class=\"data-value highlight\">" + String.format("%.2f €", precio) + "</span>" +
            "  </div>" +
            "</div>" +
            "<div class=\"alert-box alert-success\">" +
            "  <div>" +
            "    <strong>✅ Pago en escrow seguro</strong><br>" +
            "    El vendedor ha recibido la notificación y tiene un plazo para enviar el producto. " +
            "    Tu dinero permanece protegido en Nexus hasta que confirmes la entrega." +
            "  </div>" +
            "</div>" +
            "<h3>¿Qué ocurre ahora?</h3>" +
            "<ul class=\"steps-list\">" +
            "  <li><span class=\"step-num\">1</span><span>El vendedor preparará el paquete y lo enviará al transportista.</span></li>" +
            "  <li><span class=\"step-num\">2</span><span>Recibirás un email con el número de seguimiento cuando el envío esté registrado.</span></li>" +
            "  <li><span class=\"step-num\">3</span><span>Nexus actualizará el estado del envío automáticamente y te avisará en cada hito.</span></li>" +
            "  <li><span class=\"step-num\">4</span><span>Una vez recibido, podrás dejar tu valoración al vendedor.</span></li>" +
            "</ul>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=compras\" class=\"btn\">Ver mis compras</a>" +
            "</div>" +
            "<p class=\"text-muted\">Si detectas cualquier incidencia o el vendedor no envía en el plazo establecido, " +
            "nuestro equipo de soporte intervendrá para protegerte. Recuerda que bajo el sistema de escrow de Nexus, " +
            "tu dinero nunca se libera al vendedor hasta que tú confirmas la recepción del artículo o " +
            "vence el plazo de protección establecido.</p>" +
            "<hr class=\"divider\">" +
            "<h3>Preguntas frecuentes</h3>" +
            "<ul class=\"steps-list\">" +
            "  <li><span class=\"step-num\">?</span><span><strong>¿Cuándo recibiré el artículo?</strong><br>El vendedor debe enviarlo en el plazo marcado. Una vez enviado, recibirás el número de seguimiento por email.</span></li>" +
            "  <li><span class=\"step-num\">?</span><span><strong>¿Qué pasa si hay un problema?</strong><br>Abre una incidencia desde el chat del pedido. Nuestro equipo de mediación resolverá el conflicto.</span></li>" +
            "  <li><span class=\"step-num\">?</span><span><strong>¿Cuándo se libera el pago al vendedor?</strong><br>Cuando confirmas la entrega o vence el plazo de protección del comprador.</span></li>" +
            "</ul>";
        enviarEmailHtml(to, "✅ Compra confirmada: " + titulo + " — Nexus", contenido);
    }

    @Async
    public void enviarResumenPagoComprador(String to, Integer compraId, String titulo, Double totalPagado,
            Double precioProducto, Double costeEnvio, Double comisionNexus) {
        String contenido =
            "<h2>Resumen detallado de tu compra</h2>" +
            "<p class=\"lead\">Tu pedido <strong>#" + compraId + "</strong> ha sido registrado. " +
            "Aquí tienes el desglose completo de los costes:</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Nº de pedido</span>" +
            "    <span class=\"data-value\">#" + compraId + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Artículo</span>" +
            "    <span class=\"data-value\">" + escapeHtml(titulo) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Precio del producto</span>" +
            "    <span class=\"data-value\">" + String.format("%.2f €", precioProducto != null ? precioProducto : 0.0) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Coste de envío</span>" +
            "    <span class=\"data-value\">" + String.format("%.2f €", costeEnvio != null ? costeEnvio : 0.0) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Comisión de servicio Nexus</span>" +
            "    <span class=\"data-value\">" + String.format("%.2f €", comisionNexus != null ? comisionNexus : 0.0) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\" style=\"font-weight: 700;\">TOTAL pagado</span>" +
            "    <span class=\"data-value highlight\">" + String.format("%.2f €", totalPagado != null ? totalPagado : 0.0) + "</span>" +
            "  </div>" +
            "</div>" +
            "<p class=\"text-muted\">La comisión de servicio cubre la protección de pago en escrow, el seguro de la " +
            "transacción y el soporte de Nexus durante todo el proceso de compra-venta.</p>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=compras&compraId=" + compraId + "\" class=\"btn\">Seguir mi pedido</a>" +
            "</div>";
        enviarEmailHtml(to, "📋 Detalle de pago — Pedido #" + compraId + " — Nexus", contenido);
    }

    // ── 5. NUEVA VENTA (VENDEDOR) ─────────────────────────────────────────────
    @Async
    public void enviarNuevaVentaVendedor(String to, String tituloProducto, Integer compraId, String nombreComprador) {
        String linkVentas = frontendUrl + "/perfil?tab=ventas";
        String contenido =
            "<h2>¡Has vendido un artículo! 🎉</h2>" +
            "<p class=\"lead\"><strong>" + escapeHtml(nombreComprador) + "</strong> ha comprado tu artículo " +
            "<strong>\"" + escapeHtml(tituloProducto) + "\"</strong>. ¡Enhorabuena!</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Pedido</span>" +
            "    <span class=\"data-value\">#" + compraId + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Artículo</span>" +
            "    <span class=\"data-value\">" + escapeHtml(tituloProducto) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Comprador</span>" +
            "    <span class=\"data-value\">" + escapeHtml(nombreComprador) + "</span>" +
            "  </div>" +
            "</div>" +
            "<div class=\"alert-box alert-warning\">" +
            "  <div>" +
            "    <strong>⏰ Tienes un plazo para enviar</strong><br>" +
            "    Debes preparar y enviar el paquete antes de que venza el plazo. Si no envías a tiempo, " +
            "    la compra se cancelará automáticamente y el comprador recibirá un reembolso." +
            "  </div>" +
            "</div>" +
            "<h3>Pasos a seguir ahora</h3>" +
            "<ul class=\"steps-list\">" +
            "  <li><span class=\"step-num\">1</span><span>Entra en <strong>Mis Ventas</strong> y abre el pedido #" + compraId + ".</span></li>" +
            "  <li><span class=\"step-num\">2</span><span>Empaqueta el artículo con cuidado para evitar daños durante el transporte.</span></li>" +
            "  <li><span class=\"step-num\">3</span><span>Genera o registra el envío en Nexus para activar el seguimiento automático.</span></li>" +
            "  <li><span class=\"step-num\">4</span><span>El comprador recibirá una notificación y podrá seguir el estado en tiempo real.</span></li>" +
            "  <li><span class=\"step-num\">5</span><span>Una vez entregado, recibirás tu dinero descontando la comisión de Nexus.</span></li>" +
            "</ul>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + linkVentas + "\" class=\"btn\">Ir a Mis Ventas</a>" +
            "</div>" +
            "<p class=\"text-muted\">Ante cualquier problema con el envío o el comprador, nuestro equipo de soporte " +
            "está disponible 24/7 desde <a href=\"" + frontendUrl + "/ayuda\">Nexus Ayuda</a>. " +
            "Recuerda que el pago está protegido por nuestro sistema de escrow hasta que el comprador confirme la recepción.</p>";
        enviarEmailHtml(to, "🛍️ Nueva venta: " + tituloProducto + " — Nexus", contenido);
    }

    // ── 6. NOTIFICACIÓN DE ENVÍO (COMPRADOR) ─────────────────────────────────
    @Async
    public void enviarNotificacionEnvio(String to, String titulo, String tracking, String transportista) {
        String trackingRows = "";
        String trackingCta = "";
        if (tracking != null && !tracking.isEmpty()) {
            trackingRows =
                "  <div class=\"data-row\">" +
                "    <span class=\"data-label\">Transportista</span>" +
                "    <span class=\"data-value\">" + escapeHtml(transportista != null ? transportista : "Estándar") + "</span>" +
                "  </div>" +
                "  <div class=\"data-row\">" +
                "    <span class=\"data-label\">Número de seguimiento</span>" +
                "    <span class=\"data-value\" style=\"font-family: monospace;\">" + escapeHtml(tracking) + "</span>" +
                "  </div>";
            trackingCta = "<div class=\"text-center\"><a href=\"" + frontendUrl + "/perfil?tab=compras\" class=\"btn\">Ver seguimiento en Nexus</a></div>";
        }
        String contenido =
            "<h2>Tu paquete está en camino 📦</h2>" +
            "<p class=\"lead\">¡Buenas noticias! El vendedor ha registrado el envío de <strong>\"" + escapeHtml(titulo) + "\"</strong>.</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Artículo</span>" +
            "    <span class=\"data-value\">" + escapeHtml(titulo) + "</span>" +
            "  </div>" +
            trackingRows +
            "</div>" +
            "<div class=\"alert-box alert-info\">" +
            "  <div>" +
            "    <strong>📡 Seguimiento automático activado</strong><br>" +
            "    Nexus monitorizará el estado del envío y te avisará por email en cada hito importante: " +
            "    <em>En tránsito</em>, <em>En reparto</em> y <em>Entregado</em>." +
            "  </div>" +
            "</div>" +
            trackingCta +
            "<p class=\"text-muted\" style=\"margin-top: 24px;\">Si tienes algún problema con el envío, " +
            "puedes abrir una incidencia desde el chat del pedido o contactar con soporte.</p>";
        enviarEmailHtml(to, "📦 Tu pedido ha sido enviado: " + titulo + " — Nexus", contenido);
    }

    // ── 7. GUÍA DE ENVÍO QR (VENDEDOR) ───────────────────────────────────────
    @Async
    public void enviarGuiaEnvioConQrVendedor(String to, String tituloProducto, Integer compraId, String codigoEnvio,
            String qrBase64, String transportista, String ciudad, int plazoDias) {
        String qrHtml = (qrBase64 != null && !qrBase64.isBlank())
                ? "<div class=\"text-center\" style=\"margin: 28px 0;\">" +
                  "<img style=\"width: 200px; height: 200px; border: 2px solid #e2e8f0; border-radius: 16px; padding: 10px;\" " +
                  "src=\"data:image/png;base64," + qrBase64 + "\" alt=\"QR de envío\" />" +
                  "<p class=\"text-muted\" style=\"margin-top: 8px;\">Muestra este QR en el punto de entrega</p>" +
                  "</div>"
                : "";
        String contenido =
            "<h2>Guía de envío — Pedido #" + compraId + " 🚚</h2>" +
            "<p class=\"lead\">Has vendido <strong>\"" + escapeHtml(tituloProducto) + "\"</strong>. " +
            "Sigue estos pasos para completar el envío correctamente.</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Pedido</span>" +
            "    <span class=\"data-value\">#" + compraId + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Código de envío Nexus</span>" +
            "    <span class=\"data-value\" style=\"font-family: monospace; font-size: 18px; color: #7c3aed;\">" + escapeHtml(codigoEnvio) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Transportista</span>" +
            "    <span class=\"data-value\">" + escapeHtml(transportista != null ? transportista : "Correos") + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Zona recomendada</span>" +
            "    <span class=\"data-value\">" + escapeHtml(ciudad != null ? ciudad : "Tu zona") + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Plazo máximo de entrega</span>" +
            "    <span class=\"data-value\" style=\"color: #d97706; font-weight: 800;\">" + plazoDias + " días hábiles</span>" +
            "  </div>" +
            "</div>" +
            qrHtml +
            "<ul class=\"steps-list\">" +
            "  <li><span class=\"step-num\">1</span><span>Empaqueta el artículo de forma segura. Usa material de relleno si es frágil.</span></li>" +
            "  <li><span class=\"step-num\">2</span><span>Acude a la oficina del transportista <strong>" + escapeHtml(transportista != null ? transportista : "Correos") + "</strong> más cercana.</span></li>" +
            "  <li><span class=\"step-num\">3</span><span>Muestra el QR/código al empleado. Guarda el resguardo del envío.</span></li>" +
            "  <li><span class=\"step-num\">4</span><span>Introduce el número de seguimiento real en Nexus para activar el tracking automático.</span></li>" +
            "</ul>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=ventas\" class=\"btn\">Ir a la pantalla de envío</a>" +
            "</div>";
        enviarEmailHtml(to, "🚚 Guía de envío + QR — Pedido #" + compraId + " — Nexus", contenido);
    }

    // ── 8. ACTUALIZACIÓN DE TRACKING ──────────────────────────────────────────
    @Async
    public void enviarActualizacionTracking(String to, String tituloProducto, String tracking, String estado, String urlSeguimiento) {
        String ctaHtml = (urlSeguimiento != null && !urlSeguimiento.isBlank())
                ? "<div class=\"text-center\"><a href=\"" + urlSeguimiento + "\" class=\"btn\">Ver seguimiento en tiempo real</a></div>"
                : "<div class=\"text-center\"><a href=\"" + frontendUrl + "/perfil?tab=compras\" class=\"btn\">Ver mis pedidos</a></div>";
        String contenido =
            "<h2>Actualización de seguimiento 📡</h2>" +
            "<p class=\"lead\">Tu pedido de <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> ha cambiado de estado.</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Artículo</span>" +
            "    <span class=\"data-value\">" + escapeHtml(tituloProducto) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Número de seguimiento</span>" +
            "    <span class=\"data-value\" style=\"font-family: monospace;\">" + escapeHtml(tracking != null ? tracking : "—") + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Nuevo estado</span>" +
            "    <span class=\"data-value\" style=\"color: #059669; font-size: 17px;\">" + escapeHtml(estado) + "</span>" +
            "  </div>" +
            "</div>" +
            ctaHtml +
            "<p class=\"text-muted\" style=\"margin-top: 24px;\">Nexus monitoriza automáticamente el estado de tu envío " +
            "y te notificará en cada cambio importante.</p>";
        enviarEmailHtml(to, "📍 Seguimiento actualizado: " + estado + " — Nexus", contenido);
    }

    // ── 9. ENTREGA CONFIRMADA ── ──────────────────────────────────────────────
    @Async
    public void enviarEntregaConfirmada(String to, String tituloProducto, boolean paraComprador) {
        String contenido =
            "<h2>¡Entrega confirmada! ✅</h2>" +
            "<p class=\"lead\">El pedido de <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> figura como entregado.</p>" +
            (paraComprador
                ? "<div class=\"alert-box alert-success\">" +
                  "  <div><strong>¡El artículo ha llegado!</strong><br>" +
                  "  Esperamos que estés contento con tu compra. " +
                  "  Ahora puedes dejar tu valoración al vendedor y compartir tu experiencia con la comunidad Nexus." +
                  "  </div>" +
                  "</div>" +
                  "<div class=\"text-center\"><a href=\"" + frontendUrl + "/perfil?tab=compras\" class=\"btn\">Valorar al vendedor</a></div>"
                : "<div class=\"alert-box alert-success\">" +
                  "  <div><strong>¡Venta completada!</strong><br>" +
                  "  Tu venta de <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> ha sido completada con éxito. " +
                  "  La transacción ha finalizado y el proceso de cobro quedará cerrado en breve." +
                  "  </div>" +
                  "</div>" +
                  "<div class=\"text-center\"><a href=\"" + frontendUrl + "/perfil?tab=ventas\" class=\"btn\">Ver mis ventas</a></div>") +
            "<p class=\"text-muted\" style=\"margin-top: 24px;\">Gracias por ser parte de la comunidad Nexus. " +
            "¡Esperamos verte de nuevo pronto!</p>";
        enviarEmailHtml(to, "✅ Pedido entregado: " + tituloProducto + " — Nexus", contenido);
    }

    // ── 10. REEMBOLSOS Y CANCELACIONES ────────────────────────────────────────
    @Async
    public void enviarAdminReembolsoComprador(String to, Integer compraId, String tituloProducto, String motivo) {
        String contenido =
            "<h2>Reembolso procesado 💰</h2>" +
            "<p class=\"lead\">Tu pedido <strong>#" + compraId + "</strong> de <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> " +
            "ha sido reembolsado por el equipo de soporte de Nexus.</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Pedido reembolsado</span>" +
            "    <span class=\"data-value\">#" + compraId + " — " + escapeHtml(tituloProducto) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Motivo del reembolso</span>" +
            "    <span class=\"data-value\">" + escapeHtml(motivo != null ? motivo : "Incidencia de operación resuelta por soporte") + "</span>" +
            "  </div>" +
            "</div>" +
            "<div class=\"alert-box alert-info\">" +
            "  <div><strong>💳 Plazo de devolución</strong><br>" +
            "  El importe se devolverá a tu método de pago original en un plazo de <strong>5-10 días hábiles</strong>, " +
            "  dependiendo de tu entidad bancaria." +
            "  </div>" +
            "</div>" +
            "<div class=\"text-center\"><a href=\"" + frontendUrl + "/perfil?tab=compras&compraId=" + compraId + "\" class=\"btn\">Ver detalle del pedido</a></div>";
        enviarEmailHtml(to, "💰 Reembolso de pedido #" + compraId + " — Nexus", contenido);
    }

    @Async
    public void enviarAdminReembolsoVendedor(String to, Integer compraId, String tituloProducto, String motivo) {
        String contenido =
            "<h2>Venta anulada — Reembolso procesado</h2>" +
            "<p class=\"lead\">La compra <strong>#" + compraId + "</strong> de <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> " +
            "ha sido reembolsada al comprador por el equipo de soporte.</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Pedido afectado</span>" +
            "    <span class=\"data-value\">#" + compraId + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Motivo</span>" +
            "    <span class=\"data-value\">" + escapeHtml(motivo != null ? motivo : "Incidencia de operación") + "</span>" +
            "  </div>" +
            "</div>" +
            "<p class=\"text-muted\">Esta operación ya no requiere más acciones por tu parte. " +
            "Si tienes alguna duda, contacta con nuestro soporte en <a href=\"" + frontendUrl + "/ayuda\">Nexus Ayuda</a>.</p>" +
            "<div class=\"text-center\"><a href=\"" + frontendUrl + "/perfil?tab=ventas\" class=\"btn\">Ver mis ventas</a></div>";
        enviarEmailHtml(to, "❌ Venta anulada — Pedido #" + compraId + " — Nexus", contenido);
    }

    @Async
    public void enviarAdminCancelacion(String to, Integer compraId, String tituloProducto, boolean paraComprador) {
        String contenido =
            "<h2>Pedido cancelado por administración</h2>" +
            "<p class=\"lead\">El pedido <strong>#" + compraId + "</strong> de <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> " +
            "ha sido cancelado por el equipo de Nexus.</p>" +
            (paraComprador
                ? "<div class=\"alert-box alert-info\">" +
                  "  <div><strong>💳 Reembolso automático</strong><br>" +
                  "  Si el pago fue procesado, recibirás el importe íntegro de vuelta en tu método de pago en 5-10 días hábiles." +
                  "  </div>" +
                  "</div>"
                : "<div class=\"alert-box alert-info\">" +
                  "  <div>La operación ya no está activa. No se requiere ninguna acción adicional por tu parte.</div>" +
                  "</div>") +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=" + (paraComprador ? "compras" : "ventas") + "&compraId=" + compraId + "\" class=\"btn\">Ver detalle</a>" +
            "</div>";
        enviarEmailHtml(to, "❌ Cancelación de pedido #" + compraId + " — Nexus", contenido);
    }

    // ── 11. NUEVA ETIQUETA VENDEDOR ───────────────────────────────────────────
    @Async
    public void enviarNuevaEtiquetaVendedor(String to, Integer compraId, String tituloProducto, String nuevoCodigo, String qrBase64) {
        String qrHtml = (qrBase64 != null && !qrBase64.isBlank())
                ? "<div class=\"text-center\" style=\"margin: 24px 0;\">" +
                  "<img style=\"width: 180px; height: 180px; border: 2px solid #e2e8f0; border-radius: 12px; padding: 8px;\" " +
                  "src=\"data:image/png;base64," + qrBase64 + "\" alt=\"QR de envío\" /></div>"
                : "";
        String contenido =
            "<h2>Nueva etiqueta de envío 🏷️</h2>" +
            "<p class=\"lead\">Para el pedido <strong>#" + compraId + "</strong> de <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> " +
            "se ha generado una nueva etiqueta de envío.</p>" +
            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Nuevo código de envío</span>" +
            "    <span class=\"data-value\" style=\"font-family: monospace; font-size: 18px; color: #7c3aed;\">" + escapeHtml(nuevoCodigo) + "</span>" +
            "  </div>" +
            "</div>" +
            qrHtml +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=ventas\" class=\"btn\">Ir a la pantalla de envío</a>" +
            "</div>";
        enviarEmailHtml(to, "🏷️ Nueva etiqueta — Pedido #" + compraId + " — Nexus", contenido);
    }

    // ── 12. DEVOLUCIONES ──────────────────────────────────────────────────────
    @Async
    public void enviarSolicitudDevolucionVendedor(String to, String tituloProducto) {
        String contenido =
            "<h2>Solicitud de devolución recibida 🔄</h2>" +
            "<p class=\"lead\">Un comprador ha solicitado devolver el artículo <strong>\"" + escapeHtml(tituloProducto) + "\"</strong>.</p>" +
            "<div class=\"alert-box alert-warning\">" +
            "  <div>" +
            "    <strong>⏰ Responde a la brevedad posible</strong><br>" +
            "    Tienes un plazo para revisar y responder a la solicitud de devolución. " +
            "    Si no respondes en el tiempo estipulado, el equipo de Nexus tomará una decisión." +
            "  </div>" +
            "</div>" +
            "<p>Para gestionar la devolución, ve a <strong>Mis Ventas</strong> y abre el pedido afectado. " +
            "Allí podrás ver el motivo, comunicarte con el comprador y aceptar o rechazar la devolución.</p>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=ventas\" class=\"btn btn-warning\">Revisar solicitud de devolución</a>" +
            "</div>" +
            "<p class=\"text-muted\">Si tienes dudas sobre el proceso de devoluciones, consulta nuestra " +
            "<a href=\"" + frontendUrl + "/ayuda\">guía de vendedores</a>.</p>";
        enviarEmailHtml(to, "🔄 Solicitud de devolución — Nexus", contenido);
    }

    @Async
    public void enviarRespuestaDevolucionComprador(String to, String mensajePlano) {
        String contenido =
            "<h2>Actualización de tu devolución</h2>" +
            "<p class=\"lead\">El vendedor ha respondido a tu solicitud de devolución.</p>" +
            "<div class=\"data-card\">" +
            "  <p>" + escapeHtml(mensajePlano) + "</p>" +
            "</div>" +
            "<p>Si no estás de acuerdo con la respuesta del vendedor, puedes escalar la incidencia al equipo " +
            "de soporte de Nexus para que intervenga como mediador.</p>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=compras\" class=\"btn\">Ver mi devolución</a>" +
            "</div>";
        enviarEmailHtml(to, "📩 Respuesta a tu devolución — Nexus", contenido);
    }

    @Async
    public void enviarReembolsoPlazoVencidoComprador(String to, String tituloProducto) {
        String contenido =
            "<h2>Reembolso automático procesado 💰</h2>" +
            "<p class=\"lead\">El vendedor no envió <strong>\"" + escapeHtml(tituloProducto) + "\"</strong> dentro del plazo establecido.</p>" +
            "<div class=\"alert-box alert-success\">" +
            "  <div><strong>✅ Tu dinero está protegido</strong><br>" +
            "  Hemos procesado el reembolso automáticamente. El importe íntegro volverá a tu método de pago " +
            "  en un plazo de <strong>5-10 días hábiles</strong>." +
            "  </div>" +
            "</div>" +
            "<p class=\"text-muted\">El anuncio del vendedor ha sido suspendido temporalmente para revisión. " +
            "Lamentamos los inconvenientes causados.</p>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/perfil?tab=compras\" class=\"btn\">Ver mis compras</a>" +
            "</div>";
        enviarEmailHtml(to, "💰 Reembolso por plazo de envío — Nexus", contenido);
    }

    // ── 13. CONTRATOS DE PUBLICIDAD ───────────────────────────────────────────
    @Async
    public void enviarContratoNuevaPropuesta(String to, String empresaNombre, double monto,
            String descripcion, String urlContratos) {
        String descHtml = (descripcion != null && !descripcion.isBlank())
                ? "<div class=\"data-row\"><span class=\"data-label\">Descripción de la campaña</span>" +
                  "<span class=\"data-value\">" + escapeHtml(descripcion) + "</span></div>"
                : "";
        String contenido =
            "<h2>Nueva propuesta de publicidad en Nexus 📣</h2>" +
            "<p class=\"lead\">Estimado equipo de <strong>" + escapeHtml(empresaNombre) + "</strong>, " +
            "el equipo comercial de <strong>Nexus</strong> ha preparado una propuesta de publicidad " +
            "exclusiva y personalizada para tu empresa. Con miles de usuarios activos al mes, " +
            "Nexus es la plataforma ideal para dar visibilidad a tu marca.</p>" +

            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Empresa</span>" +
            "    <span class=\"data-value\">" + escapeHtml(empresaNombre) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Presupuesto total (IVA incl.)</span>" +
            "    <span class=\"data-value highlight\">" + String.format("%.2f €", monto) + "</span>" +
            "  </div>" +
            descHtml +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Estado de la propuesta</span>" +
            "    <span class=\"data-value\"><span class=\"badge badge-orange\">Pendiente de aceptación</span></span>" +
            "  </div>" +
            "</div>" +

            "<h3>¿Cómo funciona la publicidad en Nexus?</h3>" +
            "<ul class=\"steps-list\">" +
            "  <li><span class=\"step-num\">1</span><span><strong>Revisa la propuesta</strong><br>" +
            "  Accede a la sección de Publicidad en tu panel de empresa y consulta todos los detalles del contrato.</span></li>" +
            "  <li><span class=\"step-num\">2</span><span><strong>Acepta y paga con Stripe</strong><br>" +
            "  El pago es 100% seguro a través de Stripe, la plataforma de pagos de confianza utilizada por millones de empresas en Europa.</span></li>" +
            "  <li><span class=\"step-num\">3</span><span><strong>Tu campaña se activa automáticamente</strong><br>" +
            "  En segundos tras la confirmación del pago, tu banner o producto patrocinado empezará a mostrarse a los usuarios de Nexus.</span></li>" +
            "  <li><span class=\"step-num\">4</span><span><strong>Seguimiento en tiempo real</strong><br>" +
            "  Desde tu panel de empresa podrás consultar el estado de tu contrato durante toda la vigencia de la campaña.</span></li>" +
            "</ul>" +

            "<div class=\"alert-box alert-info\">" +
            "  <div>" +
            "    <strong>📊 ¿Por qué anunciarte en Nexus?</strong><br>" +
            "    Nexus cuenta con una comunidad activa de compradores y vendedores altamente comprometidos. " +
            "    Nuestras campañas cuentan con alta visibilidad directamente en el marketplace, generando " +
            "    impresiones reales de usuarios con intención de compra. El CTR medio de nuestros banners es " +
            "    superior al 3%, muy por encima de la media del sector." +
            "  </div>" +
            "</div>" +

            "<div class=\"text-center\">" +
            "  <a href=\"" + urlContratos + "\" class=\"btn\">Revisar propuesta y aceptar →</a>" +
            "</div>" +

            "<div class=\"alert-box alert-success\">" +
            "  <div>" +
            "    <strong>🔒 Pago 100% seguro y sin sorpresas</strong><br>" +
            "    El cobro se realiza de una sola vez. No hay pagos ocultos, suscripciones ni comisiones adicionales. " +
            "    Si decides rechazar la propuesta, no se efectuará ningún cargo." +
            "  </div>" +
            "</div>" +

            "<hr class=\"divider\">" +
            "<h3>¿Tienes alguna pregunta?</h3>" +
            "<p>Estamos aquí para ayudarte. Puedes contactar con nuestro equipo comercial por cualquiera de estos medios:</p>" +
            "<ul class=\"steps-list\">" +
            "  <li><span class=\"step-num\">@</span><span>Email: <a href=\"mailto:somosnexusapp@gmail.com\">somosnexusapp@gmail.com</a></span></li>" +
            "  <li><span class=\"step-num\">💬</span><span>Chat en vivo desde tu panel de empresa en Nexus</span></li>" +
            "  <li><span class=\"step-num\">📞</span><span>Soporte disponible de lunes a viernes, de 9:00 a 18:00 h.</span></li>" +
            "</ul>" +
            "<p class=\"text-muted\">Esta propuesta ha sido generada exclusivamente para " + escapeHtml(empresaNombre) + ". " +
            "Si has recibido este email por error, por favor ignóralo o contáctanos para aclararlo.</p>";
        enviarEmailHtml(to, "📣 Nexus te propone una campaña de publicidad — " + empresaNombre, contenido);
    }

    @Async
    public void enviarContratoActivado(String to, String empresaNombre, String tipoContrato) {
        String tipoDesc = "BANNER".equalsIgnoreCase(tipoContrato) ? "Banner publicitario" : "Producto patrocinado";
        String tipoDetalle = "BANNER".equalsIgnoreCase(tipoContrato)
                ? "Tu banner está siendo mostrado en las secciones estratégicas de Nexus. " +
                  "Los usuarios que hagan clic serán redirigidos a la URL que configuraste en la propuesta."
                : "Tu producto aparece ahora en las primeras posiciones del marketplace con el badge " +
                  "\"⚡ Patrocinado\", maximizando la visibilidad ante compradores con intención de compra activa.";
        String contenido =
            "<h2>¡Tu campaña en Nexus ya está en marcha! 🚀</h2>" +
            "<p class=\"lead\">¡Enhorabuena, <strong>" + escapeHtml(empresaNombre) + "</strong>! " +
            "El pago ha sido confirmado exitosamente por Stripe y tu campaña de publicidad en Nexus " +
            "está ahora completamente activa. Tu marca ya es visible para miles de usuarios.</p>" +

            "<div class=\"data-card\">" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Empresa anunciante</span>" +
            "    <span class=\"data-value\">" + escapeHtml(empresaNombre) + "</span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Tipo de campaña</span>" +
            "    <span class=\"data-value\"><span class=\"badge badge-purple\">" + tipoDesc + "</span></span>" +
            "  </div>" +
            "  <div class=\"data-row\">" +
            "    <span class=\"data-label\">Estado</span>" +
            "    <span class=\"data-value\"><span class=\"badge badge-green\">ACTIVO</span></span>" +
            "  </div>" +
            "</div>" +
            "<div class=\"alert-box alert-success\">" +
            "  <div>" +
            "    <strong>✅ Campaña en marcha</strong><br>" +
            "    Tu publicidad ya es visible para los usuarios de Nexus. Puedes gestionar tus contratos " +
            "    activos desde tu panel de empresa en cualquier momento." +
            "  </div>" +
            "</div>" +
            "<div class=\"text-center\">" +
            "  <a href=\"" + frontendUrl + "/publicidad/contratos\" class=\"btn btn-success\">Ver mis contratos</a>" +
            "</div>" +
            "<p class=\"text-muted\">Si tienes alguna pregunta sobre el rendimiento de tu campaña, " +
            "no dudes en contactarnos en <a href=\"mailto:somosnexusapp@gmail.com\">somosnexusapp@gmail.com</a>.</p>";
        enviarEmailHtml(to, "🚀 ¡Tu publicidad en Nexus está activa! — " + empresaNombre, contenido);
    }

    // ─── UTILIDADES ───────────────────────────────────────────────────────────
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}