package com.nexus.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.entity.Actor;
import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.UsuarioRepository;

@Deprecated // Facebook OAuth eliminado. Mantener para compatibilidad de columnas en BD.
@Service
public class FacebookAuthService {

    @Autowired
    private ActorRepository actorRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${facebook.app.id:}")
    private String facebookAppId;
    
    @Value("${facebook.app.secret:}")
    private String facebookAppSecret;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Actor loginConFacebook(String accessToken) throws IOException {
        // 1. Validar token
        String debugTokenUrl = String.format(
            "https://graph.facebook.com/debug_token?input_token=%s&access_token=%s|%s",
            accessToken, facebookAppId, facebookAppSecret
        );
        
        ResponseEntity<String> debugResponse = restTemplate.getForEntity(debugTokenUrl, String.class);
        JsonNode debugData = objectMapper.readTree(debugResponse.getBody());
        
        if (!debugData.path("data").path("is_valid").asBoolean()) {
            throw new IllegalArgumentException("Token de Facebook inválido");
        }
        
        // 2. Obtener datos del usuario
        String userInfoUrl = String.format(
            "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token=%s",
            accessToken
        );
        
        ResponseEntity<String> userResponse = restTemplate.getForEntity(userInfoUrl, String.class);
        JsonNode userData = objectMapper.readTree(userResponse.getBody());
        
        String facebookId = userData.path("id").asText();
        String name = userData.path("name").asText();
        String email = userData.path("email").asText();
        String pictureUrl = userData.path("picture").path("data").path("url").asText();
        
        // 3. Verificar si existe
        var actorExistente = actorRepository.findByEmail(email);
        
        if (actorExistente.isPresent()) {
            Usuario usuario = (Usuario) actorExistente.get();
            if (pictureUrl != null && !pictureUrl.isEmpty()) {
                usuario.setAvatar(pictureUrl);
            }
            if (usuario.getFacebookId() == null) {
                usuario.setFacebookId(facebookId);
            }
            usuarioRepository.save(usuario);
            return usuario;
        }
        
        // 4. Crear nuevo usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setFacebookId(facebookId);
        nuevoUsuario.setUser(name.replaceAll("\\s+", "_").toLowerCase() + "_fb");
        nuevoUsuario.setEmail(email);
        nuevoUsuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        nuevoUsuario.setAvatar(pictureUrl);
        nuevoUsuario.setEsVerificado(true);
        nuevoUsuario.setTelefono("Pendiente");
        nuevoUsuario.setUbicacion("Desconocida");
        nuevoUsuario.setReputacion(0);
        
        return usuarioRepository.save(nuevoUsuario);
    }
}