package com.nexus.service;

import com.nexus.entity.NewsletterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NewsletterScheduler {

    @Autowired
    private NewsletterService newsletterService;

    /**
     * Revisa cada hora si toca enviar el newsletter semanal automatizado.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkAndSendWeeklyNewsletter() {
        NewsletterConfig config = newsletterService.getConfig();

        if (!config.isAutomatedEnabled()) return;

        LocalDateTime now = LocalDateTime.now();
        
        // Verificar si es el día de la semana correcto
        if (now.getDayOfWeek().getValue() != config.getDayOfWeek()) return;

        // Verificar si es la hora correcta (margen de 1 hora por el cron)
        if (now.getHour() != config.getTimeOfDay().getHour()) return;

        // Verificar que no se haya enviado ya hoy
        if (config.getLastSent() != null && config.getLastSent().toLocalDate().isEqual(now.toLocalDate())) {
            return;
        }

        // Ejecutar envío
        triggerNewsletter(config);
    }

    private void triggerNewsletter(NewsletterConfig config) {
        String asunto = "Nexus Elite: Tu selección semanal personalizada ✨";
        String htmlContent = newsletterService.generateWeeklyDigestHtml();
        
        newsletterService.enviarAActivos(asunto, htmlContent);

        // Actualizar última fecha de envío
        config.setLastSent(LocalDateTime.now());
        newsletterService.saveConfig(config);
    }
}
