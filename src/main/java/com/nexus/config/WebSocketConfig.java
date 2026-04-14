package com.nexus.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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

    /**
     * TaskScheduler mínimo para el heartbeat del SimpleBroker.
     * Sin este bean, setHeartbeatValue lanza IllegalArgumentException al arrancar.
     * Un solo thread es suficiente para los pings de keep-alive en instancias pequeñas.
     */
    @Bean
    public ThreadPoolTaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker en memoria con scheduler explícito para heartbeat.
        // Heartbeat cada 25 s (send, receive) — suficiente para mantener conexiones vivas
        // sin desperdiciar CPU en instancias de 0.1 vCPU.
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{25000, 25000})
              .setTaskScheduler(webSocketTaskScheduler());

        // Prefijo para mensajes que van al @MessageMapping de los controllers
        config.setApplicationDestinationPrefixes("/app");
        // Prefijo para mensajes dirigidos a un usuario específico
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
                // Heartbeat de SockJS cada 25 s
                .setHeartbeatTime(25_000)
                // Sin cookie de sesión — reduce overhead en entornos stateless
                .setSessionCookieNeeded(false);
    }
}
