package com.nexus.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro JWT que se ejecuta una vez por request.
 *
 * REGLA DE ORO: este filtro NUNCA debe escribir en la respuesta
 * ni lanzar excepciones. Su único trabajo es:
 *   1. Leer el token del header Authorization
 *   2. Si es válido → autenticar en el SecurityContext
 *   3. En cualquier otro caso → continuar la cadena sin autenticar
 *
 * Las decisiones de acceso (401/403) las toma Spring Security
 * DESPUÉS de este filtro, basándose en las reglas de authorizeHttpRequests.
 * Eso garantiza que las rutas con permitAll() funcionen sin token.
 */
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraer token (null si no hay header Authorization o no empieza por "Bearer ")
        String token = jwtUtils.getToken(request);

        // 2. Si no hay token: continuar SIN autenticar.
        //    Spring Security permitirá las rutas públicas y bloqueará las privadas.
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Validar token. validateToken() ya no lanza excepciones — devuelve false.
        try {
            if (jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameOfToken(token);

                // Solo autenticar si hay username y el contexto está vacío
                if (StringUtils.hasText(username) &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    // Adjuntar detalles de la request (IP, session id…)
                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            // Si validateToken devuelve false: token inválido/expirado
            // → simplemente no autenticamos, continuamos la cadena
        } catch (Exception e) {
            // Captura de seguridad ante cualquier error inesperado
            // (ej. loadUserByUsername lanza excepción)
            // → limpiar contexto y continuar; Spring Security gestionará el acceso
            SecurityContextHolder.clearContext();
        }

        // 4. Siempre continuar la cadena de filtros
        filterChain.doFilter(request, response);
    }
}