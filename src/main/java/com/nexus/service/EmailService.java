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

    // --- PLANTILLA MAESTRA HTML (Diseño Premium Nexus Elite) ---
    private final String HTML_WRAPPER = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Elite</title>
            <style>
                body {
                    font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    background-color: #0f172a;
                    margin: 0;
                    padding: 40px 20px;
                    -webkit-font-smoothing: antialiased;
                }
                .wrapper {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 20px;
                    overflow: hidden;
                    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
                }
                .header {
                    background-color: #000000;
                    padding: 40px;
                    text-align: center;
                    border-bottom: 4px solid #7c3aed;
                }
                .header img {
                    height: 60px;
                    margin: 0 auto;
                }
                .header h1 {
                    margin: 0;
                    color: #ffffff;
                    font-size: 26px;
                    letter-spacing: 4px;
                    font-weight: 900;
                    text-transform: uppercase;
                }
                .content {
                    padding: 40px;
                    color: #1e293b;
                    line-height: 1.8;
                    font-size: 16px;
                }
                .content h2 {
                    color: #0f172a;
                    font-size: 24px;
                    margin-top: 0;
                    margin-bottom: 24px;
                    font-weight: 800;
                }
                .code-container {
                    background-color: #f5f3ff;
                    border: 1px solid #ddd6fe;
                    border-radius: 16px;
                    padding: 30px;
                    text-align: center;
                    margin: 32px 0;
                }
                .code-box {
                    font-family: 'Fira Code', monospace;
                    font-size: 42px;
                    font-weight: 800;
                    color: #7c3aed;
                    letter-spacing: 10px;
                    margin: 0;
                }
                .btn {
                    display: inline-block;
                    background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%);
                    color: #ffffff !important;
                    text-decoration: none;
                    padding: 18px 36px;
                    border-radius: 12px;
                    font-weight: 700;
                    font-size: 16px;
                    text-align: center;
                    margin: 24px 0;
                    box-shadow: 0 10px 20px rgba(124, 58, 237, 0.2);
                }
                .data-card {
                    background-color: #f8fafc;
                    border-radius: 16px;
                    padding: 24px;
                    margin: 24px 0;
                    border: 1px solid #e2e8f0;
                }
                .data-row {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 14px;
                    border-bottom: 1px solid #e2e8f0;
                    padding-bottom: 14px;
                }
                .data-row:last-child {
                    margin-bottom: 0;
                    border-bottom: none;
                    padding-bottom: 0;
                }
                .footer {
                    background-color: #f1f5f9;
                    padding: 40px;
                    text-align: center;
                    font-size: 13px;
                    color: #64748b;
                    border-top: 1px solid #e2e8f0;
                }
                .footer p { margin: 10px 0; }
                .text-muted { color: #64748b; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="wrapper">
                <div class="header">
                    <img src="%s/logo.webp" alt="Nexus Elite">
                </div>
                <div class="content">
                    %%s
                </div>
                <div class="footer">
                    <p>Has recibido este correo electrónico porque eres parte de la comunidad de <b>Nexus Elite</b>.</p>
                    <p>Por favor, no respondas a este mensaje, es un envío automático de nuestro sistema.</p>
                    <p style="margin-top: 20px; font-weight: 600;">&copy; 2026 Nexus App S.L. · Todos los derechos reservados.</p>
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
            String finalHtml = String.format(HTML_WRAPPER, frontendUrl, htmlContent);
            h.setText(finalHtml, true); 
            
            mailSender.send(m); 
            System.out.println("✅ [NEXUS EMAIL] Correo HTML enviado con éxito a: " + to);
        } catch(Exception e) {
            System.err.println("❌ [NEXUS EMAIL] Falla crítica al enviar a " + to + ". Causa: " + e.getMessage());
            e.printStackTrace();
            // Fallback: enviar texto plano para no perder el evento transaccional.
            try {
                SimpleMailMessage fallback = new SimpleMailMessage();
                fallback.setFrom(fromEmail);
                fallback.setTo(to);
                fallback.setSubject(subject + " (fallback)");
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
            "<p>Tu compra se ha procesado correctamente y el pago ya está validado por Nexus.</p>" +
            "<p>El vendedor ha sido notificado y debe preparar el paquete dentro del plazo establecido. " +
            "Recibirás correos automáticos cada vez que cambie el estado del envío.</p>" +
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
            "<p>Podrás seguir el pedido desde <b>Mis Compras</b> y también en la conversación del chat con el vendedor.</p>" +
            "<p class='text-muted'>Si detectas cualquier incidencia, contacta con soporte desde la app y podremos intervenir la operación.</p>";

        enviarEmailHtml(to, "Recibo de tu compra: " + titulo, contenido);
    }

    @Async
    public void enviarResumenPagoComprador(String to, Integer compraId, String titulo, Double totalPagado,
            Double precioProducto, Double costeEnvio, Double comisionNexus) {
        String contenido =
            "<h2>Resumen de pago de tu compra</h2>" +
            "<p>Tu operación se ha registrado correctamente. Aquí tienes el detalle completo:</p>" +
            "<div class='data-card'>" +
            "   <div class='data-row'><span style='color:#6b7280'>Pedido:</span><span style='font-weight:600'>#" + compraId + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Artículo:</span><span style='font-weight:600'>" + escapeHtml(titulo) + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Producto:</span><span>" + String.format("%.2f €", precioProducto != null ? precioProducto : 0.0) + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Envío:</span><span>" + String.format("%.2f €", costeEnvio != null ? costeEnvio : 0.0) + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Comisión Nexus:</span><span>" + String.format("%.2f €", comisionNexus != null ? comisionNexus : 0.0) + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'><b>Total:</b></span><span style='font-weight:700; color:#0a84ff;'>" + String.format("%.2f €", totalPagado != null ? totalPagado : 0.0) + "</span></div>" +
            "</div>" +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/perfil?tab=compras&compraId=" + compraId + "' class='btn'>Ver mi compra</a></div>";
        enviarEmailHtml(to, "Detalle de pago pedido #" + compraId, contenido);
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
            "<p>¡Buenas noticias! El vendedor acaba de registrar el envío de tu artículo.</p>" +
            "<p>Desde este momento, Nexus actualizará automáticamente el seguimiento y te avisará por correo " +
            "en cada hito importante: en tránsito, en reparto y entregado.</p>" +
            "<div class='data-card'>" +
            "   <div class='data-row'>" +
            "       <span style='color:#6b7280'>Artículo:</span>" +
            "       <span style='font-weight:600'>" + titulo + "</span>" +
            "   </div>" + trackingInfo +
            "</div>" +
            "<p>Revisa también tu chat de compra: allí verás los mensajes de sistema del flujo de envío.</p>" +
            "<p>Gracias por confiar en la comunidad de Nexus.</p>";

        enviarEmailHtml(to, "Tu pedido ha sido enviado", contenido);
    }

    /**
     * Vendedor: venta confirmada — pasos para preparar el envío (misma plantilla HTML).
     */
    @Async
    public void enviarNuevaVentaVendedor(String to, String tituloProducto, Integer compraId, String nombreComprador) {
        String linkVentas = frontendUrl + "/perfil?tab=ventas";
        String contenido =
            "<h2>¡Has vendido un artículo! 🎉</h2>" +
            "<p>El comprador <b>" + escapeHtml(nombreComprador) + "</b> ha pagado <b>" + escapeHtml(tituloProducto) + "</b>.</p>" +
            "<div class='data-card'>" +
            "   <div class='data-row'><span style='color:#6b7280'>Pedido</span><span style='font-weight:600'>#" + compraId + "</span></div>" +
            "</div>" +
            "<h3 style='font-size:18px;margin-top:28px;'>Siguientes pasos</h3>" +
            "<ol style='padding-left:20px;line-height:1.9;'>" +
            "<li>Entra en <b>Mis ventas</b> y abre el pedido.</li>" +
            "<li>Empaqueta el producto con cuidado.</li>" +
            "<li>Genera o registra el envío y añade el número de seguimiento.</li>" +
            "<li>Marca el pedido como enviado antes de que venza el plazo.</li>" +
            "</ol>" +
            "<div style='text-align:center;'>" +
            "<a href='" + linkVentas + "' class='btn'>Ir a mis ventas</a>" +
            "</div>" +
            "<p class='text-muted'>Si no envías en el plazo indicado, la compra puede cancelarse y el comprador recibirá un reembolso.</p>";

        enviarEmailHtml(to, "Nueva venta: " + tituloProducto, contenido);
    }

    @Async
    public void enviarGuiaEnvioConQrVendedor(String to, String tituloProducto, Integer compraId, String codigoEnvio,
            String qrBase64, String transportista, String ciudad, int plazoDias) {
        String qrHtml = (qrBase64 != null && !qrBase64.isBlank())
                ? "<div style='text-align:center; margin:20px 0;'><img style='width:220px;height:220px;border:1px solid #ddd;border-radius:12px;padding:8px;' src='data:image/png;base64,"
                        + qrBase64 + "' alt='QR Envío' /></div>"
                : "";
        String contenido =
            "<h2>Guía de envío de tu venta</h2>" +
            "<p>Has vendido <b>" + escapeHtml(tituloProducto) + "</b>. Sigue estos pasos para enviar correctamente:</p>" +
            "<div class='data-card'>" +
            "   <div class='data-row'><span style='color:#6b7280'>Pedido</span><span>#"+ compraId +"</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Código de envío</span><span style='font-family:monospace;font-weight:700;'>" + escapeHtml(codigoEnvio) + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Transportista</span><span>" + escapeHtml(transportista != null ? transportista : "Correos") + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Zona recomendada</span><span>" + escapeHtml(ciudad != null ? ciudad : "tu zona") + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Plazo máximo</span><span>" + plazoDias + " días</span></div>" +
            "</div>" +
            qrHtml +
            "<ol style='padding-left:20px;line-height:1.9;'>" +
            "<li>Empaqueta bien el artículo.</li>" +
            "<li>Acude a la oficina de Correos y muestra este QR/código.</li>" +
            "<li>Introduce el tracking real en Nexus para activar seguimiento automático.</li>" +
            "</ol>" +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/compras/" + compraId + "/enviar' class='btn'>Ir a pantalla de envío</a></div>";
        enviarEmailHtml(to, "Guía de envío + QR pedido #" + compraId, contenido);
    }

    @Async
    public void enviarActualizacionTracking(String to, String tituloProducto, String tracking, String estado, String urlSeguimiento) {
        String cta = (urlSeguimiento != null && !urlSeguimiento.isBlank())
                ? "<div style='text-align:center;'><a href='" + urlSeguimiento + "' class='btn'>Ver seguimiento</a></div>"
                : "";
        String contenido =
            "<h2>Actualización de seguimiento</h2>" +
            "<p>Tu pedido de <b>" + escapeHtml(tituloProducto) + "</b> cambió de estado a <b>" + escapeHtml(estado) + "</b>.</p>" +
            "<div class='data-card'>" +
            "   <div class='data-row'><span style='color:#6b7280'>Tracking</span><span style='font-family:monospace;font-weight:700;'>" + escapeHtml(tracking != null ? tracking : "—") + "</span></div>" +
            "   <div class='data-row'><span style='color:#6b7280'>Estado</span><span>" + escapeHtml(estado) + "</span></div>" +
            "</div>" + cta;
        enviarEmailHtml(to, "Seguimiento actualizado: " + estado, contenido);
    }

    @Async
    public void enviarEntregaConfirmada(String to, String tituloProducto, boolean paraComprador) {
        String contenido =
            "<h2>Entrega confirmada</h2>" +
            "<p>El pedido de <b>" + escapeHtml(tituloProducto) + "</b> figura como entregado por el transportista.</p>" +
            (paraComprador
                ? "<p>Ahora puedes dejar tu reseña al vendedor desde Mis Compras.</p>"
                : "<p>La venta se ha completado y el proceso de cobro queda cerrado.</p>") +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/perfil?tab=compras' class='btn'>Abrir Nexus</a></div>";
        enviarEmailHtml(to, "Pedido entregado: " + tituloProducto, contenido);
    }

    @Async
    public void enviarAdminReembolsoComprador(String to, Integer compraId, String tituloProducto, String motivo) {
        String contenido =
            "<h2>Reembolso procesado por soporte/admin</h2>" +
            "<p>Se ha reembolsado tu pedido <b>#" + compraId + "</b> del artículo <b>" + escapeHtml(tituloProducto) + "</b>.</p>" +
            "<p>Motivo: " + escapeHtml(motivo != null ? motivo : "Incidencia de operación") + "</p>" +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/perfil?tab=compras&compraId=" + compraId + "' class='btn'>Ver compra</a></div>";
        enviarEmailHtml(to, "Reembolso de pedido #" + compraId, contenido);
    }

    @Async
    public void enviarAdminReembolsoVendedor(String to, Integer compraId, String tituloProducto, String motivo) {
        String contenido =
            "<h2>Venta anulada con reembolso</h2>" +
            "<p>La compra <b>#" + compraId + "</b> de <b>" + escapeHtml(tituloProducto) + "</b> fue reembolsada al comprador.</p>" +
            "<p>Motivo: " + escapeHtml(motivo != null ? motivo : "Incidencia de operación") + "</p>" +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/perfil?tab=ventas&compraId=" + compraId + "' class='btn'>Ver venta</a></div>";
        enviarEmailHtml(to, "Venta anulada pedido #" + compraId, contenido);
    }

    @Async
    public void enviarAdminCancelacion(String to, Integer compraId, String tituloProducto, boolean paraComprador) {
        String contenido =
            "<h2>Pedido cancelado por administración</h2>" +
            "<p>El pedido <b>#" + compraId + "</b> de <b>" + escapeHtml(tituloProducto) + "</b> ha sido cancelado.</p>" +
            (paraComprador
                ? "<p>Si procede, el importe se te devolverá automáticamente.</p>"
                : "<p>La operación ya no está activa y no requiere más acciones por tu parte.</p>") +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/perfil?tab=" + (paraComprador ? "compras" : "ventas") + "&compraId=" + compraId + "' class='btn'>Abrir detalle</a></div>";
        enviarEmailHtml(to, "Cancelación pedido #" + compraId, contenido);
    }

    @Async
    public void enviarNuevaEtiquetaVendedor(String to, Integer compraId, String tituloProducto, String nuevoCodigo, String qrBase64) {
        String qrHtml = (qrBase64 != null && !qrBase64.isBlank())
                ? "<div style='text-align:center; margin:20px 0;'><img style='width:200px;height:200px;border:1px solid #ddd;border-radius:12px;padding:8px;' src='data:image/png;base64,"
                        + qrBase64 + "' alt='QR Envío' /></div>"
                : "";
        String contenido =
            "<h2>Nueva etiqueta de envío</h2>" +
            "<p>Para el pedido <b>#" + compraId + "</b> de <b>" + escapeHtml(tituloProducto) + "</b> se ha generado un nuevo código de envío.</p>" +
            "<div class='data-card'><div class='data-row'><span style='color:#6b7280'>Nuevo código</span><span style='font-family:monospace;font-weight:700;'>"
            + escapeHtml(nuevoCodigo) + "</span></div></div>" +
            qrHtml +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/compras/" + compraId + "/enviar' class='btn'>Ir a envío</a></div>";
        enviarEmailHtml(to, "Nueva etiqueta pedido #" + compraId, contenido);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Async
    public void enviarSolicitudDevolucionVendedor(String to, String tituloProducto) {
        String contenido =
            "<h2>Solicitud de devolución</h2>" +
            "<p>El comprador quiere devolver <b>" + escapeHtml(tituloProducto) + "</b>. Revisa la solicitud y responde en la app.</p>" +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/perfil?tab=compras' class='btn'>Ver pedido</a></div>";
        enviarEmailHtml(to, "Devolución solicitada — Nexus", contenido);
    }

    @Async
    public void enviarRespuestaDevolucionComprador(String to, String mensajePlano) {
        String contenido = "<h2>Actualización de tu devolución</h2><p>" + escapeHtml(mensajePlano) + "</p>";
        enviarEmailHtml(to, "Respuesta a tu devolución — Nexus", contenido);
    }

    @Async
    public void enviarReembolsoPlazoVencidoComprador(String to, String tituloProducto) {
        String contenido =
            "<h2>Compra reembolsada</h2>" +
            "<p>El vendedor no envió <b>" + escapeHtml(tituloProducto) + "</b> dentro del plazo. Hemos procesado el reembolso automáticamente.</p>" +
            "<p class='text-muted'>El importe volverá a tu método de pago en unos días hábiles.</p>" +
            "<div style='text-align:center;'><a href='" + frontendUrl + "/perfil?tab=compras' class='btn'>Ver mis compras</a></div>";
        enviarEmailHtml(to, "Reembolso por plazo de envío — Nexus", contenido);
    }
}