package com.nexus.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository;
import com.nexus.service.CaptchaService;
import com.nexus.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private ActorRepository actorRepository;
    @Autowired private UsuarioService usuarioService;
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
        
        // 1. Validar Captcha
        if (!captchaService.verificar(req.captchaToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Captcha inválido."));
        }

        // 2. Validar Términos Aceptados (Obligatorio)
        if (!req.terminosAceptados) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Debes aceptar los términos y condiciones para registrarte."));
        }

        // 3. Crear el Usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUser(req.username);
        nuevoUsuario.setEmail(req.email);
        
        // Pasamos la contraseña SIN encriptar aquí, el servicio UsuarioService lo encriptará.
        nuevoUsuario.setPassword(req.password);
        
        // Nuevos campos base
        nuevoUsuario.setNombre(req.nombre);
        nuevoUsuario.setApellidos(req.apellidos);
        
        // Nuevos campos específicos
        nuevoUsuario.setTerminosAceptados(req.terminosAceptados);
        nuevoUsuario.setFechaAceptacionTerminos(LocalDateTime.now());
        nuevoUsuario.setVersionTerminosAceptados("1.0"); 
        nuevoUsuario.setNewsletterSuscrito(req.newsletterSuscrito);

        try {
            // 4. Guardar usando el SERVICIO (esto lanza el email automáticamente)
            Usuario usuarioGuardado = usuarioService.registrarUsuario(nuevoUsuario);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "mensaje", "Usuario registrado exitosamente. Revisa tu correo para verificar la cuenta.", 
                    "id", usuarioGuardado.getId()
                ));
                
        } catch (IllegalArgumentException e) {
            // Atrapa si el usuario o el email ya existen (lanzado desde el servicio)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
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
        boolean disponible = actorRepository.findByUsername(username).isEmpty();
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }
}