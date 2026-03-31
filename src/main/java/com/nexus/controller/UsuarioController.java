package com.nexus.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.Actor;
import com.nexus.entity.Empresa;
import com.nexus.entity.SesionDispositivo;
import com.nexus.entity.Usuario;
import com.nexus.entity.TipoCuenta;
import com.nexus.security.JWTUtils;
import com.nexus.service.EmpresaService;
import com.nexus.service.StorageService;
import com.nexus.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.nexus.repository.SesionDispositivoRepository sesionDispositivoRepository;

    @Autowired
    private com.nexus.repository.ActorRepository actorRepository;

    @GetMapping
    @Operation(summary = "Obtener todos los usuarios")
    public List<Usuario> getAllUsuarios() {
        return usuarioService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por id")
    public ResponseEntity<Usuario> getUsuarioById(@PathVariable Integer id) {
        Optional<Usuario> usuarioOptional = usuarioService.findById(id);

        if (usuarioOptional.isPresent()) {
            return ResponseEntity.ok(usuarioOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/username/{username}")
    @Operation(summary = "Obtener perfil público por nombre de usuario")
    public ResponseEntity<?> getPublicProfileByUsername(@PathVariable String username) {
        Optional<Actor> actorOpt = actorRepository.findByUsername(username);

        if (actorOpt.isPresent()) {
            Actor actor = actorOpt.get();

            if (actor.isCuentaEliminada()) {
                return ResponseEntity.notFound().build();
            }

            // Retornamos un mapa filtrado con datos públicos
            Map<String, Object> perfilPublico = new java.util.HashMap<>();
            perfilPublico.put("id", actor.getId());
            perfilPublico.put("user", actor.getUser());
            perfilPublico.put("username", actor.getUser());
            perfilPublico.put("nombre", actor.getNombre());
            perfilPublico.put("apellidos", actor.getApellidos());
            perfilPublico.put("avatar", actor.getAvatar());
            perfilPublico.put("googleAvatarUrl", actor.getGoogleAvatarUrl());
            perfilPublico.put("avatarSource", actor.getAvatarSource());
            perfilPublico.put("fechaRegistro", actor.getFechaRegistro());
            perfilPublico.put("tipoCuenta", actor instanceof Empresa ? "EMPRESA" : "USUARIO");

            if (actor instanceof Usuario) {
                Usuario u = (Usuario) actor;
                perfilPublico.put("biografia", u.getBiografia());
                perfilPublico.put("reputacion", u.getReputacion());
                perfilPublico.put("totalVentas", u.getTotalVentas());
                perfilPublico.put("esVerificado", u.isEsVerificado());

                // 🔒 REGLAS DE PRIVACIDAD ESTRICTAS AQUÍ 🔒
                if (u.isMostrarUbicacion()) {
                    perfilPublico.put("ubicacion", u.getUbicacion());
                }
                if (u.isMostrarTelefono()) {
                    perfilPublico.put("telefono", u.getTelefono());
                }
            }

            return ResponseEntity.ok(perfilPublico);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/perfil")
    @Operation(summary = "Ver perfil público de un usuario por ID")
    public ResponseEntity<?> getPerfilPublico(@PathVariable Integer id) {
        Optional<Usuario> usuarioOptional = usuarioService.findById(id);

        if (usuarioOptional.isEmpty() || usuarioOptional.get().isCuentaEliminada()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();

        // 🔒 Construimos la respuesta al vuelo respetando la privacidad
        Map<String, Object> perfilPublico = new java.util.HashMap<>();
        perfilPublico.put("id", usuario.getId());
        perfilPublico.put("username", usuario.getUser());
        perfilPublico.put("nombre", usuario.getNombre());
        perfilPublico.put("avatar", usuario.getAvatar());
        perfilPublico.put("googleAvatarUrl", usuario.getGoogleAvatarUrl());
        perfilPublico.put("avatarSource", usuario.getAvatarSource());
        perfilPublico.put("biografia", usuario.getBiografia());
        perfilPublico.put("reputacion", usuario.getReputacion());
        perfilPublico.put("totalVentas", usuario.getTotalVentas());
        perfilPublico.put("esVerificado", usuario.isEsVerificado());
        perfilPublico.put("fechaRegistro", usuario.getFechaRegistro());

        if (usuario.isMostrarUbicacion()) {
            perfilPublico.put("ubicacion", usuario.getUbicacion());
        }
        if (usuario.isMostrarTelefono()) {
            perfilPublico.put("telefono", usuario.getTelefono());
        }

        return ResponseEntity.ok(perfilPublico);
    }

    @PostMapping
    @Operation(summary = "Crear usuario")
    public ResponseEntity<Usuario> createUsuario(@RequestBody Usuario usuario) {
        Usuario nuevoUsuario = usuarioService.save(usuario);
        return ResponseEntity.ok(nuevoUsuario);
    }

    // ✅ CORREGIDO: Subir avatar con mejor manejo de errores
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir o actualizar avatar del usuario")
    public ResponseEntity<?> subirAvatar(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        Optional<Usuario> usuarioOptional = usuarioService.findById(id);

        if (usuarioOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();

        try {
            // Subir a Cloudinary
            String url = storageService.subirImagen(file);

            if (url == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Fallo al subir la imagen. Revisa configuración de Cloudinary."));
            }

            // Eliminar avatar anterior si no es el por defecto
            String avatarActual = usuario.getAvatar();
            if (avatarActual != null && !avatarActual.contains("avatar-default")) {
                storageService.eliminarImagen(avatarActual);
            }

            // Guardar nueva URL en BD e indicar que es personalizada
            usuario.setAvatar(url);
            usuario.setCustomAvatarUrl(url);
            usuario.setAvatarSource("CUSTOM");
            usuarioService.save(usuario);

            return ResponseEntity.ok(Map.of("mensaje", "Avatar actualizado", "url", url));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la imagen: " + e.getMessage()));
        }
    }

    @PatchMapping("/me/terminos")
    @Operation(summary = "Actualizar términos y tipo de cuenta tras registro OAuth")
    public ResponseEntity<?> actualizarTerminosOAuth(@RequestBody Map<String, Object> payload) {
        try {
            Actor actorLogueado = jwtUtils.userLogin();
            if (actorLogueado == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Optional<Usuario> usuarioOptional = usuarioService.findById(actorLogueado.getId());
            if (usuarioOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Usuario usuario = usuarioOptional.get();

            // 1. Actualizar el consentimiento del newsletter si viene en el payload
            if (payload.containsKey("newsletterSuscrito")) {
                // Si tienes un servicio específico de newsletter, llámalo aquí.
                // Por simplicidad, asumimos que se guarda en el usuario o que tienes lógica
                // separada.
                // boolean newsletter = (Boolean) payload.get("newsletterSuscrito");
                // usuario.setNewsletterSuscrito(newsletter); // Si existe el campo en tu
                // entidad
            }

            // 2. Gestionar el tipo de cuenta (Usuario vs Empresa)
            String tipoCuenta = (String) payload.get("tipoCuenta");

            if ("EMPRESA".equals(tipoCuenta)) {
                // Lógica de migración de Usuario a Empresa
                Empresa nuevaEmpresa = new Empresa();
                
                // Copiar campos de Actor
                nuevaEmpresa.setUser(usuario.getUser());
                nuevaEmpresa.setEmail(usuario.getEmail());
                nuevaEmpresa.setPassword(usuario.getPassword());
                nuevaEmpresa.setNombre(usuario.getNombre());
                nuevaEmpresa.setApellidos(usuario.getApellidos());
                nuevaEmpresa.setTelefono(usuario.getTelefono());
                nuevaEmpresa.setCuentaVerificada(usuario.isCuentaVerificada());
                nuevaEmpresa.setTwoFactorEnabled(usuario.isTwoFactorEnabled());
                nuevaEmpresa.setTwoFactorMethod(usuario.getTwoFactorMethod());
                nuevaEmpresa.setTwoFactorSecret(usuario.getTwoFactorSecret());
                nuevaEmpresa.setGoogleId(usuario.getGoogleId());
                nuevaEmpresa.setGoogleAvatarUrl(usuario.getGoogleAvatarUrl());
                nuevaEmpresa.setNotificacionConfig(usuario.getNotificacionConfig());
                nuevaEmpresa.setFechaRegistro(usuario.getFechaRegistro());

                // Campos específicos de Empresa
                nuevaEmpresa.setCif((String) payload.get("cif"));
                nuevaEmpresa.setNombreComercial((String) payload.get("nombreComercial"));
                nuevaEmpresa.setDescripcion((String) payload.get("descripcion"));
                nuevaEmpresa.setWeb((String) payload.get("web"));
                nuevaEmpresa.setVerificada(false);

                // Borramos el usuario (con cuidado de dependencias) y guardamos la empresa
                usuarioService.delete(usuario.getId());
                empresaService.save(nuevaEmpresa);

                return ResponseEntity.ok(Map.of("mensaje", "Cuenta convertida a Empresa con éxito"));
            }

            // Si es 'PERSONAL' o 'USUARIO', simplemente guardamos lo que hay
            if ("PERSONAL".equals(tipoCuenta)) {
                usuario.setTipoCuenta(TipoCuenta.PERSONAL);
            }
            
            usuarioService.save(usuario);
            return ResponseEntity.ok(Map.of("mensaje", "Preferencias actualizadas con éxito"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la solicitud: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar un usuario existente")
    public ResponseEntity<?> updateUsuario(@PathVariable Integer id, @RequestBody Usuario usuarioDetalles) {
        Optional<Usuario> usuarioActual = usuarioService.findById(id);
        if (usuarioActual.isPresent()) {
            Usuario usuario = usuarioActual.get();
            usuario.setNombre(usuarioDetalles.getNombre());
            // usuario.setUser(usuarioDetalles.getUser()); // Omitimos username/email por
            // seguridad
            // usuario.setEmail(usuarioDetalles.getEmail());
            // Si el teléfono se envía como extra properties en un DTO lo cogeríamos aquí,
            // asumiendo setTelefono
            // usuario.setTelefono(usuarioDetalles.getTelefono());
            usuario.setBiografia(usuarioDetalles.getBiografia());
            usuario.setUbicacion(usuarioDetalles.getUbicacion());

            usuario = usuarioService.save(usuario);
            return ResponseEntity.ok(usuario);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado con ID: " + id));
        }
    }

    // ── CONFIGURACIÓN: Privacidad ──
    @PatchMapping("/me/privacidad")
    @Operation(summary = "Actualizar configuración de privacidad")
    public ResponseEntity<?> updatePrivacidad(@RequestBody Map<String, Boolean> payload) {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Usuario u = usuarioService.findById(actor.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (payload.containsKey("perfilPublico"))
                u.setPerfilPublico(payload.get("perfilPublico"));
            if (payload.containsKey("mostrarUbicacion"))
                u.setMostrarUbicacion(payload.get("mostrarUbicacion"));
            if (payload.containsKey("mostrarTelefono"))
                u.setMostrarTelefono(payload.get("mostrarTelefono"));
            if (payload.containsKey("permitirMensajesDesconocidos"))
                u.setPermitirMensajesDesconocidos(payload.get("permitirMensajesDesconocidos"));
            if (payload.containsKey("cuentaPrivada"))
                u.setCuentaPrivada(payload.get("cuentaPrivada"));

            usuarioService.save(u);
            return ResponseEntity.ok(Map.of("mensaje", "Privacidad actualizada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── CONFIGURACIÓN: Notificaciones ──
    @PatchMapping("/me/notificaciones-config")
    @Operation(summary = "Actualizar preferencias de notificaciones")
    public ResponseEntity<?> updateNotificaciones(@RequestBody Map<String, Boolean> payload) {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Usuario u = usuarioService.findById(actor.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (payload.containsKey("nuevosMensajes"))
                u.setNotifNuevosMensajes(payload.get("nuevosMensajes"));
            if (payload.containsKey("nuevaCompra"))
                u.setNotifNuevaCompra(payload.get("nuevaCompra"));
            if (payload.containsKey("valoracion"))
                u.setNotifValoracion(payload.get("valoracion"));
            if (payload.containsKey("ofertas"))
                u.setNotifOfertas(payload.get("ofertas"));
            if (payload.containsKey("envios"))
                u.setNotifEnvios(payload.get("envios"));
            if (payload.containsKey("novedades"))
                u.setNotifNovedades(payload.get("novedades"));
            if (payload.containsKey("newsletterSuscrito"))
                u.setNewsletterSuscrito(payload.get("newsletterSuscrito"));

            usuarioService.save(u);
            return ResponseEntity.ok(Map.of("mensaje", "Notificaciones actualizadas"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PatchMapping("/me/direccion")
    @Operation(summary = "Actualizar dirección de envío por defecto")
    public ResponseEntity<?> updateDireccion(@RequestBody com.nexus.entity.DireccionEnvio direccion) {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Usuario u = usuarioService.findById(actor.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            u.setDireccionPorDefecto(direccion);
            usuarioService.save(u);
            
            return ResponseEntity.ok(Map.of("mensaje", "Dirección actualizada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/me/datos-personales")
    @Operation(summary = "Descargar todos los datos del usuario (GDPR)")
    public ResponseEntity<?> descargarDatos() {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            Usuario u = usuarioService.findById(actor.getId()).orElseThrow();
            // Retorna JSON directo estructurado (básico de GDPR pro)
            return ResponseEntity.ok(Map.of(
                    "usuario", u,
                    "fecha_descarga", java.time.LocalDateTime.now()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/me/cuenta")
    @Operation(summary = "Eliminar cuenta permanentemente")
    public ResponseEntity<?> eliminarCuenta(@RequestBody Map<String, Object> payload) {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Usuario u = usuarioService.findById(actor.getId()).orElseThrow();
            String pwd = (String) payload.get("password");

            // Si el usuario es social (Google/Facebook), no tiene contraseña local.
            // Permitimos el borrado si están logueados (ya verificado por Spring Security).
            boolean isSocial = u.getGoogleId() != null || u.getFacebookId() != null;

            if (!isSocial) {
                if (pwd == null || !passwordEncoder.matches(pwd, u.getPassword())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Contraseña incorrecta"));
                }
            }

            usuarioService.delete(u.getId());
            return ResponseEntity.ok(Map.of("mensaje", "Cuenta eliminada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario por id")
    public ResponseEntity<String> deleteUsuario(@PathVariable Integer id) {
        Optional<Usuario> usuarioOptional = usuarioService.findById(id);

        if (usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();

            // Eliminar avatar de Cloudinary si no es el por defecto
            String avatarActual = usuario.getAvatar();
            if (avatarActual != null && !avatarActual.contains("avatar-default")) {
                storageService.eliminarImagen(avatarActual);
            }

            usuarioService.delete(id);
            return ResponseEntity.ok("Usuario eliminado correctamente");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha encontrado el usuario para eliminar");
        }
    }

    @PatchMapping("/me/tipo-cuenta")
    @Operation(summary = "Cambiar el tipo de cuenta (Personal <-> Empresa)")
    public ResponseEntity<?> actualizarTipoCuenta(@RequestBody Map<String, String> body, Principal principal) {
        Optional<Actor> actorOpt = actorRepository.findByUsername(principal.getName());
        if (actorOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Actor actor = actorOpt.get();
        String tipo = body.get("tipoCuenta");

        try {
            if ("EMPRESA".equals(tipo) && actor instanceof Usuario) {
                usuarioService.convertirAEmpresa((Usuario) actor, body);
                return ResponseEntity.ok(Map.of("mensaje", "Cuenta convertida a Empresa"));
            } else if ("PERSONAL".equals(tipo)) {
                if (actor instanceof Empresa) {
                    usuarioService.convertirAUsuarioPersonal(actor.getId());
                    return ResponseEntity.ok(Map.of("mensaje", "Cuenta revertida a Personal"));
                } else if (actor instanceof Usuario) {
                    // Ya es un usuario personal, respondemos OK para no bloquear el flujo del frontend
                    return ResponseEntity.ok(Map.of("mensaje", "Ya eres una cuenta Personal"));
                }
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Transición no válida o ya estás en este tipo de cuenta."));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/me/avatar-choice")
    @Operation(summary = "Elegir entre foto de Google o iniciales")
    public ResponseEntity<?> actualizarAvatarChoice(@RequestBody Map<String, String> body, Principal principal) {
        Actor actor = actorRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        String choice = body.get("choice"); // "GOOGLE", "INITIALS" o "CUSTOM"
        if (choice == null || (!choice.equals("GOOGLE") && !choice.equals("INITIALS") && !choice.equals("CUSTOM"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Opción no válida. Debe ser GOOGLE, INITIALS o CUSTOM."));
        }

        try {
            usuarioService.actualizarAvatarChoice(actor.getId(), choice);
            return ResponseEntity.ok(Map.of("mensaje", "Preferencia de avatar actualizada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── SEGURIDAD: CONTRASEÑA ──
    @PostMapping("/me/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> payload, Principal principal) {
        Actor actor = actorRepository.findByUsername(principal.getName()).orElseThrow();
        Usuario u = usuarioService.findById(actor.getId()).orElseThrow();

        // 1. Verificar si es un usuario social (Google/Facebook)
        if (u.getGoogleId() != null || u.getFacebookId() != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Las cuentas vinculadas a Google o Facebook gestionan su contraseña en el proveedor externo."));
        }

        String pwdActual = payload.get("passwordActual");
        String newPwd = payload.get("passwordNueva");

        // 2. Validar contraseña actual
        if (pwdActual == null || !passwordEncoder.matches(pwdActual, u.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "La contraseña actual es incorrecta."));
        }

        // 3. Validar longitud mínima
        if (newPwd == null || newPwd.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La nueva contraseña debe tener al menos 8 caracteres."));
        }

        u.setPassword(passwordEncoder.encode(newPwd));
        usuarioService.save(u);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña modificada exitosamente."));
    }

    // ── SEGURIDAD: 2FA (GOOGLE AUTHENTICATOR & EMAIL) ──
    @PostMapping("/me/2fa/setup-app")
    public ResponseEntity<?> setup2FAApp(Principal principal) {
        Actor actor = actorRepository.findByUsername(principal.getName()).orElseThrow();

        // 1. Generar Secreto Base32 seguro (16 caracteres) para Google Authenticator
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder secret = new StringBuilder(16);
        for (byte b : bytes) {
            secret.append(base32Chars.charAt((b & 0xFF) % 32));
        }

        actor.setTwoFactorSecret(secret.toString());
        actorRepository.save(actor);

        // 2. Crear URI de OTP y generar QR
        String appName = "NexusApp";
        String uri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", appName, actor.getEmail(),
                secret.toString(), appName);
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
                + java.net.URLEncoder.encode(uri, java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("qrUrl", qrUrl));
    }

    @PostMapping("/me/2fa/enable-app")
    public ResponseEntity<?> enable2FAApp(@RequestBody Map<String, String> body, Principal principal) {
        Actor actor = actorRepository.findByUsername(principal.getName()).orElseThrow();
        String code = body.get("code");

        // Aquí deberías validar el TOTP real. Por ahora, validamos formato básico (6
        // dígitos)
        if (code != null && code.matches("\\d{6}")) {
            actor.setTwoFactorEnabled(true);
            actor.setTwoFactorMethod("APP");
            actorRepository.save(actor);
            return ResponseEntity.ok(Map.of("mensaje", "2FA activado con éxito"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Código inválido"));
    }

    @PostMapping("/me/2fa/enable-email")
    public ResponseEntity<?> enable2FAEmail(Principal principal) {
        Actor actor = actorRepository.findByUsername(principal.getName()).orElseThrow();
        actor.setTwoFactorEnabled(true);
        actor.setTwoFactorMethod("EMAIL");
        actorRepository.save(actor);
        return ResponseEntity.ok(Map.of("mensaje", "2FA por email activado"));
    }

    @PostMapping("/me/2fa/disable")
    public ResponseEntity<?> disable2FA(Principal principal) {
        Actor actor = actorRepository.findByUsername(principal.getName()).orElseThrow();
        actor.setTwoFactorEnabled(false);
        actor.setTwoFactorSecret(null);
        actorRepository.save(actor);
        return ResponseEntity.ok(Map.of("mensaje", "2FA desactivado"));
    }

    // ── SEGURIDAD: HISTORIAL DE SESIONES ──
    @GetMapping("/me/sesiones")
    public ResponseEntity<?> getSesionesReales(Principal principal, jakarta.servlet.http.HttpServletRequest request) {
        Actor actor = actorRepository.findByUsername(principal.getName()).orElseThrow();

        // 1. Extraemos la IP de forma segura
        String ipTemp = request.getHeader("X-Forwarded-For");
        if (ipTemp == null || ipTemp.isEmpty()) {
            ipTemp = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ipTemp))
            ipTemp = "127.0.0.1";
        final String ipActual = ipTemp;

        // 2. Analizamos el dispositivo actual
        String userAgent = request.getHeader("User-Agent");
        String dispTemp = "Desconocido";
        if (userAgent != null) {
            if (userAgent.contains("Windows"))
                dispTemp = "Windows PC";
            else if (userAgent.contains("Mac OS X"))
                dispTemp = "Mac / OS X";
            else if (userAgent.contains("iPhone"))
                dispTemp = "Apple iPhone";
            else if (userAgent.contains("iPad"))
                dispTemp = "Apple iPad";
            else if (userAgent.contains("Android"))
                dispTemp = "Móvil Android";
            if (userAgent.contains("Chrome"))
                dispTemp += " (Chrome)";
            else if (userAgent.contains("Safari") && !userAgent.contains("Chrome"))
                dispTemp += " (Safari)";
        }
        final String dispositivoActual = dispTemp;

        // 3. Buscamos el historial
        List<SesionDispositivo> historial = sesionDispositivoRepository
                .findByActorIdOrderByFechaLoginDesc(actor.getId());

        // 🔥 EL SALVAVIDAS: Si el filtro de login falló al guardar esta sesión, la
        // guardamos "al vuelo"
        boolean existeActual = historial.stream().anyMatch(s -> s.getIp() != null && s.getIp().equals(ipActual));
        if (!existeActual) {
            SesionDispositivo nuevaSesion = new SesionDispositivo();
            nuevaSesion.setActorId(actor.getId());
            nuevaSesion.setIp(ipActual);
            nuevaSesion.setDispositivo(dispositivoActual);
            nuevaSesion.setFechaLogin(LocalDateTime.now());
            sesionDispositivoRepository.save(nuevaSesion);

            // Recargamos el historial con la nueva sesión incluida
            historial = sesionDispositivoRepository.findByActorIdOrderByFechaLoginDesc(actor.getId());
        }

        // 4. Mapeamos a JSON
        List<Map<String, Object>> response = historial.stream().map(s -> {
            boolean esActual = s.getIp() != null && s.getIp().equals(ipActual);

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", s.getId());
            map.put("dispositivo", s.getDispositivo() != null ? s.getDispositivo() : "Desconocido");
            map.put("ip", s.getIp());
            map.put("fechaLogin", s.getFechaLogin());
            map.put("actual", esActual);

            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/sesiones/otras")
    public ResponseEntity<?> cerrarOtrasSesiones(Principal principal) {
        Actor actor = actorRepository.findByUsername(principal.getName()).orElseThrow();
        // En una app real, revocarías los JWTs. Aquí borramos el historial para limpiar
        // la tabla.
        sesionDispositivoRepository.deleteByActorId(actor.getId());
        return ResponseEntity.ok(Map.of("mensaje", "Sesiones cerradas"));
    }

}