package com.nexus.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.NewsletterSuscripcion;
import com.nexus.service.NewsletterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Newsletter API - endpoints publicos y autenticados.
 *
 * Flujo Angular:
 *   1. Footer / landing:
 *      POST /newsletter/suscribir  { email, nombre, recibirOfertas, ... }
 *      -> Muestra: "Revisa tu email para confirmar"
 *
 *   2. Link en el email de confirmacion:
 *      GET /newsletter/confirmar?t=<token>
 *      -> Redirige a frontendUrl + "/newsletter/confirmado"
 *
 *   3. Link de baja en cada email:
 *      GET /newsletter/baja?t=<token>
 *      -> Redirige a frontendUrl + "/newsletter/baja-confirmada"
 *
 *   4. Ajustes del usuario autenticado:
 *      GET  /newsletter/estado?email=...      -> { activo, preferencias }
 *      POST /newsletter/preferencias          -> actualizar preferencias
 *      POST /newsletter/baja                  -> darse de baja autenticado
 */
@RestController
@RequestMapping("/newsletter")
@Tag(name = "Newsletter", description = "Suscripciones al newsletter (RGPD compliant)")
public class NewsletterController {

    @Autowired private NewsletterService newsletterService;

    @Value("${nexus.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // ---- Suscribirse (publica, rellena formulario del footer) -----------

    @PostMapping("/suscribir")
    @Operation(summary = "Suscribirse al newsletter (paso 1: envia email de confirmacion)")
    public ResponseEntity<?> suscribir(@RequestBody SuscribirRequest req,
                                        HttpServletRequest request) {
        try {
            NewsletterSuscripcion s = newsletterService.suscribir(
                req.email(), req.nombre(),
                req.recibirOfertas() != null  ? req.recibirOfertas()  : true,
                req.recibirNoticias() != null ? req.recibirNoticias() : true,
                req.recibirTrending() != null ? req.recibirTrending() : true,
                req.frecuencia(),
                null, request);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Revisa tu email para confirmar la suscripcion",
                "email",   s.getEmail()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al procesar la suscripcion"));
        }
    }

    // ---- Confirmar (link en el email, redirige al frontend) -------------

    @GetMapping("/confirmar")
    @Operation(summary = "Confirmar suscripcion via token del email (double opt-in paso 2)")
    public ResponseEntity<Void> confirmar(@RequestParam("t") String token) {
        boolean ok = newsletterService.confirmar(token);
        String destino = ok
            ? frontendUrl + "/newsletter/confirmado"
            : frontendUrl + "/newsletter/error?motivo=token-invalido";
        return ResponseEntity.status(302)
            .header("Location", destino)
            .build();
    }

    // ---- Darse de baja via token (link en el footer de cada email) ------

    @GetMapping("/baja")
    @Operation(summary = "Darse de baja via token del email (link en footer)")
    public ResponseEntity<Void> bajaViaToken(@RequestParam("t") String token,
                                              @RequestParam(required = false) String motivo) {
        boolean ok = newsletterService.darDeBajaConToken(token, motivo);
        String destino = ok
            ? frontendUrl + "/darse-baja?estado=ok"
            : frontendUrl + "/darse-baja?estado=error";
        return ResponseEntity.status(302)
            .header("Location", destino)
            .build();
    }

    // ---- Estado actual (desde ajustes del usuario) ----------------------

    @GetMapping("/estado")
    @Operation(summary = "Obtener estado de suscripcion para un email")
    public ResponseEntity<?> estado(@RequestParam String email) {
        NewsletterSuscripcion s = newsletterService.getBySuscripcionEmail(email);
        if (s == null) {
            return ResponseEntity.ok(Map.of(
                "suscrito", false,
                "estado",   "NO_SUSCRITO"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "suscrito",        s.getEstado().name().equals("ACTIVO"),
            "estado",          s.getEstado().name(),
            "frecuencia",      s.getFrecuencia(),
            "recibirOfertas",  s.isRecibirOfertas(),
            "recibirNoticias", s.isRecibirNoticias(),
            "recibirTrending", s.isRecibirTrending()
        ));
    }

    // ---- Actualizar preferencias (usuario autenticado, desde ajustes) ---

    @PostMapping("/preferencias")
    @Operation(summary = "Actualizar preferencias del newsletter (usuario autenticado)")
    public ResponseEntity<?> actualizarPreferencias(
            @RequestBody PreferenciasRequest req) {
        try {
            NewsletterSuscripcion s = newsletterService.actualizarPreferencias(
                req.email(),
                req.recibirOfertas() != null  ? req.recibirOfertas()  : true,
                req.recibirNoticias() != null ? req.recibirNoticias() : true,
                req.recibirTrending() != null ? req.recibirTrending() : true,
                req.frecuencia() != null ? req.frecuencia() : "SEMANAL"
            );
            return ResponseEntity.ok(Map.of("mensaje", "Preferencias actualizadas", "suscripcion", s));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Baja desde ajustes (usuario autenticado) -----------------------

    @PostMapping("/baja")
    @Operation(summary = "Darse de baja del newsletter (usuario autenticado desde ajustes)")
    public ResponseEntity<?> bajaAutenticado(@RequestBody BajaRequest req) {
        boolean ok = newsletterService.darDeBajaPorEmail(req.email(), req.motivo());
        if (ok) return ResponseEntity.ok(Map.of("mensaje", "Baja procesada correctamente"));
        return ResponseEntity.badRequest().body(Map.of("error", "No se encontro suscripcion activa"));
    }

    // ---- Records (DTO inline) -------------------------------------------

    record SuscribirRequest(
        String email,
        String nombre,
        Boolean recibirOfertas,
        Boolean recibirNoticias,
        Boolean recibirTrending,
        String frecuencia
    ) {}

    record PreferenciasRequest(
        String email,
        Boolean recibirOfertas,
        Boolean recibirNoticias,
        Boolean recibirTrending,
        String frecuencia
    ) {}

    record BajaRequest(String email, String motivo) {}
}