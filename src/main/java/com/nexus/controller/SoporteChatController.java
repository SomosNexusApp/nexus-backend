package com.nexus.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.service.SoporteChatService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/soporte/chat")
@Tag(name = "Soporte chat", description = "Chat de ayuda con IA (opcional)")
public class SoporteChatController {

    @Autowired
    private SoporteChatService soporteChatService;

    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> nuevaSesion(@RequestBody(required = false) Map<String, Integer> body) {
        Integer uid = body != null ? body.get("usuarioId") : null;
        return ResponseEntity.ok(soporteChatService.nuevaSesion(uid));
    }

    @PostMapping("/message")
    public ResponseEntity<?> mensaje(@RequestBody Map<String, String> body) {
        String token = body.get("sessionToken");
        String text = body.get("text");
        if (token == null || text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionToken y text requeridos"));
        }
        try {
            return ResponseEntity.ok(soporteChatService.enviarMensajeUsuario(token, text.trim()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionToken}/messages")
    public ResponseEntity<List<Map<String, Object>>> poll(@PathVariable String sessionToken) {
        try {
            return ResponseEntity.ok(soporteChatService.mensajesPorToken(sessionToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/session/survey")
    public ResponseEntity<?> survey(@RequestBody Map<String, Object> body) {
        String token = (String) body.get("sessionToken");
        Integer val = (Integer) body.get("valoracion");
        String com = (String) body.get("comentario");
        if (token == null || val == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionToken y valoracion requeridos"));
        }
        soporteChatService.guardarEncuesta(token, val, com);
        return ResponseEntity.ok().build();
    }
}
