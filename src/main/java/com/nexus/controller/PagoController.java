package com.nexus.controller;

import com.nexus.entity.Actor;
import com.nexus.repository.ActorRepository;
import com.nexus.service.StripeService;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/pago")
@Tag(name = "Pagos", description = "Gestión de métodos de pago con Stripe")
public class PagoController {

    @Autowired
    private StripeService stripeService;

    @Autowired
    private ActorRepository actorRepository;

    @PostMapping("/{actorId}/setup-intent")
    @Operation(summary = "Crear un SetupIntent para añadir una tarjeta")
    public ResponseEntity<?> createSetupIntent(@PathVariable Integer actorId) {
        try {
            Optional<Actor> opt = actorRepository.findById(actorId);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Actor actor = opt.get();

            Customer customer = stripeService.getOrCreateCustomer(actor);
            actorRepository.save(actor); // Guardar si se creó un customer_id nuevo

            SetupIntent intent = stripeService.createSetupIntent(customer.getId());
            return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{actorId}/metodos")
    @Operation(summary = "Listar métodos de pago guardados")
    public ResponseEntity<?> getMetodos(@PathVariable Integer actorId) {
        try {
            Optional<Actor> opt = actorRepository.findById(actorId);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Actor actor = opt.get();

            if (actor.getStripeCustomerId() == null) {
                return ResponseEntity.ok(Map.of("data", new java.util.ArrayList<>()));
            }
            PaymentMethodCollection pmc = stripeService.getPaymentMethods(actor.getStripeCustomerId());
            return ResponseEntity.ok(Map.of("data", pmc.getData()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/metodo/{paymentMethodId}")
    @Operation(summary = "Eliminar un método de pago")
    public ResponseEntity<?> deleteMetodo(@PathVariable String paymentMethodId) {
        try {
            stripeService.removePaymentMethod(paymentMethodId);
            return ResponseEntity.ok(Map.of("mensaje", "Método eliminado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
