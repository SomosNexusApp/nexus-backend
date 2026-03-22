package com.nexus.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Actor;
import com.nexus.entity.SesionDispositivo;
import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.SesionDispositivoRepository;
import com.nexus.security.JWTUtils;
import com.nexus.service.CaptchaService;
import com.nexus.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private CaptchaService captchaService;

    // --- DEPENDENCIAS AÑADIDAS PARA EL LOGIN Y SESIONES ---
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JWTUtils jwtUtils;
    @Autowired
    private SesionDispositivoRepository sesionDispositivoRepository;

    // DTO Interno para registro
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

    // DTO Interno para login
    public static class LoginRequest {
        public String username;
        public String password;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión, generar JWT y registrar el dispositivo")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {

        // 1. Autenticar al usuario
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username, req.password));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Generar el Token JWT
        String jwt = jwtUtils.generateToken(authentication);

        // 3. REGISTRAR LA SESIÓN EN BASE DE DATOS (NIVEL EXPERTO)
        String userAgent = request.getHeader("User-Agent");
        String clientHints = request.getHeader("Sec-CH-UA"); // Cabecera para detectar Brave/Edge

        String dispositivo = "Desconocido";
        if (userAgent != null) {
            // Sistema Operativo
            if (userAgent.contains("Windows"))
                dispositivo = "Windows PC";
            else if (userAgent.contains("Mac OS X"))
                dispositivo = "Mac / OS X";
            else if (userAgent.contains("iPhone"))
                dispositivo = "Apple iPhone";
            else if (userAgent.contains("iPad"))
                dispositivo = "Apple iPad";
            else if (userAgent.contains("Android"))
                dispositivo = "Móvil Android";
            else if (userAgent.contains("Linux"))
                dispositivo = "PC Linux";

            // Navegador (Orden de prioridad estricto)
            if (clientHints != null && clientHints.contains("Brave")) {
                dispositivo += " (Brave)";
            } else if (userAgent.contains("Edg/") || (clientHints != null && clientHints.contains("Edge"))) {
                dispositivo += " (Edge)";
            } else if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
                dispositivo += " (Opera)";
            } else if (userAgent.contains("Firefox")) {
                dispositivo += " (Firefox)";
            } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
                dispositivo += " (Safari)";
            } else if (userAgent.contains("Chrome")) {
                dispositivo += " (Chrome)";
            }
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "127.0.0.1 (Localhost)";
        }

        // Guardar el historial de conexión real
        Actor actor = actorRepository.findByUsername(req.username).orElseThrow();
        SesionDispositivo sesion = new SesionDispositivo();
        sesion.setActorId(actor.getId());
        sesion.setIp(ip);
        sesion.setDispositivo(dispositivo);
        sesion.setFechaLogin(LocalDateTime.now());
        sesionDispositivoRepository.save(sesion);

        // 4. Devolver el Token al frontend
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        if (!captchaService.verificar(req.captchaToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Captcha inválido."));
        }

        if (!req.terminosAceptados) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Debes aceptar los términos y condiciones para registrarte."));
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUser(req.username);
        nuevoUsuario.setEmail(req.email);
        nuevoUsuario.setPassword(req.password);
        nuevoUsuario.setNombre(req.nombre);
        nuevoUsuario.setApellidos(req.apellidos);
        nuevoUsuario.setTerminosAceptados(req.terminosAceptados);
        nuevoUsuario.setFechaAceptacionTerminos(LocalDateTime.now());
        nuevoUsuario.setVersionTerminosAceptados("1.0");
        nuevoUsuario.setNewsletterSuscrito(req.newsletterSuscrito);

        try {
            Usuario usuarioGuardado = usuarioService.registrarUsuario(nuevoUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Usuario registrado exitosamente. Revisa tu correo para verificar la cuenta.",
                    "id", usuarioGuardado.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
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

    @GetMapping("/check-phone")
    @Operation(summary = "Comprueba si un teléfono de usuario está disponible (true) o ya existe (false)")
    public ResponseEntity<Map<String, Boolean>> checkPhone(@RequestParam String phone) {
        boolean disponible = !phone.equals("000000000");
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }
}