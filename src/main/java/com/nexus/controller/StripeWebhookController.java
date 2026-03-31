package com.nexus.controller;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.nexus.service.CompraService;
import com.nexus.service.ContratoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Autowired
    private CompraService compraService;

    @Autowired
    private ContratoService contratoService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            if (webhookSecret != null && !webhookSecret.isEmpty()) {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } else {
                log.warn("Stripe Webhook Secret no configurado — procesando sin verificación de firma (solo desarrollo)");
                // En desarrollo sin stripe-cli, se podría saltar la verificación, 
                // pero lo ideal es usar el secret.
                return ResponseEntity.badRequest().body("Webhook Secret missing");
            }
        } catch (Exception e) {
            log.error("Error verificando webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook Error");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            try {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null && session.getMetadata() != null && session.getMetadata().get("contrato_id") != null) {
                    log.info("Checkout completado contrato publicidad: session={}", session.getId());
                    contratoService.activarTrasCheckoutCompletado(session.getId());
                }
            } catch (Exception e) {
                log.error("Webhook checkout contrato: {}", e.getMessage());
            }
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
            log.info("Pago recibido vía Webhook: PI={}", intent.getId());
            try {
                compraService.confirmarPagoPorStripeId(intent.getId());
            } catch (Exception e) {
                log.error("Error confirmando compra vía webhook: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok("Received");
    }
}
