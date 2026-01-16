package com.nexus.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    private JWTAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // --- Rutas PÚBLICAS ---
                .requestMatchers(HttpMethod.POST, "/login").permitAll() // Endpoint de login
                .requestMatchers(HttpMethod.GET, "/producto", "/producto/**").permitAll() // Ver productos es público
                
                // --- Rutas SWAGGER (Documentación) ---
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // --- Rutas ADMIN ---
                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                
                // --- Rutas EMPRESA ---
                .requestMatchers("/empresa/**").hasAuthority("EMPRESA")
                // Las empresas suelen crear productos
                .requestMatchers(HttpMethod.POST, "/producto").hasAuthority("EMPRESA") 
                .requestMatchers(HttpMethod.PUT, "/producto/**").hasAuthority("EMPRESA")
                .requestMatchers(HttpMethod.DELETE, "/producto/**").hasAuthority("EMPRESA")

                // --- Rutas USUARIO ---
                // Asumo que usuario es el que compra
                .requestMatchers("/compra/**").hasAnyAuthority("USUARIO", "ADMIN")
                
                // --- Rutas Comunes (Autenticados) ---
                .requestMatchers("/contrato/**").authenticated() // Empresas y Usuarios quizás usan contratos
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}