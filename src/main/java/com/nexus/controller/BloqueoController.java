package com.nexus.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nexus.service.BloqueoService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/bloqueo")
@Tag(name = "Bloqueos", description = "Bloquear y desbloquear usuarios")
public class BloqueoController {

    @Autowired private BloqueoService bloqueoService;

    /** POST /bloqueo?bloqueadorId=5&bloqueadoId=7 */
    @PostMapping
    public ResponseEntity<?> bloquear(
            @RequestParam Integer bloqueadorId,
            @RequestParam Integer bloqueadoId,
            @RequestParam(required = false, defaultValue = "") String motivo) {
        try {
            bloqueoService.bloquear(bloqueadorId, bloqueadoId, motivo);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario bloqueado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> desbloquear(
            @RequestParam Integer bloqueadorId, @RequestParam Integer bloqueadoId) {
        bloqueoService.desbloquear(bloqueadorId, bloqueadoId);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario desbloqueado"));
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<Integer>> listaBloqueados(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(bloqueoService.getIdsBloqueados(usuarioId));
    }

    @GetMapping("/comprobar")
    public ResponseEntity<Map<String, Boolean>> comprobar(
            @RequestParam Integer bloqueadorId, @RequestParam Integer bloqueadoId) {
        return ResponseEntity.ok(Map.of("bloqueado",
            bloqueoService.estaBloqueado(bloqueadorId, bloqueadoId)));
    }
}