package com.nexus.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.entity.Actor;
import com.nexus.entity.ActorLogin;
import com.nexus.entity.Usuario;
import com.nexus.security.JWTUtils;
import com.nexus.service.UsuarioService;

@RestController
@RequestMapping("/auth")
public class ActorController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTUtils jwtUtils;
    
    @Autowired
    private UsuarioService usuarioService;

    // --- 1. LOGIN NORMAL ---
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody ActorLogin actorLogin) {
        try {
            UsernamePasswordAuthenticationToken authInputToken = new UsernamePasswordAuthenticationToken(actorLogin.getUser(), actorLogin.getPassword());
            Authentication authentication = authenticationManager.authenticate(authInputToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            String token = jwtUtils.generateToken(authentication);
            String rol = authentication.getAuthorities().iterator().next().getAuthority();
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("rol", rol);
            response.put("username", authentication.getName());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("mensaje", "Credenciales inválidas"), HttpStatus.UNAUTHORIZED);
        }
    }
    
    // --- 2. LOGIN GOOGLE ---
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> loginGoogle(@RequestBody Map<String, String> body) {
        try {
            String idToken = body.get("token");
            Actor actor = usuarioService.ingresarConGoogle(idToken);
            
            // Generamos nuestro propio JWT para que siga funcionando igual
            // Necesitamos 'engañar' un poco a Spring Security creando una autenticación manual
            // para poder usar jwtUtils.generateToken, o crear un método overload en JWTUtils.
            
            UserDetails userDetails = usuarioService.loadUserByUsername(actor.getUser());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            String token = jwtUtils.generateToken(auth);
            String rol = usuarioService.obtenerRol(actor);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("rol", rol);
            response.put("username", actor.getUser());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("mensaje", "Error validando Google: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
    
    // --- 3. REGISTRO USUARIO (Envía correo) ---
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody Usuario usuario) {
        try {
            Usuario nuevo = usuarioService.registrarUsuario(usuario);
            return new ResponseEntity<>(Map.of("mensaje", "Usuario registrado. Revisa tu correo para el código.", "email", nuevo.getEmail()), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("mensaje", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
    
    // --- 4. VERIFICAR CÓDIGO ---
    @PostMapping("/verificar")
    public ResponseEntity<?> verificar(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String codigo = payload.get("codigo");
        
        boolean verificado = usuarioService.verificarCuenta(email, codigo);
        
        if (verificado) {
            return new ResponseEntity<>(Map.of("mensaje", "Cuenta verificada correctamente"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Map.of("mensaje", "Código incorrecto o email no encontrado"), HttpStatus.BAD_REQUEST);
        }
    }
}