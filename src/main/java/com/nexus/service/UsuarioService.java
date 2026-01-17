package com.nexus.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nexus.entity.Actor;
import com.nexus.entity.Admin;
import com.nexus.entity.Empresa;
import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.UsuarioRepository;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ActorRepository actorRepository; 
    
    @Autowired
    @Lazy 
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService; 

    @Value("${google.client.id}")
    private String googleClientId;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Actor actor = actorRepository.findByUser(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        
         if (actor instanceof Usuario && !((Usuario) actor).getEsVerificado()) {
             throw new UsernameNotFoundException("Cuenta no verificada. Revisa tu correo.");
         }

        String rol = obtenerRol(actor);
        return new User(actor.getUser(), actor.getPassword(), Collections.singletonList(new SimpleGrantedAuthority(rol)));
    }

    // --- REGISTRO NORMAL (Con código) ---
    public Usuario registrarUsuario(Usuario usuario) {
        // 1. Verificar si ya existe email o user
        if (actorRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        // 2. Configurar usuario
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setEsVerificado(false); // NO verificado inicialmente
        
        // 3. Generar Código 6 dígitos
        String codigo = String.format("%06d", new Random().nextInt(999999));
        usuario.setCodigoVerificacion(codigo);
        
        // 4. Guardar
        Usuario guardado = usuarioRepository.save(usuario);
        
        // 5. Enviar Correo (Asíncrono idealmente, pero directo por ahora)
        emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo);
        
        return guardado;
    }

    // --- VERIFICAR CÓDIGO ---
    public boolean verificarCuenta(String email, String codigo) {
        Optional<Actor> oActor = actorRepository.findByEmail(email);
        
        if (oActor.isPresent() && oActor.get() instanceof Usuario) {
            Usuario usuario = (Usuario) oActor.get();
            if (codigo.equals(usuario.getCodigoVerificacion())) {
                usuario.setEsVerificado(true);
                usuario.setCodigoVerificacion(null); // Borramos el código usado
                usuarioRepository.save(usuario);
                return true;
            }
        }
        return false;
    }

    // --- LOGIN CON GOOGLE (Sin código, auto-verificado) ---
    public Actor ingresarConGoogle(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        
        if (idToken != null) {
            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String pictureUrl = (String) payload.get("picture");
            String name = (String) payload.get("name");

            Optional<Actor> oActor = actorRepository.findByEmail(email);

            if (oActor.isPresent()) {
                return oActor.get(); // Login
            } else {
                // Registro Automático Google
                Usuario nuevo = new Usuario();
                nuevo.setEmail(email);
                nuevo.setUser(email);
                nuevo.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                nuevo.setFechaRegistro(LocalDateTime.now());
                nuevo.setFotoPerfil(pictureUrl);
                nuevo.setEsVerificado(true); // GOOGLE SIEMPRE ES VERIFICADO
                nuevo.setReputacion(0);
                nuevo.setBiografia("Usuario verificado vía Google: " + name);

                return usuarioRepository.save(nuevo);
            }
        } else {
            throw new IllegalArgumentException("Token Google inválido");
        }
    }
    
    // Auxiliar
    public String obtenerRol(Actor actor) {
        if (actor instanceof Admin) return "ADMIN";
        if (actor instanceof Empresa) return "EMPRESA";
        return "USUARIO";
    }

    // CRUD Básico
    public Optional<Usuario> findById(Integer id) { return usuarioRepository.findById(id); }
    public List<Usuario> findAll() { return usuarioRepository.findAll(); }
    public Usuario save(Usuario usuario) { return registrarUsuario(usuario); } // Redirige al registro seguro
    public void delete(Integer id) { usuarioRepository.deleteById(id); }
}