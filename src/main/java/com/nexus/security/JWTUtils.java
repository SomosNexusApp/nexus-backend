package com.nexus.security;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nexus.entity.Actor;
import com.nexus.entity.Admin;
import com.nexus.entity.Empresa;
import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository;
import com.nexus.service.AdminService;
import com.nexus.service.EmpresaService;
import com.nexus.service.UsuarioService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JWTUtils {

    @Autowired
    private ActorRepository actorRepository; 

    @Autowired
    @Lazy
    private AdminService adminService;

    @Autowired
    @Lazy
    private EmpresaService empresaService;

    @Autowired
    @Lazy
    private UsuarioService usuarioService;

    @Value("${jwt.secret}")
    private String jwtFirma; 

    @Value("${jwt.expiration:86400000}") 
    private long extensionToken; 

    public String getToken(HttpServletRequest request) {
        String tokenBearer = request.getHeader("Authorization");
        if (StringUtils.hasText(tokenBearer) && tokenBearer.startsWith("Bearer ")) {
            return tokenBearer.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtFirma).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            throw new AuthenticationCredentialsNotFoundException("JWT ha expirado o no es válido");
        }
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date fechaActual = new Date();
        Date fechaExpiracion = new Date(fechaActual.getTime() + extensionToken);
        
        String rol = authentication.getAuthorities().iterator().next().getAuthority();
        
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(fechaActual)
                .setExpiration(fechaExpiracion)
                .claim("rol", rol)
                .signWith(SignatureAlgorithm.HS512, jwtFirma)
                .compact();
        return token;
    }

    public String getUsernameOfToken(String token) {
        return Jwts.parser().setSigningKey(jwtFirma).parseClaimsJws(token).getBody().getSubject();
    }

    @SuppressWarnings("unchecked")
    public <T> T userLogin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!StringUtils.hasText(username)) {
            return null;
        }
        
        Optional<Actor> actorO = actorRepository.findByUser(username);
        
        if (!actorO.isPresent()) {
            return null;
        }
        
        Actor actor = actorO.get();
        
        if (actor instanceof Admin) {
            return (T) adminService.findById(actor.getId()).orElse(null);
        } else if (actor instanceof Empresa) {
            return (T) empresaService.findById(actor.getId()).orElse(null);
        } else if (actor instanceof Usuario) {
            return (T) usuarioService.findById(actor.getId()).orElse(null);
        }
        
        return null;
    }
}