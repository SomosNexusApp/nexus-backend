package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.*;
import com.nexus.repository.*;
import org.springframework.data.domain.PageRequest;

/**
 * Servicio de Newsletter - cumplimiento RGPD / LSSI.
 *
 * FLUJO double opt-in (obligatorio por RGPD):
 *   1. POST /newsletter/suscribir        -> estado PENDIENTE, envia email con tokenConfirmacion
 *   2. GET  /newsletter/confirmar?t=...  -> estado ACTIVO
 *
 * FLUJO de baja (obligatorio por LSSI art. 22):
 *   3. GET  /newsletter/baja?t=...       -> estado BAJA (irreversible por el usuario)
 *      O bien  POST /newsletter/baja     -> baja desde los ajustes del usuario autenticado
 *
 * El tokenBaja se incluye en el footer de CADA email enviado.
 *
 * Se guardan: fecha y IP del consentimiento, version de politica de privacidad.
 * Nunca se elimina el registro (soft-delete) para poder demostrar el consentimiento
 * y la baja ante la AEPD si fuera necesario.
 */
@Service
public class NewsletterService {

    @Autowired private NewsletterRepository newsletterRepository;
    @Autowired private NewsletterConfigRepository newsletterConfigRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private OfertaRepository ofertaRepository;
    @Autowired private EmailService         emailService;

    @Value("${nexus.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${nexus.api.url:http://localhost:8080}")
    private String apiUrl;

    @Value("${nexus.newsletter.version-politica:1.0}")
    private String versionPolitica;

    // ---- Suscripcion (double opt-in paso 1) ----------------------------

    @Transactional
    public NewsletterSuscripcion suscribir(String email, String nombre,
                                            boolean recibirOfertas, boolean recibirNoticias,
                                            boolean recibirTrending, String frecuencia,
                                            String ip, HttpServletRequest request) {
        // Si ya existe una suscripcion activa, no hacer nada
        newsletterRepository.findByEmail(email).ifPresent(s -> {
            if (s.getEstado() == EstadoSuscripcion.ACTIVO)
                throw new IllegalStateException("Este email ya esta suscrito");
            // Si estaba de baja, reactivar con nuevo double opt-in
        });

        NewsletterSuscripcion s = newsletterRepository.findByEmail(email)
            .orElse(new NewsletterSuscripcion());

        s.setEmail(email);
        s.setNombre(nombre);
        s.setEstado(EstadoSuscripcion.PENDIENTE);
        s.setTokenConfirmacion(UUID.randomUUID().toString());
        s.setFechaEnvioConfirmacion(LocalDateTime.now());
        s.setRecibirOfertas(recibirOfertas);
        s.setRecibirNoticias(recibirNoticias);
        s.setRecibirTrending(recibirTrending);
        s.setFrecuencia(frecuencia != null ? frecuencia : "SEMANAL");
        s.setFechaConsentimiento(LocalDateTime.now());
        s.setIpConsentimiento(obtenerIp(request, ip));
        s.setVersionPolitica(versionPolitica);
        s.setFechaBaja(null);
        s.setMotivoBaja(null);

        // Generar tokenBaja unico si no tiene
        if (s.getTokenBaja() == null)
            s.setTokenBaja(UUID.randomUUID().toString());

        NewsletterSuscripcion guardada = newsletterRepository.save(s);
        enviarEmailConfirmacion(guardada);
        return guardada;
    }

    // ---- Confirmacion double opt-in (paso 2) ---------------------------

    @Transactional
    public boolean confirmar(String token) {
        NewsletterSuscripcion s = newsletterRepository
            .findByTokenConfirmacion(token).orElse(null);
        if (s == null) return false;
        if (s.getEstado() == EstadoSuscripcion.ACTIVO) return true; // ya confirmado

        // El link de confirmacion tiene 48h de validez
        if (s.getFechaEnvioConfirmacion() != null
                && s.getFechaEnvioConfirmacion().isBefore(LocalDateTime.now().minusHours(48))) {
            return false;
        }

        s.setEstado(EstadoSuscripcion.ACTIVO);
        s.setFechaConfirmacion(LocalDateTime.now());
        s.setTokenConfirmacion(null); // invalidar el token
        newsletterRepository.save(s);
        enviarEmailBienvenida(s);
        return true;
    }

    // ---- Baja por token (link en el footer del email) ------------------

    @Transactional
    public boolean darDeBajaConToken(String token, String motivo) {
        NewsletterSuscripcion s = newsletterRepository
            .findByTokenBaja(token).orElse(null);
        if (s == null) return false;
        if (s.getEstado() == EstadoSuscripcion.BAJA) return true;

        s.setEstado(EstadoSuscripcion.BAJA);
        s.setFechaBaja(LocalDateTime.now());
        s.setMotivoBaja(motivo);
        newsletterRepository.save(s);
        enviarEmailConfirmacionBaja(s);
        return true;
    }

