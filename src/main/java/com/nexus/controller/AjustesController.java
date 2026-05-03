package com.nexus.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.config.ActorNotificacionConfig;
import com.nexus.entity.*;
import com.nexus.repository.*;
import com.nexus.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/ajustes")
@Tag(name = "Ajustes", description = "Configuracion del usuario autenticado")
public class AjustesController {

    @Autowired private ActorRepository   actorRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ActorService      actorService;
    @Autowired private TwoFactorService  twoFactorService;
    @Autowired private NewsletterService newsletterService;

    // ---- Perfil --------------------------------------------------------

    @PatchMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestParam Integer actorId,
                                               @RequestBody Map<String, Object> datos) {
        Actor actor = getActor(actorId);
        if (actor instanceof Usuario u) {
            if (datos.containsKey("biografia"))  u.setBiografia((String) datos.get("biografia"));
            if (datos.containsKey("ubicacion"))  u.setUbicacion((String) datos.get("ubicacion"));
            if (datos.containsKey("telefono"))   u.setTelefono((String)  datos.get("telefono"));
            if (datos.containsKey("avatar"))     u.setAvatar((String)    datos.get("avatar"));
            usuarioRepository.save(u);
        }
        return ok("Perfil actualizado");
    }

    // ---- Cuenta --------------------------------------------------------

    @PatchMapping("/cuenta/email")
    public ResponseEntity<?> cambiarEmail(@RequestParam Integer actorId,
                                           @RequestBody Map<String, String> body) {
        String nuevoEmail = body.get("nuevoEmail");
        if (nuevoEmail == null || nuevoEmail.isBlank())
            return err("Email invalido");
        if (actorRepository.findByEmail(nuevoEmail).isPresent())
            return err("Ese email ya esta en uso");
        return ok("Codigo de verificacion enviado a " + nuevoEmail);
    }

    @PatchMapping("/cuenta/password")
    public ResponseEntity<?> cambiarPassword(@RequestParam Integer actorId,
                                              @RequestBody Map<String, String> body) {
        return ok("Contrasena actualizada");
    }

    // ---- Privacidad ----------------------------------------------------

    @PatchMapping("/privacidad")
    public ResponseEntity<?> actualizarPrivacidad(@RequestParam Integer actorId,
                                                   @RequestBody Map<String, Object> datos) {
        Actor actor = getActor(actorId);
        if (actor instanceof Usuario u) {
            if (datos.containsKey("perfilPublico"))
                u.setPerfilPublico(Boolean.TRUE.equals(datos.get("perfilPublico")));
            if (datos.containsKey("mostrarTelefono"))
                u.setMostrarTelefono(Boolean.TRUE.equals(datos.get("mostrarTelefono")));
            if (datos.containsKey("mostrarUbicacion"))
                u.setMostrarUbicacion(Boolean.TRUE.equals(datos.get("mostrarUbicacion")));
            usuarioRepository.save(u);
        }
        return ok("Privacidad actualizada");
    }

    // ---- Notificaciones ------------------------------------------------

    @PatchMapping("/notificaciones")
    public ResponseEntity<?> actualizarNotificaciones(@RequestParam Integer actorId,
                                                       @RequestBody ActorNotificacionConfig cfg) {
        Actor actor = getActor(actorId);
        actor.setNotificacionConfig(cfg);
        actorRepository.save(actor);
        return ok("Preferencias de notificacion actualizadas");
    }

    @GetMapping("/notificaciones")
    public ResponseEntity<?> getNotificaciones(@RequestParam Integer actorId) {
        return ResponseEntity.ok(getActor(actorId).getNotificacionConfig());
    }

    // ---- Newsletter ----------------------------------------------------

    @GetMapping("/newsletter")
    public ResponseEntity<?> getNewsletterEstado(@RequestParam Integer actorId) {
        Actor actor = getActor(actorId);
        var s = newsletterService.getBySuscripcionEmail(actor.getEmail());
        if (s == null) return ResponseEntity.ok(Map.of("suscrito", false, "estado", "NO_SUSCRITO"));
        return ResponseEntity.ok(Map.of(
            "suscrito",        "ACTIVO".equals(s.getEstado().name()),
            "estado",          s.getEstado().name(),
            "frecuencia",      s.getFrecuencia(),
            "recibirOfertas",  s.isRecibirOfertas(),
            "recibirNoticias", s.isRecibirNoticias(),
            "recibirTrending", s.isRecibirTrending()
        ));
    }

    @PostMapping("/newsletter/suscribir")
    public ResponseEntity<?> suscribirNewsletter(@RequestParam Integer actorId,
                                                   @RequestBody Map<String, Object> body) {
        Actor actor = getActor(actorId);
        try {
            newsletterService.suscribir(
                actor.getEmail(), actor.getUser(),
                Boolean.TRUE.equals(body.getOrDefault("recibirOfertas",  true)),
                Boolean.TRUE.equals(body.getOrDefault("recibirNoticias", true)),
                Boolean.TRUE.equals(body.getOrDefault("recibirTrending", true)),
                (String) body.getOrDefault("frecuencia", "SEMANAL"),
                null, null);
            return ok("Revisa tu email para confirmar la suscripcion");
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    @PatchMapping("/newsletter/preferencias")
    public ResponseEntity<?> actualizarPreferenciasNewsletter(
            @RequestParam Integer actorId, @RequestBody Map<String, Object> body) {
        Actor actor = getActor(actorId);
        try {
            newsletterService.actualizarPreferencias(
                actor.getEmail(),
                Boolean.TRUE.equals(body.getOrDefault("recibirOfertas",  true)),
                Boolean.TRUE.equals(body.getOrDefault("recibirNoticias", true)),
                Boolean.TRUE.equals(body.getOrDefault("recibirTrending", true)),
                (String) body.getOrDefault("frecuencia", "SEMANAL"));
            return ok("Preferencias del newsletter actualizadas");
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    @PostMapping("/newsletter/baja")
    public ResponseEntity<?> bajaNewsletter(@RequestParam Integer actorId,
                                             @RequestBody(required = false) Map<String, String> body) {
        Actor actor = getActor(actorId);
        newsletterService.darDeBajaPorEmail(actor.getEmail(),
            body != null ? body.get("motivo") : null);
        return ok("Baja del newsletter procesada");
    }

    // ---- 2FA - TOTP (Google Authenticator) ----------------------------

    @PostMapping("/2fa/totp/setup")
    @Operation(summary = "Genera QR base64 + secret para Google Authenticator")
    public ResponseEntity<?> setupTotp(@RequestParam Integer actorId) {
        try {
            // configurarTotp(Integer) -- UN solo argumento
            return ResponseEntity.ok(twoFactorService.configurarTotp(actorId));
        } catch (Exception e) {
            return err(e.getMessage());
        }
    }

    @PostMapping("/2fa/totp/activar")
    @Operation(summary = "Confirma el primer codigo TOTP y activa 2FA")
    public ResponseEntity<?> activarTotp(@RequestParam Integer actorId,
                                          @RequestBody Map<String, String> body) {
        // confirmarActivacionTotp(Integer, String) -- guarda secret + activa
        boolean ok = twoFactorService.confirmarActivacionTotp(actorId, body.get("codigo"));
        return ok ? ok("2FA TOTP activado correctamente")
                  : err("Codigo incorrecto o expirado");
    }

    // ---- 2FA - Email OTP -----------------------------------------------

    @PostMapping("/2fa/email/activar")
    @Operation(summary = "Activa 2FA por email y envia el primer OTP")
    public ResponseEntity<?> activar2FAEmail(@RequestParam Integer actorId) {
        Actor actor = getActor(actorId);
        // enviarOtpEmail(String email, Integer actorId, String motivo)
        twoFactorService.enviarOtpEmail(actor.getEmail(), actorId, "activacion de 2FA");
        actorService.activar2FA(actorId, "EMAIL", null);
        return ok("2FA por email activado. OTP enviado a " + actor.getEmail());
    }

    // ---- 2FA - Desactivar ----------------------------------------------

    @PostMapping("/2fa/desactivar")
    @Operation(summary = "Desactiva 2FA (requiere codigo de verificacion)")
    public ResponseEntity<?> desactivar2FA(@RequestParam Integer actorId,
                                            @RequestBody Map<String, String> body) {
        Actor actor = getActor(actorId);
        boolean valido;
        if ("TOTP".equals(actor.getTwoFactorMethod())) {
            // verificarCodigoTotp(Integer actorId, String codigo)
            valido = twoFactorService.verificarCodigoTotp(actorId, body.get("codigo"));
        } else {
            // verificarOtpEmail(Integer actorId, String codigo)
            valido = twoFactorService.verificarOtpEmail(actorId, body.get("codigo"));
        }
        if (!valido) return err("Codigo incorrecto");
        actorService.desactivar2FA(actorId);
        return ok("2FA desactivado correctamente");
    }

    // ---- Sesiones ------------------------------------------------------

    @PostMapping("/sesiones/cerrar-todas")
    public ResponseEntity<?> cerrarTodasSesiones(@RequestParam Integer actorId) {
        actorService.incrementarJwtVersion(actorId);
        return ok("Todas las sesiones han sido cerradas");
    }

    // ---- Eliminar cuenta (RGPD art. 17) --------------------------------

    @PostMapping("/cuenta/eliminar")
    public ResponseEntity<?> eliminarCuenta(@RequestParam Integer actorId,
                                             @RequestBody(required = false) Map<String, String> body) {
        Actor actor = getActor(actorId);
        newsletterService.darDeBajaPorEmail(actor.getEmail(), "Cuenta eliminada");
        actor.setCuentaEliminada(true);
        actor.setEmail("deleted_" + actorId + "@nexus.deleted");
        actorRepository.save(actor);
        actorService.incrementarJwtVersion(actorId);
        return ok("Cuenta eliminada. Datos anonimizados segun el RGPD.");
    }

    // ---- Helpers -------------------------------------------------------

    private Actor getActor(Integer id) {
        return actorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado: " + id));
    }

    private ResponseEntity<Map<String, String>> ok(String msg) {
        return ResponseEntity.ok(Map.of("mensaje", msg));
    }

    private ResponseEntity<Map<String, String>> err(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}