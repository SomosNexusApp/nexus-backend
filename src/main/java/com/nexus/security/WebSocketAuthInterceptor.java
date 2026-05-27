package com.nexus.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nexus.repository.ActorRepository;

import java.time.LocalDateTime;

/**
 * Interceptor para autenticar conexiones WebSocket usando JWT.
 * Permite que convertAndSendToUser funcione correctamente al asociar
 * el token del header native con el Principal de la sesión STOMP.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    @Lazy
    private ActorRepository actorRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extraer el token del header "Authorization" (o el que use tu frontend)
            // StompHeaderAccessor.getNativeHeader devuelve una lista
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtUtils.validateToken(token)) {
                    String username = jwtUtils.getUsernameOfToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (userDetails != null) {
                        // Comprobar si el actor está baneado o suspendido temporalmente
                        boolean blocked = actorRepository.findByUsername(username)
                            .or(() -> actorRepository.findByEmail(username))
                            .map(actor -> {
                                boolean isSuspended = actor.getSuspendidoHasta() != null && 
                                                      actor.getSuspendidoHasta().isAfter(LocalDateTime.now());
                                boolean isBanned = actor.isBaneado();
                                return isBanned || isSuspended;
                            })
                            .orElse(false);

                        if (blocked) {
                            System.err.println("❌ WS Auth denegada: Usuario bloqueado o suspendido: " + username);
                            throw new AccessDeniedException("Usuario bloqueado o suspendido");
                        }

                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        
                        // Establecemos el usuario en la sesión del WebSocket
                        accessor.setUser(authentication);
                        System.out.println("✅ WS Auth exitosa para: " + username);
                    }
                } else {
                    System.err.println("❌ WS Auth fallida: Token inválido o expirado");
                }
            } else {
                System.err.println("⚠️ WS Auth: No se encontró header Authorization válido en CONNECT");
            }
        }
        return message;
    }
}
