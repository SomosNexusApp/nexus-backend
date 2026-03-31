package com.nexus.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nexus.service.EnvioService;

@Component
public class EnvioTrackingScheduler {

    private static final Logger log = LoggerFactory.getLogger(EnvioTrackingScheduler.class);
    private final EnvioService envioService;

    public EnvioTrackingScheduler(EnvioService envioService) {
        this.envioService = envioService;
    }

    @Scheduled(fixedDelayString = "${nexus.shipping.tracking-refresh-ms:300000}")
    public void refrescarTracking() {
        int n = envioService.refrescarTrackingPendientes();
        if (n > 0) {
            log.info("[SCHEDULER] Envíos actualizados por tracking: {}", n);
        }
    }
}
