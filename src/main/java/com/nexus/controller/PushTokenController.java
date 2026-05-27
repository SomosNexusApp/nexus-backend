package com.nexus.controller;

import com.nexus.entity.Actor;
import com.nexus.entity.PushToken;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.PushTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Gestiona los tokens FCM de los dispositivos de los usuarios.
 * El frontend Angular/Capacitor llama a estos endpoints para:
 *   - Registrar un token cuando el usuario hace login o la app se inicia
 *   - Eliminar el token cuando el usuario hace logout
 */
@RestController
@RequestMapping("/api/push-tokens")
public class PushTokenController {

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private ActorRepository actorRepository;

    /**
     * Registra o actualiza un token FCM para el usuario autenticado.
     * Hace upsert: si el token ya existe, lo reactiva y actualiza la fecha.
     *
     * Body esperado: { "token": "...", "plataforma": "android" | "ios" }
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Void> registrarToken(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String token = body.get("token");
        String plataforma = body.getOrDefault("plataforma", "android");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String username = auth.getName();
        Actor actor = actorRepository.findByUser(username).orElse(null);
        if (actor == null) {
            return ResponseEntity.notFound().build();
        }

        // Upsert: si el token ya existe lo actualizamos, si no lo creamos
        Optional<PushToken> existing = pushTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            PushToken pt = existing.get();
            pt.setActor(actor); // puede haber cambiado de usuario (re-login)
            pt.setPlataforma(plataforma);
            pt.setFechaRegistro(LocalDateTime.now());
            pt.setActivo(true);
            pushTokenRepository.save(pt);
        } else {
            PushToken pt = new PushToken();
            pt.setActor(actor);
            pt.setToken(token);
            pt.setPlataforma(plataforma);
            pt.setFechaRegistro(LocalDateTime.now());
            pt.setActivo(true);
            pushTokenRepository.save(pt);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Elimina (desactiva) un token FCM al hacer logout.
     *
     * @param token El valor del token a eliminar (path variable, URL-encoded si necesario)
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> eliminarToken(
            @RequestParam String token,
            Authentication auth) {

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        pushTokenRepository.deactivateByToken(token);
        return ResponseEntity.noContent().build();
    }
}
