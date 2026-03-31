package com.nexus.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.service.SoporteChatService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/soporte")
@Tag(name = "Admin soporte", description = "Conversaciones del chat de ayuda")
public class AdminSoporteController {

    @Autowired
    private SoporteChatService soporteChatService;

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> sesiones() {
        return ResponseEntity.ok(soporteChatService.listarSesionesAdmin());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<Map<String, Object>>> mensajes(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(soporteChatService.mensajesAdmin(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/takeover")
    public ResponseEntity<Void> takeover(@PathVariable Integer sessionId) {
        soporteChatService.takeoverAdmin(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{sessionId}/reply")
    public ResponseEntity<?> reply(@PathVariable Integer sessionId, @RequestBody Map<String, Object> body) {
        String text = (String) body.get("text");
        String tipo = (String) body.get("tipoContenido");
        Integer refId = null;
        if (body.get("referenciaId") != null) {
            try {
                refId = Integer.valueOf(body.get("referenciaId").toString());
            } catch (Exception ignored) {
            }
        }

        if ((text == null || text.isBlank()) && tipo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "text o tipoContenido requerido"));
        }
        soporteChatService.responderAdmin(sessionId, text != null ? text.trim() : "", tipo, refId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}/compras")
    public ResponseEntity<List<Map<String, Object>>> comprasSesion(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(soporteChatService.listarComprasUsuarioSesion(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/referencias")
    public ResponseEntity<List<Map<String, Object>>> referenciasSesion(
            @PathVariable Integer sessionId,
            @RequestParam String tipo,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(soporteChatService.listarReferenciasSesion(sessionId, tipo, q));
    }

    @PostMapping("/sessions/{sessionId}/compras/{compraId}/reembolsar")
    public ResponseEntity<?> reembolsarDesdeSoporte(
            @PathVariable Integer sessionId,
            @PathVariable Integer compraId,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.getOrDefault("motivo", "Reembolso gestionado desde soporte") : "Reembolso gestionado desde soporte";
        soporteChatService.reembolsarCompraDesdeSoporte(sessionId, compraId, motivo);
        return ResponseEntity.ok(Map.of("mensaje", "Reembolso procesado desde chat de soporte"));
    }

    @PostMapping("/sessions/{sessionId}/resume-ai")
    public ResponseEntity<Void> resumeAi(@PathVariable Integer sessionId) {
        soporteChatService.reanudarAi(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<Void> close(@PathVariable Integer sessionId) {
        soporteChatService.cerrarSesion(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{sessionId}/request-survey")
    public ResponseEntity<Void> requestSurvey(@PathVariable Integer sessionId) {
        soporteChatService.solicitarEncuesta(sessionId);
        return ResponseEntity.ok().build();
    }
}
