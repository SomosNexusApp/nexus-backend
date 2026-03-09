package com.nexus.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.Actor;
import com.nexus.entity.Empresa;
import com.nexus.entity.Usuario;
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
            if (!usuario.getAvatar().contains("avatar-default")) {
                storageService.eliminarImagen(usuario.getAvatar());
            }

            // Guardar nueva URL en BD
            usuario.setAvatar(url);
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
                // NOTA: Como la jerarquía de herencia (Actor -> Usuario/Empresa) no permite
                // casteos directos ni "transformaciones" mágicas en Hibernate, la forma más
                // segura
                // es crear una entidad Empresa, copiar los datos base, eliminar el Usuario y
                // guardar la Empresa.

                Empresa nuevaEmpresa = new Empresa();
                nuevaEmpresa.setUser(usuario.getUser());
                nuevaEmpresa.setEmail(usuario.getEmail());
                nuevaEmpresa.setPassword(usuario.getPassword()); // Ya está hasheada
                nuevaEmpresa.setAvatar(usuario.getAvatar());
                nuevaEmpresa.setCuentaVerificada(usuario.isCuentaVerificada());
                // ... copiar otros campos comunes

                // Borramos el usuario (con cuidado de dependencias) y guardamos la empresa
                usuarioService.delete(usuario.getId());
                empresaService.save(nuevaEmpresa);

                return ResponseEntity.ok(Map.of("mensaje", "Cuenta convertida a Empresa con éxito"));
            }

            // Si es 'USUARIO', simplemente guardamos (los términos se asumen aceptados por
            // el simple hecho de llamar al endpoint)
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

    // ── CONFIGURACIÓN: Seguridad (Mocks avanzados) ──
    @PostMapping("/me/cambiar-email")
    public ResponseEntity<?> cambiarEmail(@RequestBody Map<String, String> payload) {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Usuario u = usuarioService.findById(actor.getId()).orElseThrow();
            String pwd = payload.get("password");
            String newEmail = payload.get("emailNuevo");

            if (!passwordEncoder.matches(pwd, u.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Contraseña incorrecta"));
            }

            u.setEmail(newEmail);
            usuarioService.save(u);
            return ResponseEntity.ok(Map.of("mensaje", "Email actualizado en base de datos"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> payload) {
        try {
            Actor actor = jwtUtils.userLogin();
            if (actor == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Usuario u = usuarioService.findById(actor.getId()).orElseThrow();
            String pwdActual = payload.get("passwordActual");
            String newPwd = payload.get("passwordNueva");
            String confirm = payload.get("confirmar");

            if (!passwordEncoder.matches(pwdActual, u.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Contraseña actual incorrecta"));
            }
            if (!newPwd.equals(confirm)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas nuevas no coinciden"));
            }

            u.setPassword(passwordEncoder.encode(newPwd));
            usuarioService.save(u);
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña modificada exitosamente"));
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

            if (!passwordEncoder.matches(pwd, u.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Contraseña incorrecta"));
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
            if (!usuario.getAvatar().contains("avatar-default")) {
                storageService.eliminarImagen(usuario.getAvatar());
            }

            usuarioService.delete(id);
            return ResponseEntity.ok("Usuario eliminado correctamente");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha encontrado el usuario para eliminar");
        }
    }

    @PatchMapping("/me/tipo-cuenta")
    @Operation(summary = "Configurar el tipo de cuenta (Personal o Empresa) tras el registro")
    public ResponseEntity<?> actualizarTipoCuenta(@RequestBody Map<String, String> payload) {
        try {
            Actor actorLogueado = jwtUtils.userLogin();
            if (actorLogueado == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String tipoCuenta = payload.get("tipoCuenta");

            // Si elige Empresa, delegamos TODA la lógica de negocio y base de datos al
            // Servicio
            if ("EMPRESA".equals(tipoCuenta) && actorLogueado instanceof Usuario) {
                usuarioService.convertirAEmpresa((Usuario) actorLogueado, payload);
            }

            return ResponseEntity.ok(Map.of("mensaje", "Tipo de cuenta configurado correctamente"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error al procesar el tipo de cuenta: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/perfil")
    @Operation(summary = "Ver perfil público de un usuario")
    public ResponseEntity<?> getPerfilPublico(@PathVariable Integer id) {
        Optional<Usuario> usuarioOptional = usuarioService.findById(id);

        if (usuarioOptional.isEmpty() || usuarioOptional.get().isCuentaEliminada()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();

        // Si el usuario configuró su cuenta como privada, bloqueamos el acceso
        if (usuario.isCuentaPrivada()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Este perfil es privado"));
        }

        // --- SOLUCIÓN SIN DTO ---
        // Construimos la respuesta al vuelo solo con los datos seguros
        Map<String, Object> perfilPublico = new java.util.HashMap<>();
        perfilPublico.put("id", usuario.getId());
        perfilPublico.put("username", usuario.getUser());
        perfilPublico.put("nombre", usuario.getNombre());
        perfilPublico.put("avatar", usuario.getAvatar());
        perfilPublico.put("biografia", usuario.getBiografia());
        perfilPublico.put("reputacion", usuario.getReputacion());
        perfilPublico.put("totalVentas", usuario.getTotalVentas());
        perfilPublico.put("esVerificado", usuario.isEsVerificado());
        perfilPublico.put("fechaRegistro", usuario.getFechaRegistro());

        // Lógica de privacidad condicional
        if (usuario.isMostrarUbicacion()) {
            perfilPublico.put("ubicacion", usuario.getUbicacion());
        }
        if (usuario.isMostrarTelefono()) {
            perfilPublico.put("telefono", usuario.getTelefono());
        }

        return ResponseEntity.ok(perfilPublico);
    }
}