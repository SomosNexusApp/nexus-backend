package com.nexus.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import com.nexus.security.WebSocketAuthInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker en memoria: prefijo /topic (broadcast) y /queue (por usuario)
        // Heartbeat reducido para ahorrar recursos en instancias pequeñas:
        // [server-send-interval-ms, server-receive-interval-ms]
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{25000, 25000});

        // Prefijo para mensajes que van al @MessageMapping de los controllers
        config.setApplicationDestinationPrefixes("/app");
        // Prefijo para mensajes dirigidos a un usuario especifico
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Reducir threads del canal de entrada al mínimo para ahorrar CPU/RAM
        registration.taskExecutor()
                .corePoolSize(1)
                .maxPoolSize(2)
                .queueCapacity(50);
        registration.interceptors(webSocketAuthInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Reducir threads del canal de salida al mínimo
        registration.taskExecutor()
                .corePoolSize(1)
                .maxPoolSize(2)
                .queueCapacity(50);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Limitar tamaño de mensajes para proteger memoria en instancias pequeñas
        registration
                // Tamaño máximo de mensaje de texto: 64 KB
                .setMessageSizeLimit(64 * 1024)
                // Tamaño máximo del buffer de envío por sesión: 128 KB
                .setSendBufferSizeLimit(128 * 1024)
                // Tiempo máximo de espera para enviar al cliente: 10 s
                .setSendTimeLimit(10 * 1000);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
                // Reducir el timeout de desconexión de SockJS (default 5 min → 1 min)
                .setDisconnectDelay(60_000)
                // Heartbeat de SockJS cada 25 s (default 25 s, explícito para claridad)
                .setHeartbeatTime(25_000)
                // Deshabilitar sesiones JSR-356 para reducir overhead de classpath scanning
                .setSessionCookieNeeded(false);
    }
}
