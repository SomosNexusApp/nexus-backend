package com.nexus.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nexus.service.AnuncioCaducidadService;

@Component
public class AnuncioCaducidadScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnuncioCaducidadScheduler.class);
    private final AnuncioCaducidadService anuncioCaducidadService;

    public AnuncioCaducidadScheduler(AnuncioCaducidadService anuncioCaducidadService) {
        this.anuncioCaducidadService = anuncioCaducidadService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void diario() {
        try {
            anuncioCaducidadService.ejecutarDiario();
            log.debug("[SCHEDULER] Caducidad de anuncios ejecutada");
        } catch (Exception e) {
            log.error("AnuncioCaducidadScheduler: {}", e.getMessage());
        }
    }
}