    // ---- Baja desde ajustes del usuario autenticado --------------------

    @Transactional
    public boolean darDeBajaPorEmail(String email, String motivo) {
        return newsletterRepository.findByEmail(email)
            .map(s -> {
                if (s.getEstado() == EstadoSuscripcion.BAJA) return true;
                s.setEstado(EstadoSuscripcion.BAJA);
                s.setFechaBaja(LocalDateTime.now());
                s.setMotivoBaja(motivo);
                newsletterRepository.save(s);
                enviarEmailConfirmacionBaja(s);
                return true;
            }).orElse(false);
    }

    // ---- Admin Actions --------------------------------------------------

    public Map<String, Long> getNewsletterStats() {
        return Map.of(
            "total", newsletterRepository.count(),
            "activos", newsletterRepository.countByEstado(EstadoSuscripcion.ACTIVO),
            "pendientes", newsletterRepository.countByEstado(EstadoSuscripcion.PENDIENTE),
            "bajas", newsletterRepository.countByEstado(EstadoSuscripcion.BAJA)
        );
    }

    @Async
    public void enviarNewsletterPrueba(String targetEmail, String asunto, String htmlContent) {
        String testTag = "<div style='background:#fff3cd; color:#856404; padding:10px; text-align:center; font-size:12px; margin-bottom:20px;'>MODO PRUEBA: Este es un correo de test</div>";
        emailService.enviarEmailHtml(targetEmail, "[PRUEBA] " + asunto, testTag + htmlContent);
    }

    @Async
    @Transactional(readOnly = true)
    public void enviarAActivos(String asunto, String htmlContent) {
        List<NewsletterSuscripcion> activos = newsletterRepository.findByEstado(EstadoSuscripcion.ACTIVO);
        for (NewsletterSuscripcion s : activos) {
            String htmlConFooter = agregarFooterBaja(htmlContent, s.getTokenBaja(), s.getNombre());
            emailService.enviarEmailHtml(s.getEmail(), asunto, htmlConFooter);
        }
    }

    // --- GESTIÓN DE CONFIGURACIÓN SEMANAL ---

