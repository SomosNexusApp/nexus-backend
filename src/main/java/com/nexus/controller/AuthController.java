package com.nexus.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.UsuarioRepository;
import com.nexus.service.CaptchaService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ActorRepository actorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CaptchaService captchaService;

    // DTO Interno para recibir la petición
    public static class RegisterRequest {
        public String username;
        public String email;
        public String password;
        public String nombre;
        public String apellidos;
        public boolean terminosAceptados;
        public boolean newsletterSuscrito;
        public String captchaToken;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        
        // 1. Validar Captcha (corregido a verificar)
        if (!captchaService.verificar(req.captchaToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Captcha inválido."));
        }

        // 2. Validar Términos Aceptados (Obligatorio)
        if (!req.terminosAceptados) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Debes aceptar los términos y condiciones para registrarte."));
        }

        // 3. Validar disponibilidad de Email y Username (corregido a findByUsername)
        if (actorRepository.findByEmail(req.email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El email ya está en uso."));
        }
        if (actorRepository.findByUsername(req.username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El nombre de usuario ya está en uso."));
        }

        // 4. Crear el Usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUser(req.username);
        nuevoUsuario.setEmail(req.email);
        nuevoUsuario.setPassword(passwordEncoder.encode(req.password));
        
        // Nuevos campos base
        nuevoUsuario.setNombre(req.nombre);
        nuevoUsuario.setApellidos(req.apellidos);
        
        // Nuevos campos específicos
        nuevoUsuario.setTerminosAceptados(req.terminosAceptados);
        nuevoUsuario.setFechaAceptacionTerminos(LocalDateTime.now());
        nuevoUsuario.setVersionTerminosAceptados("1.0"); 
        nuevoUsuario.setNewsletterSuscrito(req.newsletterSuscrito);

        // Guardar
        usuarioRepository.save(nuevoUsuario);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("mensaje", "Usuario registrado exitosamente.", "id", nuevoUsuario.getId()));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Comprueba si un email está disponible (true) o ya existe (false)")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean disponible = actorRepository.findByEmail(email).isEmpty();
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    @GetMapping("/check-username")
    @Operation(summary = "Comprueba si un nombre de usuario está disponible (true) o ya existe (false)")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        // Corregido a findByUsername
        boolean disponible = actorRepository.findByUsername(username).isEmpty();
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }
}