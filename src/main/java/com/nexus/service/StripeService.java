package com.nexus.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Customer;
import com.stripe.model.SetupIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.nexus.entity.Actor;

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
    public PaymentIntent crearIntentoPago(Double cantidad, String descripcion, String idempotencyKey, String customerId)
            throws Exception {
        validarConfig();

        long centimos = (long) (cantidad * 100);

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(centimos)
                .setCurrency(moneda)
                .setDescription(descripcion)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                .addPaymentMethodType("card");

        if (customerId != null && !customerId.isEmpty()) {
            builder.setCustomer(customerId);
        }

        PaymentIntentCreateParams params = builder.build();

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
     * Se usa cuando se cancela una compra ya pagada o se resuelve una disputa a
     * favor del comprador.
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

    public Customer getOrCreateCustomer(Actor actor) throws Exception {
        validarConfig();
        if (actor.getStripeCustomerId() != null && !actor.getStripeCustomerId().isEmpty()) {
            return Customer.retrieve(actor.getStripeCustomerId());
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(actor.getEmail())
                .setName(actor.getNombre() + " " + actor.getApellidos())
                .build();
        Customer customer = Customer.create(params);
        actor.setStripeCustomerId(customer.getId());
        return customer;
    }

    public SetupIntent createSetupIntent(String customerId) throws Exception {
        validarConfig();
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .build();
        return SetupIntent.create(params);
    }

    public PaymentMethodCollection getPaymentMethods(String customerId) throws Exception {
        validarConfig();
        PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
        return PaymentMethod.list(params);
    }

    public void removePaymentMethod(String paymentMethodId) throws Exception {
        validarConfig();
        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
        pm.detach();
    }

    /**
     * Checkout Stripe para pago de contrato publicitario (metadata contrato_id).
     */
    public Session crearCheckoutContrato(Integer contratoId, Integer empresaActorId, double montoEur,
            String successUrl, String cancelUrl) throws Exception {
        validarConfig();
        long centimos = Math.round(montoEur * 100);
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(moneda)
                                .setUnitAmount(centimos)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Contrato publicitario Nexus")
                                        .build())
                                .build())
                        .build())
                .putMetadata("contrato_id", String.valueOf(contratoId))
                .putMetadata("empresa_actor_id", String.valueOf(empresaActorId))
                .build();
        return Session.create(params);
    }
}