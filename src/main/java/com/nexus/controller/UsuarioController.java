package com.nexus.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.Usuario;
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
    
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario")
    public ResponseEntity<Usuario> updateUsuario(@PathVariable Integer id, @RequestBody Usuario usuarioDetalles) {
        Optional<Usuario> usuarioOptional = usuarioService.findById(id);
        
        if (usuarioOptional.isPresent()) {
            Usuario usuarioExistente = usuarioOptional.get();
            usuarioExistente.setUser(usuarioDetalles.getUser());
            usuarioExistente.setEmail(usuarioDetalles.getEmail());
            usuarioExistente.setTelefono(usuarioDetalles.getTelefono());
            usuarioExistente.setBiografia(usuarioDetalles.getBiografia());
            usuarioExistente.setUbicacion(usuarioDetalles.getUbicacion());
            
            Usuario usuarioActualizado = usuarioService.save(usuarioExistente);
            return ResponseEntity.ok(usuarioActualizado);
        } else {
            return ResponseEntity.notFound().build();
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