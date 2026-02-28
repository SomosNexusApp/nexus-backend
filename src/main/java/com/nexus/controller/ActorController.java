package com.nexus.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.*;
import com.nexus.repository.ActorRepository;
import com.nexus.security.JWTUtils;
import com.nexus.service.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación")
public class ActorController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JWTUtils              jwtUtils;
    @Autowired private UsuarioService        usuarioService;
    @Autowired private FacebookAuthService   facebookAuthService;
    @Autowired private ActorRepository       actorRepository;
    @Autowired private PasswordResetService  passwordResetService;
    @Autowired private TwoFactorService      twoFactorService;
    @Autowired private CaptchaService        captchaService;
    @Autowired private ActorService          actorService;

    // ── LOGIN ─────────────────────────────────────────────────────────────

    /**
     * Body: { "user": "username", "password": "...", "captchaToken": "..." }
     *
     * Respuesta normal:  { "token": "JWT...", "rol": "USUARIO", "userId": 5 }
     * Con 2FA activo:    { "requiere2FA": true, "metodo2FA": "TOTP", "usuarioId": 5 }
     */
    @PostMapping("/login")
    @Operation(summary = "Login con reCAPTCHA. Si hay 2FA activo, devuelve requiere2FA=true.")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> creds) {
        try {
            captchaService.verificarOLanzar(creds.get("captchaToken"));

            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(creds.get("user"), creds.get("password")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            Actor actor = actorRepository.findByUsername(auth.getName()).orElse(null);

            if (actor != null && actor.isCuentaEliminada())
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Esta cuenta ha sido eliminada"));

            if (actor != null && actor.isTwoFactorEnabled()) {
                if ("EMAIL".equals(actor.getTwoFactorMethod()))
                    twoFactorService.enviarOtpEmail(actor.getId(), actor.getEmail(), actor.getUser());
                return ResponseEntity.ok(Map.of(
                    "requiere2FA", true,
                    "metodo2FA",   actor.getTwoFactorMethod(),
                    "usuarioId",   actor.getId()
                ));
            }

            return ResponseEntity.ok(buildLoginResponse(jwtUtils.generateToken(auth), auth, actor));

        } catch (IllegalArgumentException e) {
            // Error de captcha
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario o contraseña incorrectos"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Error de autenticación"));
        }
    }

    // ── 2FA ───────────────────────────────────────────────────────────────

    @PostMapping("/2fa/verificar")
    @Operation(summary = "Verificar código 2FA y obtener JWT")
    public ResponseEntity<Map<String, Object>> verificar2FA(@RequestBody Map<String, Object> body) {
        try {
            Integer usuarioId = Integer.valueOf(body.get("usuarioId").toString());
            String  codigo    = (String) body.get("codigo");

            Actor actor = actorRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            boolean valido = "TOTP".equals(actor.getTwoFactorMethod())
                ? twoFactorService.verificarLoginTotp(usuarioId, codigo)
                : twoFactorService.verificarOtpEmail(usuarioId, codigo);

            if (!valido)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Código incorrecto o expirado"));

            UserDetails ud = usuarioService.loadUserByUsername(actor.getUser());
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return ResponseEntity.ok(buildLoginResponse(jwtUtils.generateToken(auth), auth, actor));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── OLVIDÉ CONTRASEÑA ─────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        captchaService.verificarOLanzar(body.get("captchaToken"));
        passwordResetService.solicitarReset(body.get("email"));
        return ResponseEntity.ok(Map.of("mensaje",
            "Si ese email está registrado, recibirás un enlace en breve."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        try {
            passwordResetService.resetearPassword(body.get("token"), body.get("nuevaPassword"));
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada. Ya puedes iniciar sesión."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── REGISTRO ──────────────────────────────────────────────────────────

    @PostMapping("/registro")
    @Operation(summary = "Registro de nuevo usuario con reCAPTCHA")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        try {
            captchaService.verificarOLanzar((String) body.get("captchaToken"));

            Usuario u = new Usuario();
            u.setUser((String) body.get("user"));
            u.setEmail((String) body.get("email"));
            u.setPassword((String) body.get("password"));

            Usuario nuevo = usuarioService.registrarUsuario(u);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Cuenta creada. Revisa tu correo para verificarla.",
                "userId",  nuevo.getId(),
                "email",   nuevo.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error en el registro"));
        }
    }

    @PostMapping("/verificar")
    public ResponseEntity<?> verificar(@RequestBody Map<String, String> body) {
        boolean ok = usuarioService.verificarCuenta(body.get("email"), body.get("codigo"));
        return ok
            ? ResponseEntity.ok(Map.of("mensaje", "Cuenta verificada correctamente"))
            : ResponseEntity.badRequest().body(Map.of("error", "Código incorrecto o expirado"));
    }

    // ── OAUTH ─────────────────────────────────────────────────────────────

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> loginGoogle(@RequestBody Map<String, String> body) {
        try {
            Actor actor = usuarioService.ingresarConGoogle(body.get("token"));
            return buildOAuthResponse(actor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/facebook")
    public ResponseEntity<Map<String, Object>> loginFacebook(@RequestBody Map<String, String> body) {
        try {
            Actor actor = facebookAuthService.loginConFacebook(body.get("token"));
            return buildOAuthResponse(actor);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // ── ME ────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado (requiere JWT en header)")
    public ResponseEntity<?> me() {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null || actor.isCuentaEliminada())
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Map<String, Object> perfil = new HashMap<>();
            perfil.put("id",              actor.getId());
            perfil.put("username",        actor.getUser());
            perfil.put("email",           actor.getEmail());
            perfil.put("rol",             usuarioService.obtenerRol(actor));
            perfil.put("fechaRegistro",   actor.getFechaRegistro());
            perfil.put("twoFactorActivo", actor.isTwoFactorEnabled());
            perfil.put("metodo2FA",       actor.getTwoFactorMethod());
            perfil.put("cuentaVerificada",actor.isCuentaVerificada());
            perfil.put("notificaciones",  actor.getNotificacionConfig());

            if (actor instanceof Usuario u) {
                perfil.put("avatar",           u.getAvatar());
                perfil.put("telefono",         u.isMostrarTelefono() ? u.getTelefono() : null);
                perfil.put("biografia",        u.getBiografia());
                perfil.put("ubicacion",        u.isMostrarUbicacion() ? u.getUbicacion() : null);
                perfil.put("reputacion",       u.getReputacion());
                perfil.put("esVerificado",     u.isEsVerificado());
                perfil.put("perfilPublico",    u.isPerfilPublico());
                perfil.put("direccionDefecto", u.getDireccionPorDefecto());
            } else if (actor instanceof Empresa e) {
                perfil.put("cif",         e.getCif());
                perfil.put("descripcion", e.getDescripcion());
                perfil.put("web",         e.getWeb());
            } else if (actor instanceof Admin a) {
                perfil.put("nivelAcceso", a.getNivelAcceso());
            }

            return ResponseEntity.ok(perfil);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Map<String, Object> buildLoginResponse(String token, Authentication auth, Actor actor) {
        Map<String, Object> r = new HashMap<>();
        r.put("token",       token);
        r.put("rol",         auth.getAuthorities().iterator().next().getAuthority());
        r.put("username",    auth.getName());
        r.put("userId",      actor != null ? actor.getId() : null);
        r.put("requiere2FA", false);
        return r;
    }

    private ResponseEntity<Map<String, Object>> buildOAuthResponse(Actor actor) {
        UserDetails ud = usuarioService.loadUserByUsername(actor.getUser());
        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return ResponseEntity.ok(buildLoginResponse(jwtUtils.generateToken(auth), auth, actor));
    }
}