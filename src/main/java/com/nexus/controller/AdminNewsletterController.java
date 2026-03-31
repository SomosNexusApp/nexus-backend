package com.nexus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.nexus.service.NewsletterService;
import com.nexus.entity.NewsletterConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/newsletter")
@Tag(name = "Admin Newsletter", description = "Gestión administrativa de boletines y suscriptores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsletterController {

    @Autowired private NewsletterService newsletterService;

    @GetMapping("/stats")
    @Operation(summary = "Obtener estadísticas de suscripciones")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(newsletterService.getNewsletterStats());
    }

    @PostMapping("/prueba")
    @Operation(summary = "Enviar un correo de prueba a una dirección específica")
    public ResponseEntity<?> enviarPrueba(@RequestBody NewsletterRequest req) {
        if (req.email() == null || req.asunto() == null || req.contenido() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos obligatorios"));
        }
        newsletterService.enviarNewsletterPrueba(req.email(), req.asunto(), req.contenido());
        return ResponseEntity.ok(Map.of("mensaje", "Correo de prueba enviado a " + req.email()));
    }

    @PostMapping("/enviar")
    @Operation(summary = "Enviar newsletter a todos los suscriptores activos")
    public ResponseEntity<?> enviarATodos(@RequestBody NewsletterRequest req) {
        if (req.asunto() == null || req.contenido() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Asunto y contenido son obligatorios"));
        }
        newsletterService.enviarAActivos(req.asunto(), req.contenido());
        return ResponseEntity.ok(Map.of("mensaje", "Proceso de envío masivo iniciado"));
    }

    @GetMapping("/config")
    @Operation(summary = "Obtener configuración de automatización semanal")
    public ResponseEntity<?> getConfig() {
        return ResponseEntity.ok(newsletterService.getConfig());
    }

    @PostMapping("/config")
    @Operation(summary = "Guardar configuración de automatización semanal")
    public ResponseEntity<?> saveConfig(@RequestBody NewsletterConfig config) {
        return ResponseEntity.ok(newsletterService.saveConfig(config));
    }

    @GetMapping("/preview-weekly")
    @Operation(summary = "Previsualizar el contenido dinámico de la semana")
    public ResponseEntity<?> previewWeekly() {
        return ResponseEntity.ok(Map.of("html", newsletterService.generateWeeklyDigestHtml()));
    }

    @PostMapping("/trigger-weekly")
    @Operation(summary = "Lanzar manualmente el boletín semanal a todos")
    public ResponseEntity<?> triggerWeekly() {
        String asunto = "Nexus Elite: Selección Semanal ✨";
        String html = newsletterService.generateWeeklyDigestHtml();
        newsletterService.enviarAActivos(asunto, html);
        return ResponseEntity.ok(Map.of("mensaje", "Envío semanal forzado correctamente"));
    }

    // Record para las peticiones
    public record NewsletterRequest(String email, String asunto, String contenido) {}
}
