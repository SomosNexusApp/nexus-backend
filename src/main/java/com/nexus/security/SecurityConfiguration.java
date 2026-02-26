package com.nexus.security;

import com.nexus.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion de seguridad de Nexus.
 *
 * FIX BeanDefinitionOverrideException:
 *   El @Bean passwordEncoder() fue ELIMINADO de esta clase.
 *   Ahora reside EXCLUSIVAMENTE en PasswordConfig.java
 *   (com/nexus/security/PasswordConfig.java).
 *
 *   Spring Boot encuentra el bean 'passwordEncoder' en PasswordConfig
 *   y lo inyecta aqui a traves de @Autowired.
 *   Tener el @Bean en DOS clases simultaneamente causa el error:
 *     BeanDefinitionOverrideException: Invalid bean definition with name 'passwordEncoder'
 *
 * RUTAS PUBLICAS (sin autenticacion):
 *   GET  /api/productos/**         <- listado y detalle publico
 *   GET  /api/ofertas/**           <- feed y detalle publico
 *   GET  /api/vehiculos/**         <- busqueda y detalle publico
 *   GET  /api/categorias/**        <- arbol de categorias publico
 *   POST /api/auth/**              <- registro, login, oauth
 *   GET  /api/usuarios/{id}/perfil <- perfil publico de usuario
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Autowired private UsuarioService   usuarioService;
    @Autowired private JWTAuthenticationFilter jwtFilter;
    @Autowired private PasswordEncoder  passwordEncoder; // inyectado desde PasswordConfig

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── Swagger / actuator ──────────────────────────────────────
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**", "/v3/api-docs",
                    "/actuator/**", "/ws/**"
                ).permitAll()

                // ── Auth: registro, login, verify, reset, OAuth ─────────────
                .requestMatchers("/api/auth/**").permitAll()

                // ── Contenido publico (lectura) ─────────────────────────────
                // Nexus funciona como Wallapop / Chollometro:
                // cualquier visitante puede ver productos, ofertas y vehiculos
                // sin necesidad de cuenta.
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ofertas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vehiculos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comentarios/**").permitAll()

                // Perfil publico de usuario (sin datos privados)
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*/perfil").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*/valoraciones").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*/productos").permitAll()

                // ── Newsletter: suscripcion publica ─────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/newsletter/suscribir").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/newsletter/confirmar").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/newsletter/cancelar").permitAll()

                // ── Admin: solo nivelAcceso > 0 ─────────────────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ── Todo lo demas requiere autenticacion ───────────────────
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:4200", "http://localhost:*", "https://*.nexus.app"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}