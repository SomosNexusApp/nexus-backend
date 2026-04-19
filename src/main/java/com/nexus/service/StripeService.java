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

// servicio que encapsula todas las operaciones con la API de Stripe
// paymentIntents (cobros), refunds (devoluciones), customers y metodos de pago
@Service
public class StripeService {

    @Value("${stripe.api.key:}") // si no esta configurado usa string vacio
    private String stripeApiKey;

    @Value("${stripe.currency:eur}") // usamos euros por defecto
    private String moneda;

    // al arrancar el servicio le pasamos la api key a la libreria de Stripe
    // se llama solo una vez al iniciar el servidor con @PostConstruct
    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
        }
    }

    /**
     * Crea un PaymentIntent en Stripe.
     * El PaymentIntent es como un "intento de pago" que queda pendiente hasta que el cliente
     * introduce la tarjeta. El dinero se retiene y despues se captura automaticamente.
     * idempotencyKey: clave unica para evitar crear el mismo pago dos veces si hay reintentos.
     */
    public PaymentIntent crearIntentoPago(Double cantidad, String descripcion, String idempotencyKey, String customerId)
            throws Exception {
        validarConfig();

        // stripe trabaja en centimos de euro, no en decimales (10.50€ = 1050 centimos)
        long centimos = (long) (cantidad * 100);

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(centimos)
                .setCurrency(moneda)
                .setDescription(descripcion)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC) // captura automatica al confirmar
                .addPaymentMethodType("card"); // solo aceptamos tarjeta

        if (customerId != null && !customerId.isEmpty()) {
            builder.setCustomer(customerId); // vinculamos al cliente de Stripe si existe
        }

        PaymentIntentCreateParams params = builder.build();

        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            com.stripe.net.RequestOptions requestOptions = com.stripe.net.RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey) // si se manda la misma key, Stripe devuelve el mismo resultado sin duplicar
                    .build();
            return PaymentIntent.create(params, requestOptions);
        }

        return PaymentIntent.create(params);
    }

    /**
     * Procesa un reembolso total.
     * Se usa cuando se cancela una compra pagada o cuando se resuelve una disputa a favor del comprador.
     */
    public Refund reembolsar(String paymentIntentId) throws Exception {
        validarConfig();

        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();

        return Refund.create(params);
    }

    /**
     * Reembolso parcial (por ejemplo, solo el precio del producto pero no los gastos de envio).
     */
    public Refund reembolsarParcial(String paymentIntentId, Double cantidad) throws Exception {
        validarConfig();

        long centimos = (long) (cantidad * 100);

        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(centimos) // especificamos cuanto reembolsar
                .build();

        return Refund.create(params);
    }

    // validacion basica: si no hay api key configurada, no tiene sentido llamar a Stripe
    private void validarConfig() throws Exception {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            throw new Exception("Stripe API Key no configurada en application.properties");
        }
    }

    // busca el cliente de Stripe por su stripeCustomerId o lo crea si no existe
    // asi evitamos crear clientes duplicados en Stripe para el mismo usuario
    public Customer getOrCreateCustomer(Actor actor) throws Exception {
        validarConfig();
        if (actor.getStripeCustomerId() != null && !actor.getStripeCustomerId().isEmpty()) {
            return Customer.retrieve(actor.getStripeCustomerId()); // ya existe, lo recuperamos
        }

        // no existe, lo creamos con su email y nombre
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(actor.getEmail())
                .setName(actor.getNombre() + " " + actor.getApellidos())
                .build();
        Customer customer = Customer.create(params);
        actor.setStripeCustomerId(customer.getId()); // guardamos el id para no crearlo otra vez
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