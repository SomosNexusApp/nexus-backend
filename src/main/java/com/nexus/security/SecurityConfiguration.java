package com.nexus.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nexus.service.UsuarioService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

        @Autowired
        private JWTAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        @Lazy
        private UsuarioService usuarioService;

	@org.springframework.beans.factory.annotation.Value("${nexus.frontend.url:http://localhost:4200}")
	private String frontendUrl;

	@org.springframework.beans.factory.annotation.Value("${nexus.admin.url:http://localhost:4201}")
	private String adminUrl;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);

		// Patrones de orígenes permitidos (soporta wildcards con allowCredentials=true)
		config.setAllowedOriginPatterns(java.util.List.of(
			"https://nexus-app.es",
			"https://*.nexus-app.es",
			"https://*.vercel.app",
			"http://localhost:4200",
			"http://localhost:4201",
			"http://127.0.0.1:4200"
		));

		config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
		config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setExposedHeaders(java.util.List.of("Authorization", "Cache-Control", "X-Cache-Status"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
                        throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                                                .permitAll()
                                                .requestMatchers("/auth/login", "/auth/registro", "/api/auth/login",
                                                                "/api/auth/verify-2fa")
                                                .permitAll()
                                                .requestMatchers("/api/auth/me").authenticated()
                                                .requestMatchers("/auth/**", "/api/auth/**").permitAll()

                                                // BLOQUE 0 – ACCESO PÚBLICO GENERAL (PUBLICIDAD Y PERFILES)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/public/**", "/public/**",
                                                                "/usuario/username/**", "/api/usuarios/username/**",
                                                                "/usuario/*/perfil", "/api/usuarios/*/perfil")
                                                .permitAll()

                                                // BLOQUE 1 – INFRAESTRUCTURA
                                                .requestMatchers(
                                                                "/", "/logo.webp", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                                                "/actuator/health", "/error", "/favicon.ico", "/ecentia-icon.ico",
                                                                "/postman-icon.webp", "/swagger-logo.webp",
                                                                "/api/webhooks/**",
                                                                "/envio/correos/evento")
                                                .permitAll()

                                                // BLOQUE 2 – WEBSOCKET
                                                .requestMatchers("/ws/**").permitAll()

                                                // BLOQUE 4 – PRODUCTOS (Arreglado para permitir raíz y subrutas)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/producto", "/producto/**",
                                                                "/productos", "/productos/**",
                                                                "/api/productos", "/api/productos/**")
                                                .permitAll()

                                                // BLOQUE 5 – VEHÍCULOS (Arreglado)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/vehiculo", "/vehiculo/**",
                                                                "/vehiculos", "/vehiculos/**",
                                                                "/api/vehiculos", "/api/vehiculos/**")
                                                .permitAll()

                                                // BLOQUE 6 – OFERTAS (Arreglado)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/oferta", "/oferta/**",
                                                                "/ofertas", "/ofertas/**",
                                                                "/api/ofertas", "/api/ofertas/**")
                                                .permitAll()

                                                // BLOQUE 7 – CATEGORÍAS (Arreglado - ESTO CAUSABA EL 403 ACTUAL)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/categorias", "/categorias/**",
                                                                "/api/categorias", "/api/categorias/**")
                                                .permitAll()

                                                // BLOQUE 8 – COMENTARIOS
                                                .requestMatchers(HttpMethod.GET,
                                                                "/comentario", "/comentario/**",
                                                                "/api/comentarios", "/api/comentarios/**")
                                                .permitAll()

                                                // BLOQUE 9 – VALORACIONES
                                                .requestMatchers(HttpMethod.GET,
                                                                "/valoracion", "/valoracion/**",
                                                                "/api/valoraciones", "/api/valoraciones/**")
                                                .permitAll()

                                                // BLOQUE 10 – PERFIL PÚBLICO
                                                .requestMatchers(HttpMethod.GET,
                                                                "/usuario/*/perfil", "/usuario/*/valoraciones",
                                                                "/usuario/*/productos", "/usuario/username/*",
                                                                "/api/usuarios/*/perfil",
                                                                "/api/usuarios/*/valoraciones",
                                                                "/api/usuarios/*/productos", "/api/usuarios/username/*")
                                                .permitAll()

                                                // BLOQUE 10.5 - PUBLICIDAD PÚBLICA
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/public/publicidad/**", "/public/publicidad/**")
                                                .permitAll()

                                                // BLOQUE 11 – NEWSLETTER
                                                .requestMatchers(HttpMethod.POST,
                                                                "/newsletter/suscribir", "/api/newsletter/suscribir")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/newsletter/confirmar", "/newsletter/cancelar",
                                                                "/newsletter/baja",
                                                                "/newsletter/estado",
                                                                "/api/newsletter/confirmar", "/api/newsletter/cancelar")
                                                .permitAll()

                                                // BLOQUE 12 – LEGAL
                                                .requestMatchers(HttpMethod.GET, "/legal/**", "/api/legal/**")
                                                .permitAll()

                                                // BLOQUE 13 – VOTOS
                                                .requestMatchers(HttpMethod.GET, "/votos/**", "/api/spark-votos/**")
                                                .permitAll()

                                                // BLOQUE 14 – BÚSQUEDA UNIFICADA (Público)
                                                .requestMatchers(HttpMethod.GET, "/market/**", "/api/market/**")
                                                .permitAll()

                                                // BLOQUE 16 – FAVORITOS (Público para lectura, Auth para escritura)
                                                .requestMatchers(HttpMethod.GET, "/api/favoritos/usuario/**",
                                                                "/favoritos/usuario/**")
                                                .permitAll()
                                                .requestMatchers("/favorito/**", "/api/favoritos/**")
                                                .authenticated()

                                                // BLOQUE 15 – SOLO ADMIN
                                                .requestMatchers("/admin/**", "/api/admin/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                // check-text lo usan TODOS los usuarios autenticados (publicar
                                                // producto/oferta/vehículo)
                                                .requestMatchers(HttpMethod.POST, "/api/moderation/check-text")
                                                .authenticated()
                                                .requestMatchers("/api/moderation/**").hasAuthority("ROLE_ADMIN")
                                                .requestMatchers("/api/newsletter/admin/**", "/newsletter/admin/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers("/contrato/**", "/api/contratos/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers("/empresa/admin/**", "/api/empresas/admin/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.PATCH,
                                                                "/usuario/*/banear", "/usuario/*/verificar",
                                                                "/api/usuarios/*/banear", "/api/usuarios/*/verificar")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers("/reporte/admin/**", "/api/reportes/admin/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/usuario/*", "/api/usuarios/*")
                                                .hasAuthority("ROLE_ADMIN")

                                                // BLOQUE 15 – AUTENTICADOS
                                                .requestMatchers(HttpMethod.POST, "/producto/**", "/api/productos/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/producto/**", "/api/productos/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PATCH, "/producto/**", "/api/productos/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/producto/**", "/api/productos/**")
                                                .authenticated()

                                                .requestMatchers(HttpMethod.POST, "/vehiculo/**", "/api/vehiculos/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/vehiculo/**", "/api/vehiculos/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PATCH, "/vehiculo/**", "/api/vehiculos/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/vehiculo/**", "/api/vehiculos/**")
                                                .authenticated()

                                                .requestMatchers(HttpMethod.POST, "/oferta/**", "/api/ofertas/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/oferta/**", "/api/ofertas/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PATCH, "/oferta/**", "/api/ofertas/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/oferta/**", "/api/ofertas/**")
                                                .authenticated()

                                                .requestMatchers(HttpMethod.POST, "/comentario/**",
                                                                "/api/comentarios/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/comentario/**",
                                                                "/api/comentarios/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/comentario/**",
                                                                "/api/comentarios/**")
                                                .authenticated()

                                                .requestMatchers(HttpMethod.POST, "/valoracion/**",
                                                                "/api/valoraciones/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/valoracion/**",
                                                                "/api/valoraciones/**")
                                                .authenticated()

                                                .requestMatchers(HttpMethod.POST, "/votos/**", "/api/spark-votos/**",
                                                                "/api/drip-votos/**")
                                                .authenticated()

                                                .requestMatchers("/compra/**", "/api/compras/**").authenticated()
                                                .requestMatchers("/devolucion/**", "/api/devoluciones/**")
                                                .authenticated()

                                                .requestMatchers("/mensaje/**", "/chat/**", "/api/mensajes/**",
                                                                "/api/chat-mensajes/**",
                                                                "/api/conversaciones/**")
                                                .authenticated()

                                                .requestMatchers("/favorito/**", "/api/favoritos/**").authenticated()
                                                .requestMatchers("/bloqueo/**", "/api/bloqueos/**").authenticated()
                                                .requestMatchers("/api/notificaciones/**").authenticated()
                                                .requestMatchers("/envio/**", "/api/envios/**").authenticated()
                                                .requestMatchers("/ajustes/**", "/api/usuarios/me/**").authenticated()

                                                .requestMatchers("/usuario/**", "/api/usuarios/**").authenticated()
                                                .requestMatchers("/reporte/**", "/api/reportes/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/newsletter/preferencias",
                                                                "/newsletter/baja")
                                                .authenticated()
                                                .requestMatchers("/empresa/**", "/api/empresas/**").authenticated()

                                                // BLOQUE 16 – DENY BY DEFAULT
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
