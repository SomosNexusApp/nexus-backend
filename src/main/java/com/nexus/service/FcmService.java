package com.nexus.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.nexus.entity.PushToken;
import com.nexus.repository.PushTokenRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para enviar notificaciones push nativas mediante Firebase Cloud Messaging (FCM).
 * Se inicializa con el fichero de credenciales de la cuenta de servicio de Firebase.
 *
 * Configuración requerida (una de las dos):
 *   1. Fichero en classpath: src/main/resources/firebase-service-account.json
 *   2. Variable de entorno FIREBASE_SERVICE_ACCOUNT_JSON con el contenido del JSON
 *
 * Si no hay credenciales configuradas, el servicio se deshabilita y no envía nada
 * (no rompe el flujo normal de la aplicación).
 */
@Service
public class FcmService {

    @Autowired
    private PushTokenRepository pushTokenRepository;

    /** Contenido del JSON de la cuenta de servicio como variable de entorno (Render/producción). */
    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        // Evitar doble inicialización si ya hay una app Firebase
        if (!FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            return;
        }

        try {
            InputStream credentialsStream = resolveCredentials();
            if (credentialsStream == null) {
                System.out.println("[FCM] Sin credenciales configuradas — push deshabilitado.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            FirebaseApp.initializeApp(options);
            initialized = true;
            System.out.println("[FCM] Firebase inicializado correctamente.");
        } catch (IOException e) {
            System.err.println("[FCM] Error inicializando Firebase: " + e.getMessage());
        }
    }

    /**
     * Resuelve las credenciales de Firebase desde variable de entorno o classpath.
     */
    private InputStream resolveCredentials() throws IOException {
        // 1. Variable de entorno (para Render / producción)
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }
        // 2. Fichero en classpath (para desarrollo local)
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
        if (resource.exists()) {
            return resource.getInputStream();
        }
        return null;
    }

    /**
     * Envía una notificación push a todos los dispositivos activos de un actor.
     *
     * @param actorId ID del actor destino
     * @param titulo  Título de la notificación
     * @param cuerpo  Cuerpo/mensaje de la notificación
     * @param url     URL de navegación al tocar la notificación (puede ser null)
     */
    @Transactional
    public void sendPush(Integer actorId, String titulo, String cuerpo, String url) {
        if (!initialized || actorId == null) return;

        List<PushToken> tokens = pushTokenRepository.findByActorIdAndActivoTrue(actorId);
        if (tokens.isEmpty()) return;

        List<String> tokenValues = tokens.stream()
                .map(PushToken::getToken)
                .collect(Collectors.toList());

        // Construimos el payload de datos adicionales (para manejo en foreground)
        var dataBuilder = com.google.common.collect.ImmutableMap.<String, String>builder()
                .put("titulo", titulo != null ? titulo : "")
                .put("cuerpo", cuerpo != null ? cuerpo : "");
        if (url != null && !url.isBlank()) {
            dataBuilder.put("url", url);
        }

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(titulo)
                        .setBody(cuerpo)
                        .build())
                .putAllData(dataBuilder.build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setIcon("ic_notification")
                                .setColor("#6366f1")
                                .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .setBadge(1)
                                .build())
                        .build())
                .addAllTokens(tokenValues)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            handleInvalidTokens(response, tokens);
        } catch (FirebaseMessagingException e) {
            System.err.println("[FCM] Error enviando push a actor " + actorId + ": " + e.getMessage());
        }
    }

    /**
     * Desactiva en BD los tokens que FCM reporta como inválidos o no registrados.
     */
    private void handleInvalidTokens(BatchResponse response, List<PushToken> tokens) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (!r.isSuccessful() && r.getException() != null) {
                MessagingErrorCode code = r.getException().getMessagingErrorCode();
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    pushTokenRepository.deactivateByToken(tokens.get(i).getToken());
                }
            }
        }
    }
}
