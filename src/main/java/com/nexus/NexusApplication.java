package com.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Nexus — Wallapop + Chollometro
 *
 * @EnableAsync      → EmailService y NotificacionService no bloquean el hilo HTTP
 * @EnableScheduling → UpvoteRankingScheduler + limpieza de expiradas
 * @EnableCaching    → Caché Spring para consultas frecuentes
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
public class NexusApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusApplication.class, args);
    }
}