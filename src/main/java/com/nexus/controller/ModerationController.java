package com.nexus.controller;

import com.nexus.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
@Tag(name = "Moderación", description = "Servicios de moderación para administradores")
public class ModerationController {

    @Autowired
    private ModerationService moderationService;

    @PostMapping("/check-text")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // O hasRole('ADMIN') según tu SecurityConfig
    @Operation(summary = "Verificar si un texto es apropiado (Solo ADMIN)")
    public ResponseEntity<Map<String, Object>> checkText(@RequestBody Map<String, String> body) {
        String texto = body.getOrDefault("texto", "");
        
        boolean apropiado = moderationService.esContenidoApropiado(texto);
        
        if (apropiado) {
            return ResponseEntity.ok(Map.of(
                "apropiado", true, 
                "razon", "El contenido cumple las normas comunitarias."
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "apropiado", false, 
                "razon", "El contenido incluye lenguaje inapropiado y fue rechazado."
            ));
        }
    }
}