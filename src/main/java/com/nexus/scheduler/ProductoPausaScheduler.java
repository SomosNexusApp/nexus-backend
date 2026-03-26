package com.nexus.scheduler;

import com.nexus.service.AdminProductosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron que revisa cada minuto los productos con pausa vencida y los reactiva automáticamente.
 * Usa @Scheduled(fixedDelay = 60_000) para no solapar ejecuciones consecutivas.
 */
@Component
public class ProductoPausaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProductoPausaScheduler.class);

    private final AdminProductosService service;

    public ProductoPausaScheduler(AdminProductosService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 60_000)
    public void reactivarProductosVencidos() {
        int count = service.reactivarVencidos();
        if (count > 0) {
            log.info("[SCHEDULER] Reactivados {} producto(s) con pausa vencida.", count);
        }
    }
}
