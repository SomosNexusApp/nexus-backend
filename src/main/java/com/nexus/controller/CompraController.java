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
import com.nexus.repository.ActorRepository;
import com.nexus.service.*;
import com.stripe.model.PaymentIntent;

import org.springframework.security.core.userdetails.UserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/compra")
@Tag(name = "Compras", description = "Ciclo completo de compra con pago seguro (escrow)")
public class CompraController {

    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private CompraService compraService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private StripeService stripeService;
    @Autowired
    private ShippingPriceService shippingPriceService;
    @Autowired
    private CarrierApiService carrierApiService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private BloqueoService bloqueoService;

    // ── HISTORIAL Y LISTADOS ───────────────────────────────────────────────

    @GetMapping("/historial/{usuarioId}")
    @Operation(summary = "Historial de compras del usuario (Endpoint legado)")
    public ResponseEntity<List<Compra>> historial(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(compraService.findHistorialUsuario(usuarioId));
    }

    @Autowired
    private ActorRepository actorRepository;

    @GetMapping("/mis-compras")
    @Operation(summary = "Historial de compras del usuario autenticado")
    public ResponseEntity<List<Compra>> misCompras(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        return actorRepository.findByUsername(principal.getUsername())
                .map(actor -> ResponseEntity.ok(compraService.findHistorialUsuario(actor.getId())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @GetMapping("/mis-ventas")
    @Operation(summary = "Historial de ventas del usuario autenticado")
    public ResponseEntity<List<Compra>> misVentas(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();

        return actorRepository.findByUsername(principal.getUsername())
                .map(actor -> ResponseEntity.ok(compraService.findMisVentas(actor.getId())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver detalle de una compra")
    public ResponseEntity<?> findById(@PathVariable Integer id) {
        return compraService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── CONSULTAR PRECIO (sin crear nada) ─────────────────────────────

    @GetMapping("/precio")
    @Operation(summary = "Calcula el coste de envío de un producto sin crear ningún registro")
    public ResponseEntity<?> consultarPrecio(
            @RequestParam Integer productoId,
            @RequestParam(defaultValue = "false") boolean esRecogida,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails principal) {

        return productoService.findById(productoId)
                .map(p -> {
                    // Check for blocking
                    Integer compradorId = null;
                    if (principal != null) {
                        Optional<Actor> actor = actorRepository.findByUsername(principal.getUsername());
                        if (actor.isPresent()) compradorId = actor.get().getId();
                    }

                    Integer vendedorId = p.getPublicador().getId();
                    if (compradorId != null && (bloqueoService.estaBloqueado(compradorId, vendedorId) || bloqueoService.estaBloqueado(vendedorId, compradorId))) {
                        return ResponseEntity.status(403).build();
                    }

                    double peso = (p.getPeso() != null && p.getPeso() > 0) ? p.getPeso() : 0.5;
                    boolean necesitaEnvio = Boolean.TRUE.equals(p.getAdmiteEnvio());
                    double costoEnvio = 0.0;
                    if (necesitaEnvio) {
                        Double api = carrierApiService.getBestPrice(peso, esRecogida);
                        costoEnvio = api != null ? api : shippingPriceService.calculateShippingPrice(peso, esRecogida);
                    }

                    Double precioBase = p.getPrecio();
                    if (compradorId != null) {
                        Double negociado = chatService.getPrecioNegociado(productoId, compradorId);
                        if (negociado != null) precioBase = negociado;
                    }

                    double comision = compraService.calcularComisionNexus(precioBase);
                    double ahorro = shippingPriceService.ahorroRecogida(peso);
                    
                    // Opciones de transportistas
                    java.util.List<java.util.Map<String, Object>> opcionesEnvio = carrierApiService.getAvailableCarriers(peso, esRecogida, costoEnvio);

                    return ResponseEntity.ok(Map.of(
                            "costoEnvio", costoEnvio,
                            "comisionNexus", comision,
                            "pesoKg", peso,
                            "ahorroRecogida", ahorro,
                            "total", precioBase + costoEnvio + comision,
                            "precioProducto", precioBase,
                            "opcionesEnvio", opcionesEnvio));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── PASO 1: INICIAR PAGO ──────────────────────────────────────────────

    @PostMapping("/intent")
    @Operation(summary = "Paso 1: Crear PaymentIntent — el peso lo fija el vendedor al publicar")
    public ResponseEntity<?> crearIntentoPago(
            @RequestParam Integer productoId,
            @RequestParam Integer compradorId,
            @RequestParam TipoEnvio tipoEnvio,
            @RequestParam(defaultValue = "false") boolean esRecogida,
            @RequestParam(required = false) String direccionCompleta,
            @RequestParam(required = false) String puntoRecogidaId,
            @RequestParam(required = false) String transportista) {

        Optional<Producto> p = productoService.findById(productoId);
        Optional<Actor> a = actorRepository.findById(compradorId);

        if (p.isEmpty() || a.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto o Comprador no válidos"));
        }
        if (p.get().getEstadoProducto() != EstadoProducto.DISPONIBLE) {
            return ResponseEntity.badRequest().body(Map.of("error", "El producto ya no está disponible"));
        }
        if (p.get().getPublicador().getId() == compradorId) {
            return ResponseEntity.badRequest().body(Map.of("error", "No puedes comprar tu propio producto"));
        }

        // --- BLOQUEO CHECK ---
        Integer vendedorId = p.get().getPublicador().getId();
        if (bloqueoService.estaBloqueado(compradorId, vendedorId) || bloqueoService.estaBloqueado(vendedorId, compradorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Transacción no permitida: Usuario bloqueado"));
        }
        // ---------------------

        try {
            Double precioProducto = p.get().getPrecio();
            
            // Check for negotiated price
            Double negociado = chatService.getPrecioNegociado(productoId, compradorId);
            if (negociado != null) {
                precioProducto = negociado;
            }

            Double comisionNexus = compraService.calcularComisionNexus(precioProducto);

            // Peso definido por el VENDEDOR al publicar. Si no lo configuró → 0,5 kg (tier
            // más barato).
            double pesoFinal = (p.get().getPeso() != null && p.get().getPeso() > 0)
                    ? p.get().getPeso()
                    : 0.5;

            // Precio de envío: API real primero, tabla de fallback si no hay credenciales
            double costoEnvio = 0.0;
            boolean necesitaEnvio = tipoEnvio != TipoEnvio.RECOGIDA_PERSONAL
                    && Boolean.TRUE.equals(p.get().getAdmiteEnvio());
            
            if (necesitaEnvio) {
                double baseFallback = shippingPriceService.calculateShippingPrice(pesoFinal, esRecogida);
                if (transportista != null && !transportista.isBlank()) {
                    costoEnvio = carrierApiService.getPriceForCarrier(transportista, pesoFinal, esRecogida, baseFallback);
                } else {
                    Double precioApi = carrierApiService.getBestPrice(pesoFinal, esRecogida);
                    costoEnvio = (precioApi != null) ? precioApi : baseFallback;
                }
            }

            double totalCobrar = precioProducto + costoEnvio + comisionNexus;
            String idempotencyKey = "intent_" + compradorId + "_" + productoId + "_" + System.currentTimeMillis();

            PaymentIntent intent = stripeService.crearIntentoPago(
                    totalCobrar, "Nexus: " + p.get().getTitulo(), idempotencyKey, a.get().getStripeCustomerId());

            // Compra PENDIENTE
            Compra compra = new Compra();
            compra.setComprador(a.get());
            compra.setProducto(p.get());
            compra.setFechaCompra(LocalDateTime.now());
            compra.setEstado(EstadoCompra.PENDIENTE);
            compra.setPrecioFinal(totalCobrar);
            compra.setTipoEnvio(tipoEnvio);
            compra.setCostoEnvio(costoEnvio);
            compra.setComisionNexus(comisionNexus);
            compra.setTransportista(transportista != null ? transportista : "ESTANDAR");
            compra.setStripePaymentIntentId(intent.getId());

            if (tipoEnvio == TipoEnvio.DOMICILIO)
                compra.setDireccionCompleta(direccionCompleta);
            else if (tipoEnvio == TipoEnvio.PUNTO_RECOGIDA)
                compra.setPuntoRecogidaId(puntoRecogidaId);
            compraRepository.save(compra);

            // Ahorro que tiene el comprador si elige punto de recogida
            double ahorroRecogida = shippingPriceService.ahorroRecogida(pesoFinal);

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            response.put("compraId", compra.getId());
            response.put("precioProducto", precioProducto);
            response.put("costoEnvio", costoEnvio);
            response.put("comisionNexus", comisionNexus);
            response.put("tipoEnvio", tipoEnvio.name());
            response.put("pesoKg", pesoFinal);
            response.put("esRecogida", esRecogida);
            response.put("ahorroRecogida", ahorroRecogida);
            response.put("total", totalCobrar);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
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
     * "paymentIntentId": "pi_3QxyzABC...",
     * "metodoEntrega": "ENVIO_PAQUETERIA", // o "ENTREGA_EN_PERSONA"
     * "nombreDestinatario": "María García",
     * "direccion": "Calle Mayor 10, 2B",
     * "ciudad": "Madrid",
     * "codigoPostal": "28001",
     * "pais": "España",
     * "telefono": "600123456",
     * "precioEnvio": 4.99
     * }
     *
     * Respuesta:
     * {
     * "mensaje": "Pago confirmado",
     * "compra": { ... },
     * "envio": { ... }
     * }
     */
    @PostMapping("/{compraId}/confirmar-pago")
    @Operation(summary = "Paso 2: Confirmar pago exitoso → reserva producto y crea envío con código QR")
    public ResponseEntity<?> confirmarPago(
            @PathVariable Integer compraId,
            @RequestBody Map<String, Object> body) {

        try {
            String paymentIntentId = (String) body.get("paymentIntentId");
            String metodoStr = (String) body.getOrDefault("metodoEntrega", "ENVIO_PAQUETERIA");
            MetodoEntrega metodo = MetodoEntrega.valueOf(metodoStr);

            String nombreDest = (String) body.get("nombreDestinatario");
            String direccion = (String) body.get("direccion");
            String ciudad = (String) body.get("ciudad");
            String cp = (String) body.get("codigoPostal");
            String pais = (String) body.getOrDefault("pais", "España");
            String telefono = (String) body.get("telefono");
            Double precioEnvio = body.get("precioEnvio") != null
                    ? Double.valueOf(body.get("precioEnvio").toString())
                    : 0.0;

            // Nuevos campos de peso y transportista
            Double pesoKg = body.get("pesoKg") != null
                    ? Double.valueOf(body.get("pesoKg").toString())
                    : 0.5;
            Transportista transportista = Transportista.CORREOS;
            if (body.get("transportista") != null) {
                try {
                    transportista = Transportista.valueOf(body.get("transportista").toString().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            Compra compra = compraService.confirmarPago(
                    compraId, paymentIntentId, metodo,
                    nombreDest, direccion, ciudad, cp, pais, telefono, precioEnvio,
                    pesoKg, transportista);

            // Recuperar el envío para devolver código y QR al frontend
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensaje", "✅ Pago confirmado. El vendedor preparará tu pedido.");
            resp.put("compraId", compra.getId());
            resp.put("estado", compra.getEstado());
            resp.put("metodoEntrega", compra.getMetodoEntrega());
            return ResponseEntity.ok(resp);

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
                    "estado", cancelada.getEstado()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}