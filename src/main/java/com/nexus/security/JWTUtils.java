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

// clase de utilidades para todo lo relacionado con JWT (Json Web Tokens)
// los tokens son la forma en que sabemos quien es el usuario en cada peticion
@Component
public class JWTUtils {

    // usamos @Lazy en todas estas dependencias para evitar dependencias circulares
    // en el arranque de Spring (los servicios necesitan JWTUtils y JWTUtils los necesita a ellos)
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

    // la firma secreta viene del application.properties, no esta hardcodeada en el codigo
    @Value("${jwt.secret}")
    private String jwtFirma;

    // si no se configura, el token dura 24 horas (86400000 ms)
    @Value("${jwt.expiration:86400000}")
    private long extensionToken;

    // construye la clave de firma a partir del secreto configurado
    // necesita minimo 32 bytes para HMAC-SHA256, si es mas corta la rellenamos con ceros
    private Key getSigningKey() {
        byte[] keyBytes = jwtFirma.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = java.util.Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // extrae el token del header Authorization
    // el formato esperado es: "Bearer <token>"
    // si el header no existe o no empieza por Bearer, devuelve null
    public String getToken(HttpServletRequest request) {
        String tokenBearer = request.getHeader("Authorization");
        if (StringUtils.hasText(tokenBearer) && tokenBearer.startsWith("Bearer ")) {
            return tokenBearer.substring(7); // quitamos "Bearer " y nos quedamos solo con el token
        }
        return null;
    }

    // valida que el token tenga firma correcta y no haya expirado
    // devuelve false en lugar de lanzar excepcion para que el filtro pueda continuar tranquilo
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // cualquier problema (firma invalida, expirado, malformado...) devuelve false
            return false;
        }
    }

    // genera un token para un usuario que acaba de hacer login
    // incluye: nombre de usuario, fecha de emision, fecha de expiracion y el rol
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + extensionToken);
        String rol = authentication.getAuthorities().iterator().next().getAuthority();

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("rol", rol) // guardamos el rol en el token para no consultarlo en cada peticion
                .signWith(getSigningKey())
                .compact();
    }

    // version alternativa de generateToken que recibe directamente un Actor
    // se usa en flujos donde no tenemos el objeto Authentication de Spring (ej: OAuth, verify-2fa)
    public String generateTokenForUser(Actor actor) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + extensionToken);
        // determinamos el rol segun el tipo de actor
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

    // saca el username del token (el campo "subject" del JWT)
    public String getUsernameOfToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // metodo muy util usado en todos los controladores para obtener el actor que esta logueado
    // devuelve el tipo correcto (Admin, Empresa o Usuario) gracias al generic <T>
    // si no hay sesion activa o el usuario es anonimo devuelve null
    @SuppressWarnings("unchecked")
    public <T> T userLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            return null;

        String username = authentication.getName();
        // "anonymousUser" es lo que Spring pone cuando no hay nadie logueado
        if (!StringUtils.hasText(username) || "anonymousUser".equals(username))
            return null;

        // buscamos el actor por username o email en la bbdd
        Optional<Actor> actorO = actorRepository.findByUsername(username)
                .or(() -> actorRepository.findByEmail(username));
        if (actorO.isEmpty())
            return null;

        Actor actor = actorO.get();

        // cargamos el objeto completo segun el tipo de actor para tener todos sus campos
        if (actor instanceof Admin)
            return (T) adminService.findById(actor.getId()).orElse(null);
        if (actor instanceof Empresa)
            return (T) empresaService.findById(actor.getId()).orElse(null);
        if (actor instanceof Usuario)
            return (T) usuarioService.findById(actor.getId()).orElse(null);

        return null;
    }
}