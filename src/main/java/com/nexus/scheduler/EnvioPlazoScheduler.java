package com.nexus.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nexus.service.EnvioService;

/**
 * Reembolso automático si el vendedor no envía antes de {@code fechaLimiteEnvio}.
 */
@Component
public class EnvioPlazoScheduler {

    private static final Logger log = LoggerFactory.getLogger(EnvioPlazoScheduler.class);
    private final EnvioService envioService;

    public EnvioPlazoScheduler(EnvioService envioService) {
        this.envioService = envioService;
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void procesar() {
        int n = envioService.procesarEnviosPendientesPlazoVencido();
        if (n > 0) {
            log.info("[SCHEDULER] Reembolsos por plazo de envío procesados: {}", n);
        }
    }
}
