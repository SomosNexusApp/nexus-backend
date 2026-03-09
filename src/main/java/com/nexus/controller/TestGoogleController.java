package com.nexus.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.nexus.service.UsuarioService;
import java.util.Map;
import java.util.UUID;
import com.nexus.entity.Usuario;
import com.nexus.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/test-google")
public class TestGoogleController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public Map<String, Object> test(@RequestParam String email, @RequestParam String nombre) {
        String baseUser = email.contains("@") ? email.substring(0, email.indexOf("@")) : nombre;

        Usuario nu = new Usuario();
        nu.setEmail(email);
        // Llama al método privado por reflexión o copiamos su lógica para probar
        // ya que usernameUnico es privado. Copio la lógica aquí:
        String username = null;
        try {
            java.lang.reflect.Method m = UsuarioService.class.getDeclaredMethod("usernameUnico", String.class);
            m.setAccessible(true);
            username = (String) m.invoke(usuarioService, baseUser);
        } catch (Exception e) {
        }

        nu.setUser(username);
        nu.setNombre(nombre);
        nu.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        nu.setCuentaVerificada(true);

        Usuario guardado = usuarioRepository.save(nu);

        return Map.of(
                "email", email,
                "baseUser", baseUser,
                "username_generado", username,
                "entidad_guardada", guardado);
    }
}
