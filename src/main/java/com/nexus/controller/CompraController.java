package com.nexus.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.*;
import com.nexus.repository.CompraRepository;
import com.nexus.service.*;
import com.stripe.model.PaymentIntent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/compra")
@Tag(name = "Compras", description = "Ciclo completo de compra con pago seguro (escrow)")
public class CompraController {

    @Autowired private CompraRepository compraRepository;
    @Autowired private CompraService compraService;
    @Autowired private ProductoService productoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private StripeService stripeService;

    // ── HISTORIAL ──────────────────────────────────────────────────────────

    @GetMapping("/historial/{usuarioId}")
    @Operation(summary = "Historial de compras del usuario")
    public ResponseEntity<List<Compra>> historial(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(compraService.findHistorialUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver detalle de una compra")
    public ResponseEntity<?> findById(@PathVariable Integer id) {
        return compraService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

 // ── PASO 1: INICIAR PAGO ──────────────────────────────────────────────

    @PostMapping("/intent")
    @Operation(summary = "Paso 1: Crear PaymentIntent en Stripe calculando comisiones y envío")
    public ResponseEntity<?> crearIntentoPago(
            @RequestParam Integer productoId,
            @RequestParam Integer compradorId,
            @RequestParam TipoEnvio tipoEnvio,
            @RequestParam(required = false) String direccionCompleta,
            @RequestParam(required = false) String puntoRecogidaId) {

        Optional<Producto> p = productoService.findById(productoId);
        Optional<Usuario>  u = usuarioService.findById(compradorId);

        if (p.isEmpty() || u.isEmpty()) {
            return ResponseEntity.badRequest().body("Producto o Comprador no válidos");
        }

        if (p.get().getEstadoProducto() != EstadoProducto.DISPONIBLE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El producto ya no está disponible"));
        }

        if (p.get().getPublicador().getId() == compradorId) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No puedes comprar tu propio producto"));
        }

        try {
            Double precioProducto = p.get().getPrecio();
            
            // Calcular Comisión
            Double comisionNexus = compraService.calcularComisionNexus(precioProducto);

            // Calcular Costo de Envío
            Double costoEnvio = 0.0;
            if (tipoEnvio == TipoEnvio.DOMICILIO) {
                costoEnvio = 3.69;
            } else if (tipoEnvio == TipoEnvio.PUNTO_RECOGIDA) {
                costoEnvio = 2.69;
            }

            // Total que pagará el comprador en Stripe
            double totalCobrar = precioProducto + costoEnvio + comisionNexus;

            PaymentIntent intent = stripeService.crearIntentoPago(
                totalCobrar, "Nexus: " + p.get().getTitulo());

            // Crear compra PENDIENTE
            Compra compra = new Compra();
            compra.setComprador(u.get());
            compra.setProducto(p.get());
            compra.setFechaCompra(LocalDateTime.now());
            compra.setEstado(EstadoCompra.PENDIENTE);
            
            // Guardar los nuevos campos financieros y de logística
            compra.setPrecioFinal(totalCobrar);
            compra.setTipoEnvio(tipoEnvio);
            compra.setCostoEnvio(costoEnvio);
            compra.setComisionNexus(comisionNexus);
            
            if (tipoEnvio == TipoEnvio.DOMICILIO) {
                compra.setDireccionCompleta(direccionCompleta);
            } else if (tipoEnvio == TipoEnvio.PUNTO_RECOGIDA) {
                compra.setPuntoRecogidaId(puntoRecogidaId);
            }

            compraRepository.save(compra);

            // Preparar respuesta (DTO/Map ampliado)
            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            response.put("compraId",     compra.getId());
            response.put("precioProducto", precioProducto);
            response.put("costoEnvio",   costoEnvio);
            response.put("comisionNexus", comisionNexus);
            response.put("tipoEnvio",    tipoEnvio.name());
            response.put("total",        totalCobrar);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Captura el error si el precio supera los 1000 EUR
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error en Stripe: " + e.getMessage()));
        }
    }

    // ── PASO 2: CONFIRMAR PAGO ────────────────────────────────────────────

    /**
     * Angular llama aquí justo después de que Stripe.js confirme el pago.
     *
     * Body esperado:
     * {
     *   "paymentIntentId": "pi_3QxyzABC...",
     *   "metodoEntrega": "ENVIO_PAQUETERIA",    // o "ENTREGA_EN_PERSONA"
     *   "nombreDestinatario": "María García",
     *   "direccion": "Calle Mayor 10, 2B",
     *   "ciudad": "Madrid",
     *   "codigoPostal": "28001",
     *   "pais": "España",
     *   "telefono": "600123456",
     *   "precioEnvio": 4.99
     * }
     *
     * Respuesta:
     * {
     *   "mensaje": "Pago confirmado",
     *   "compra": { ... },
     *   "envio": { ... }
     * }
     */
    @PostMapping("/{compraId}/confirmar-pago")
    @Operation(summary = "Paso 2: Confirmar pago exitoso → reserva producto y crea envío")
    public ResponseEntity<?> confirmarPago(
            @PathVariable Integer compraId,
            @RequestBody Map<String, Object> body) {

        try {
            String paymentIntentId = (String) body.get("paymentIntentId");
            String metodoStr       = (String) body.getOrDefault("metodoEntrega", "ENVIO_PAQUETERIA");
            MetodoEntrega metodo   = MetodoEntrega.valueOf(metodoStr);

            String nombreDest  = (String) body.get("nombreDestinatario");
            String direccion   = (String) body.get("direccion");
            String ciudad      = (String) body.get("ciudad");
            String cp          = (String) body.get("codigoPostal");
            String pais        = (String) body.getOrDefault("pais", "España");
            String telefono    = (String) body.get("telefono");
            Double precioEnvio = body.get("precioEnvio") != null
                                 ? Double.valueOf(body.get("precioEnvio").toString()) : 0.0;

            Compra compra = compraService.confirmarPago(
                compraId, paymentIntentId, metodo,
                nombreDest, direccion, ciudad, cp, pais, telefono, precioEnvio);

            return ResponseEntity.ok(Map.of(
                "mensaje", "✅ Pago confirmado. El vendedor preparará tu pedido.",
                "compraId", compra.getId(),
                "estado",   compra.getEstado(),
                "metodoEntrega", compra.getMetodoEntrega()
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al confirmar pago: " + e.getMessage()));
        }
    }

    // ── CANCELAR ─────────────────────────────────────────────────────────

    @PostMapping("/{compraId}/cancelar")
    @Operation(summary = "Cancelar compra (con reembolso automático si ya fue pagada)")
    public ResponseEntity<?> cancelar(@PathVariable Integer compraId) {
        try {
            Compra cancelada = compraService.cancelar(compraId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Compra cancelada correctamente",
                "estado",  cancelada.getEstado()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}