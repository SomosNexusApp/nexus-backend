package com.nexus.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nexus.service.AnuncioCaducidadService;

// scheduler de caducidad: se ejecuta automaticamente una vez al dia
// delega toda la logica en AnuncioCaducidadService — este archivo solo es el "despertador"
@Component
public class AnuncioCaducidadScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnuncioCaducidadScheduler.class);
    private final AnuncioCaducidadService anuncioCaducidadService;

    public AnuncioCaducidadScheduler(AnuncioCaducidadService anuncioCaducidadService) {
        this.anuncioCaducidadService = anuncioCaducidadService;
    }

    // cron expression: seg min hora dia mes dia-semana
    // "0 0 8 * * *" = cada dia a las 8:00 AM (hora del servidor)
    // si el servidor estuviera en UTC y los usuarios en UTC+2, las 8:00 serian las 10:00 local
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
