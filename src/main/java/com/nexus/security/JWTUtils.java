package com.nexus.security;

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
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JWTUtils {

    @Autowired
    private ActorRepository actorRepository;

    @Autowired @Lazy
    private AdminService adminService;

    @Autowired @Lazy
    private EmpresaService empresaService;

    @Autowired @Lazy
    private UsuarioService usuarioService;

    @Value("${jwt.secret}")
    private String jwtFirma;

    @Value("${jwt.expiration:86400000}")
    private long extensionToken;

    // ── Extraer token del header ──────────────────────────────────────────

    public String getToken(HttpServletRequest request) {
        String tokenBearer = request.getHeader("Authorization");
        if (StringUtils.hasText(tokenBearer) && tokenBearer.startsWith("Bearer ")) {
            return tokenBearer.substring(7);
        }
        return null;
    }

    // ── Validar token ─────────────────────────────────────────────────────
    //
    //  ⚠️  BUG ORIGINAL: este método lanzaba AuthenticationCredentialsNotFoundException
    //  cuando el token era inválido o faltaba. Eso causaba que JWTAuthenticationFilter
    //  propagara la excepción hacia arriba, Spring la convertía en 403 Forbidden
    //  ANTES de que las reglas de autorización (permitAll/authenticated) pudieran
    //  evaluarse. Resultado: todas las rutas "públicas" devolvían 403.
    //
    //  FIX: ahora retorna false silenciosamente. El filtro simplemente no autentica
    //  al usuario y deja que Spring Security decida si la ruta requiere auth o no.

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtFirma)
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // ✅ CORRECTO: retornar false, nunca lanzar excepción desde aquí.
            // Si el token es inválido o ha expirado, simplemente no autenticamos.
            // Las rutas públicas (permitAll) seguirán funcionando sin token.
            return false;
        }
    }

    // ── Generar token ─────────────────────────────────────────────────────

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
                .signWith(SignatureAlgorithm.HS512, jwtFirma)
                .compact();
    }

    // ── Extraer username del token ────────────────────────────────────────

    public String getUsernameOfToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtFirma)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // ── Obtener actor autenticado en el contexto actual ───────────────────

    @SuppressWarnings("unchecked")
    public <T> T userLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;

        String username = authentication.getName();
        if (!StringUtils.hasText(username) || "anonymousUser".equals(username)) return null;

        Optional<Actor> actorO = actorRepository.findByUsername(username);
        if (actorO.isEmpty()) return null;

        Actor actor = actorO.get();

        if (actor instanceof Admin)   return (T) adminService.findById(actor.getId()).orElse(null);
        if (actor instanceof Empresa) return (T) empresaService.findById(actor.getId()).orElse(null);
        if (actor instanceof Usuario) return (T) usuarioService.findById(actor.getId()).orElse(null);

        return null;
    }
}