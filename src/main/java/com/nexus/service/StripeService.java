package com.nexus.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.currency:eur}")
    private String moneda;

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
        }
    }

    /**
     * Crea un PaymentIntent en Stripe.
     * Los fondos quedan retenidos (escrow) hasta captura o cancelación.
     */
    public PaymentIntent crearIntentoPago(Double cantidad, String descripcion, String idempotencyKey) throws Exception {
        validarConfig();

        long centimos = (long) (cantidad * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(centimos)
                .setCurrency(moneda)
                .setDescription(descripcion)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            com.stripe.net.RequestOptions requestOptions = com.stripe.net.RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();
            return PaymentIntent.create(params, requestOptions);
        }

        return PaymentIntent.create(params);
    }

    /**
     * Procesa un reembolso total para un PaymentIntent.
     * Se usa cuando se cancela una compra ya pagada o se resuelve una disputa a favor del comprador.
     */
    public Refund reembolsar(String paymentIntentId) throws Exception {
        validarConfig();

        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();

        return Refund.create(params);
    }

    /**
     * Reembolso parcial (por ejemplo, solo el precio del producto sin el envío).
     */
    public Refund reembolsarParcial(String paymentIntentId, Double cantidad) throws Exception {
        validarConfig();

        long centimos = (long) (cantidad * 100);

        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(centimos)
                .build();

        return Refund.create(params);
    }

    private void validarConfig() throws Exception {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            throw new Exception("Stripe API Key no configurada en application.properties");
        }
    }
}