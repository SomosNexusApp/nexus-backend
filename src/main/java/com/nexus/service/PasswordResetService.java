package com.nexus.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.Actor;
import com.nexus.repository.ActorRepository;

/**
 * flujo completo "Olvide mi contrasena":
 *
 *  1. Angular → POST /auth/forgot-password  { "email": "user@mail.com" }
 *  2. Servidor envia email con link: https://nexus-app.es/reset-password?token=UUID
 *  3. Angular → POST /auth/reset-password   { "token": "UUID", "nuevaPassword": "..." }
 *
 * el token expira en 15 minutos (configurable en application.properties).
 * se permite solicitar un nuevo token aunque ya exista uno → invalida el anterior.
 *
 * NOTA: ya no usamos la entidad PasswordResetToken. El token y su expiracion
 * se guardan directamente en el campo 'resetToken' y 'resetTokenExpira' de Actor.
 * Esto reduce una tabla y simplifica el UML.
 */
@Service
public class PasswordResetService {

    @Autowired private ActorRepository  actorRepository;
    @Autowired private EmailService     emailService;
    @Autowired private PasswordEncoder  passwordEncoder;

    @Value("${nexus.password-reset.expiry-minutes:15}")
    private int expiryMinutes;

    @Value("${nexus.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * genera un token y envia el email. si el email no existe, NO se lanza error
     * (evita enumerar usuarios validos — buena practica de seguridad).
     * si ya tenia un token previo, lo sobreescribimos (el anterior queda invalido).
     */
    @Transactional
    public void solicitarReset(String email) {
        Optional<Actor> actor = actorRepository.findByEmail(email);
        if (actor.isEmpty()) return; // silencioso por seguridad

        // generamos el token y lo guardamos directamente en el actor
        String token = UUID.randomUUID().toString();
        Actor a = actor.get();
        a.setResetToken(token);
        a.setResetTokenExpira(LocalDateTime.now().plusMinutes(expiryMinutes));
        actorRepository.save(a);

        String link = frontendUrl + "/reset-password?token=" + token;
        emailService.enviarEmailHtml(
            email,
            "Restablecer contraseña — Nexus",
            buildEmailHtml(a.getUser(), link)
        );
    }

    /**
     * valida el token y actualiza la contrasena.
     * @throws IllegalArgumentException si el token es invalido o expiro.
     */
    @Transactional
    public void resetearPassword(String token, String nuevaPassword) {
        // buscamos el actor por el token (campo unico en la tabla actor)
        Actor actor = actorRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (actor.getResetTokenExpira().isBefore(LocalDateTime.now())) {
            // limpiamos el token caducado y rechazamos
            actor.setResetToken(null);
            actor.setResetTokenExpira(null);
            actorRepository.save(actor);
            throw new IllegalArgumentException("El enlace ha expirado. Solicita uno nuevo.");
        }

        // token valido: actualizamos la contrasena y limpiamos el token (solo se usa una vez)
        actor.setPassword(passwordEncoder.encode(nuevaPassword));
        actor.setResetToken(null);
        actor.setResetTokenExpira(null);
        actorRepository.save(actor);
    }

    // ── email HTML de recuperacion ────────────────────────────────────────────

    private String buildEmailHtml(String username, String link) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:32px;background:#f9f9f9;border-radius:12px">
              <h2 style="color:#FF6B35">🔐 Restablecer contraseña</h2>
              <p>Hola <strong>%s</strong>,</p>
              <p>Recibimos una solicitud para restablecer tu contraseña en Nexus.</p>
              <p>Haz clic en el botón para crear una nueva contraseña. Este enlace es válido durante <strong>15 minutos</strong>.</p>
              <a href="%s" style="display:inline-block;padding:14px 28px;background:#FF6B35;color:#fff;text-decoration:none;border-radius:8px;font-weight:bold;margin:16px 0">
                Restablecer contraseña
              </a>
              <p style="color:#888;font-size:13px">Si no solicitaste este cambio, ignora este correo. Tu contraseña no cambiará.</p>
              <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
              <p style="color:#aaa;font-size:12px">© Nexus App</p>
            </div>
            """.formatted(username, link);
    }
}