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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Autowired private UsuarioService usuarioService;
    @Autowired private JWTAuthenticationFilter jwtFilter;
    @Autowired private PasswordEncoder passwordEncoder;

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

                // ── Swagger / actuator / WebSockets ─────────────────────────
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**", "/v3/api-docs",
                    "/actuator/**", "/ws/**"
                ).permitAll()

                // ── Auth: registro, login, verify, reset, OAuth ─────────────
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()

                // ── Legal: T&C y privacidad ─────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/legal/**", "/legal/**").permitAll()

                // ── Contenido publico (lectura estilo Wallapop) ─────────────
                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/producto/**", "/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ofertas/**", "/oferta/**", "/ofertas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vehiculos/**", "/vehiculo/**", "/vehiculos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias/**", "/categoria/**", "/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comentarios/**", "/comentario/**", "/comentarios/**").permitAll()

                // ── Perfiles publicos de usuario ────────────────────────────
                .requestMatchers(HttpMethod.GET, 
                    "/api/usuarios/*/perfil", "/usuario/*/perfil",
                    "/api/usuarios/*/valoraciones", "/usuario/*/valoraciones",
                    "/api/usuarios/*/productos", "/usuario/*/productos"
                ).permitAll()

                // ── Newsletter: suscripcion sin cuenta ──────────────────────
                .requestMatchers(HttpMethod.POST, "/api/newsletter/suscribir", "/newsletter/suscribir").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/newsletter/confirmar", "/newsletter/confirmar").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/newsletter/cancelar", "/newsletter/cancelar").permitAll()

                // ── Admin: solo rol ADMIN ───────────────────────────────────
                .requestMatchers("/api/admin/**", "/admin/**", "/api/moderation/**").hasRole("ADMIN")

                // ── Todo lo demas requiere autenticacion (JWT) ──────────────
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