    public NewsletterConfig getConfig() {
        return newsletterConfigRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    NewsletterConfig nc = new NewsletterConfig();
                    return newsletterConfigRepository.save(nc);
                });
    }

    public NewsletterConfig saveConfig(NewsletterConfig nc) {
        NewsletterConfig current = getConfig();
        current.setAutomatedEnabled(nc.isAutomatedEnabled());
        current.setDayOfWeek(nc.getDayOfWeek());
        current.setTimeOfDay(nc.getTimeOfDay());
        return newsletterConfigRepository.save(current);
    }

    // --- GENERACIÓN DE RESUMEN SEMANAL ---

    public String generateWeeklyDigestHtml() {
        // 1. Obtener top productos (3 más actuales)
        List<Producto> topProductos = productoRepository.buscarConFiltros(null, null, null, null, PageRequest.of(0, 3)).getContent();

        // 2. Obtener top ofertas (3 con más Sparks)
        List<Oferta> topOfertas = ofertaRepository.findTopBySparkScore(PageRequest.of(0, 3));

        StringBuilder sb = new StringBuilder();
        sb.append("<h2 style='text-align:center;'>Lo mejor de Nexus esta semana</h2>");
        sb.append("<p style='text-align:center; color:#64748b;'>Hemos seleccionado estas piezas exclusivas y chollazos para ti.</p>");

        // Sección Ofertas
        if (!topOfertas.isEmpty()) {
            sb.append("<div style='margin-top:40px;'>");
            sb.append("<h3 style='color:#7c3aed; border-bottom:2px solid #f5f3ff; padding-bottom:10px;'>🔥 Chollazos Destacados</h3>");
            for (Oferta o : topOfertas) {
                sb.append("<div style='display:flex; margin-bottom:20px; background:#f8fafc; border-radius:12px; padding:15px; border:1px solid #e2e8f0;'>");
                String img = o.getImagenPrincipal();
                if (img == null || img.isEmpty()) {
                   img = "https://nexus-app.es/logo.webp"; // Fallback publico
                } else if (!img.startsWith("http")) {
                    img = (img.startsWith("/") ? apiUrl : apiUrl + "/") + img;
                }
                sb.append("<img src='").append(img).append("' style='width:100px; height:100px; object-fit:cover; border-radius:8px; margin-right:15px;'>");
                sb.append("<div>");
                sb.append("<h4 style='margin:0; font-size:16px;'>").append(o.getTitulo()).append("</h4>");
                sb.append("<p style='margin:5px 0; color:#7c3aed; font-weight:700; font-size:18px;'>").append(o.getPrecioOferta()).append("€ <span style='color:#64748b; font-size:14px; text-decoration:line-through; font-weight:400;'>").append(o.getPrecioOriginal()).append("€</span></p>");
                sb.append("<p style='margin:0; font-size:12px; color:#64748b;'>Tienda: ").append(o.getTienda()).append("</p>");
                sb.append("</div></div>");
            }
            sb.append("</div>");
        }

        // Sección Productos
        if (!topProductos.isEmpty()) {
            sb.append("<div style='margin-top:40px;'>");
            sb.append("<h3 style='color:#7c3aed; border-bottom:2px solid #f5f3ff; padding-bottom:10px;'>✨ Últimas Novedades</h3>");
            for (Producto p : topProductos) {
                sb.append("<div style='display:flex; margin-bottom:20px; background:#f8fafc; border-radius:12px; padding:15px; border:1px solid #e2e8f0;'>");
                String img = p.getImagenPrincipal();
                if (img == null || img.isEmpty()) {
                    img = "https://nexus-app.es/logo.webp"; // Fallback publico
                } else if (!img.startsWith("http")) {
                    img = (img.startsWith("/") ? apiUrl : apiUrl + "/") + img;
                }
                sb.append("<img src='").append(img).append("' style='width:100px; height:100px; object-fit:cover; border-radius:8px; margin-right:15px;'>");
                sb.append("<div>");
                sb.append("<h4 style='margin:0; font-size:16px;'>").append(p.getTitulo()).append("</h4>");
                sb.append("<p style='margin:5px 0; color:#0f172a; font-weight:700; font-size:18px;'>").append(p.getPrecio()).append("€</p>");
                sb.append("<p style='margin:0; font-size:12px; color:#64748b;'>Vendedor: ").append(p.getVendedor().getUser()).append("</p>");
                sb.append("</div></div>");
            }
            sb.append("</div>");
        }

        sb.append("<div style='text-align:center; margin-top:40px;'>");
        sb.append("<a href='https://nexus-app.es/market' class='btn'>Ver todo en Nexus</a>");
        sb.append("</div>");

        return sb.toString();
    }

    // ---- Actualizar preferencias (desde ajustes) -----------------------

    @Transactional
    public NewsletterSuscripcion actualizarPreferencias(String email,
                                                         boolean ofertas,
                                                         boolean noticias,
                                                         boolean trending,
                                                         String frecuencia) {
        NewsletterSuscripcion s = newsletterRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No hay suscripcion para ese email"));
        if (s.getEstado() != EstadoSuscripcion.ACTIVO)
            throw new IllegalStateException("La suscripcion no esta activa");

        s.setRecibirOfertas(ofertas);
        s.setRecibirNoticias(noticias);
        s.setRecibirTrending(trending);
        s.setFrecuencia(frecuencia);
        return newsletterRepository.save(s);
    }

    // ---- Estado de suscripcion (para el frontend) ----------------------

    public boolean estaActivo(String email) {
        return newsletterRepository.findByEmail(email)
            .map(s -> s.getEstado() == EstadoSuscripcion.ACTIVO)
            .orElse(false);
    }

    public NewsletterSuscripcion getBySuscripcionEmail(String email) {
        return newsletterRepository.findByEmail(email).orElse(null);
    }

    // ---- Envio de newsletters (desde admin) ----------------------------

    @Async
    public void enviarNewsletterOfertas(List<String> emailsDestino, String asunto, String html) {
        for (String email : emailsDestino) {
            newsletterRepository.findByEmail(email).ifPresent(s -> {
                if (s.getEstado() == EstadoSuscripcion.ACTIVO && s.isRecibirOfertas()) {
                    String htmlConFooter = agregarFooterBaja(html, s.getTokenBaja(), s.getNombre());
                    emailService.enviarEmailHtml(email, asunto, htmlConFooter);
                }
            });
        }
    }

    // Limpieza automatica: borrar tokens de confirmacion expirados (>7 dias)
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void limpiarPendientesExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusDays(7);
        newsletterRepository.findByEstado(EstadoSuscripcion.PENDIENTE).stream()
            .filter(s -> s.getFechaEnvioConfirmacion() != null
                      && s.getFechaEnvioConfirmacion().isBefore(limite))
            .forEach(s -> {
                // No eliminar: solo marcar como bloqueado para auditoria
                s.setEstado(EstadoSuscripcion.BLOQUEADO);
                newsletterRepository.save(s);
            });
    }

    // ---- Emails ---------------------------------------------------------

    @Async
    private void enviarEmailConfirmacion(NewsletterSuscripcion s) {
        String link = frontendUrl + "/newsletter/confirmar?t=" + s.getTokenConfirmacion();
        String html = "<html><body style='font-family:sans-serif;max-width:600px;margin:auto'>"
            + "<h2>Confirma tu suscripcion a Nexus Newsletter</h2>"
            + "<p>Hola " + (s.getNombre() != null ? s.getNombre() : "") + ",</p>"
            + "<p>Haz clic en el siguiente boton para confirmar que quieres suscribirte:</p>"
            + "<a href='" + link + "' style='background:#FF5722;color:#fff;padding:12px 24px;"
            + "border-radius:6px;text-decoration:none;display:inline-block;margin:16px 0'>"
            + "Confirmar suscripcion</a>"
            + "<p style='color:#666;font-size:12px'>Si no solicitaste esta suscripcion, "
            + "ignora este email. El enlace expira en 48 horas.</p>"
            + "<p style='color:#666;font-size:12px'>Nexus - Plataforma de compraventa. "
            + "Tu IP de registro: " + s.getIpConsentimiento() + "</p>"
            + "</body></html>";
        emailService.enviarEmailHtml(s.getEmail(), "Confirma tu suscripcion al Newsletter de Nexus", html);
    }

    @Async
    private void enviarEmailBienvenida(NewsletterSuscripcion s) {
        String html = "<html><body style='font-family:sans-serif;max-width:600px;margin:auto'>"
            + "<h2>Bienvenido/a al Newsletter de Nexus</h2>"
            + "<p>Hola " + (s.getNombre() != null ? s.getNombre() : "") + ",</p>"
            + "<p>Ya estas suscrito/a. Recibiras las mejores ofertas y novedades segun "
            + "tus preferencias.</p>"
            + "<p><b>Tu frecuencia:</b> " + s.getFrecuencia() + "<br>"
            + "<b>Recibiras:</b>"
            + (s.isRecibirOfertas()   ? " Ofertas" : "") + " "
            + (s.isRecibirNoticias()  ? " Noticias" : "") + " "
            + (s.isRecibirTrending()  ? " Trending" : "") + "</p>"
            + "<p>Puedes cambiar tus preferencias en cualquier momento desde "
            + "<a href='" + frontendUrl + "/ajustes/notificaciones'>Ajustes</a>.</p>"
            + agregarFooterBaja("", s.getTokenBaja(), s.getNombre())
            + "</body></html>";
        emailService.enviarEmailHtml(s.getEmail(), "Bienvenido/a al Newsletter de Nexus", html);
    }

    @Async
    private void enviarEmailConfirmacionBaja(NewsletterSuscripcion s) {
        String html = "<html><body style='font-family:sans-serif;max-width:600px;margin:auto'>"
            + "<h2>Has sido dado/a de baja del Newsletter de Nexus</h2>"
            + "<p>Hola " + (s.getNombre() != null ? s.getNombre() : "") + ",</p>"
            + "<p>Hemos procesado tu solicitud de baja correctamente. "
            + "No volveremos a enviarte emails de marketing.</p>"
            + "<p>Si esto fue un error, puedes volver a suscribirte en: "
            + "<a href='" + frontendUrl + "/newsletter'>Suscribirte de nuevo</a></p>"
            + "<p style='color:#666;font-size:12px'>Nexus cumple con el RGPD (UE 2016/679) "
            + "y la LSSI. Tus datos se conservan de forma anonimizada segun lo requerido "
            + "por la normativa de auditoria.</p>"
            + "</body></html>";
        emailService.enviarEmailHtml(s.getEmail(), "Baja del Newsletter de Nexus confirmada", html);
    }

    private String agregarFooterBaja(String html, String tokenBaja, String nombre) {
        String bajaLink = frontendUrl + "/newsletter/baja?t=" + tokenBaja;
        return html + "<hr style='margin-top:40px'>"
            + "<p style='color:#999;font-size:11px;text-align:center'>"
            + "Has recibido este email porque estas suscrito/a al newsletter de Nexus. "
            + "<a href='" + bajaLink + "'>Darse de baja</a> | "
            + "<a href='" + frontendUrl + "/ajustes/notificaciones'>Gestionar preferencias</a>"
            + "<br>Nexus S.L. · Calle Ejemplo 1, 28001 Madrid · Spain</p>";
    }

    private String obtenerIp(HttpServletRequest request, String fallback) {
        if (request == null) return fallback != null ? fallback : "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        // Si hay varias IPs (proxy chain) coger la primera
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip != null ? ip : "unknown";
    }
}