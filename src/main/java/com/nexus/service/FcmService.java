package com.nexus.service;

import org.springframework.stereotype.Service;

/**
 * Stub de FCM — Las notificaciones push en la APK se gestionan mediante
 * notificaciones locales de Capacitor (@capacitor/local-notifications),
 * disparadas directamente desde el WebSocket en el frontend.
 * No se necesita ningún servicio externo ni clave de API.
 */
@Service
public class FcmService {

    /**
     * No-op: el envío push real ocurre en el frontend vía local-notifications.
     */
    public void sendPush(Integer actorId, String titulo, String cuerpo, String url) {
        // No hace nada — implementación movida al frontend con Capacitor Local Notifications
    }
}
