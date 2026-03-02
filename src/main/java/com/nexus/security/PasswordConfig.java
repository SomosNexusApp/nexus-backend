package com.nexus.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder en clase separada para evitar dependencia circular:
 *
 *   SecurityConfiguration → JWTAuthenticationFilter
 *        → UsuarioService (UserDetailsService)
 *        → PasswordEncoder
 *        → SecurityConfiguration   ← CICLO
 *
 * Al tener el @Bean aquí, SecurityConfiguration puede inyectar
 * PasswordEncoder sin crear el ciclo.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}