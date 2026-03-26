package com.nexus.security;

import java.security.Key;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@Component
public class JWTUtils {

    @Autowired
    @Lazy
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

    // Genera una Key segura compatible con la nueva versión de JJWT
    private Key getSigningKey() {
        byte[] keyBytes = jwtFirma.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getToken(HttpServletRequest request) {
        String tokenBearer = request.getHeader("Authorization");
        if (StringUtils.hasText(tokenBearer) && tokenBearer.startsWith("Bearer ")) {
            return tokenBearer.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + extensionToken);
        String rol = authentication.getAuthorities().iterator().next().getAuthority();

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("rol", rol)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenForUser(Actor actor) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + extensionToken);
        String rol = "ROLE_USER";
        if (actor instanceof Admin) rol = "ROLE_ADMIN";
        else if (actor instanceof Empresa) rol = "ROLE_EMPRESA";

        return Jwts.builder()
                .setSubject(actor.getUser())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("rol", rol)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameOfToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public <T> T userLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            return null;

        String username = authentication.getName();
        if (!StringUtils.hasText(username) || "anonymousUser".equals(username))
            return null;

        Optional<Actor> actorO = actorRepository.findByUsername(username)
                .or(() -> actorRepository.findByEmail(username));
        if (actorO.isEmpty())
            return null;

        Actor actor = actorO.get();

        if (actor instanceof Admin)
            return (T) adminService.findById(actor.getId()).orElse(null);
        if (actor instanceof Empresa)
            return (T) empresaService.findById(actor.getId()).orElse(null);
        if (actor instanceof Usuario)
            return (T) usuarioService.findById(actor.getId()).orElse(null);

        return null;
    }
}