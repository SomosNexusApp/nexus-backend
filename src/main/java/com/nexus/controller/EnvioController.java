package com.nexus.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Envio;
import com.nexus.entity.EstadoEnvio;
import com.nexus.service.EnvioService;
import com.nexus.service.PuntoRecogidaService;
import com.nexus.service.ShippingPriceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/envio")
@Tag(name = "Envíos", description = "Gestión del ciclo de envío y entrega con pago seguro")
public class EnvioController {

    @Autowired
    private EnvioService envioService;
    @Autowired
    private ShippingPriceService shippingPriceService;
    @Autowired
    private PuntoRecogidaService puntoRecogidaService;
    @Autowired
    private com.nexus.repository.EnvioRepository envioRepository;

    @org.springframework.beans.factory.annotation.Value("${nexus.shipping.carrier-webhook-secret:}")
    private String carrierWebhookSecret;

    // ── PRECIO DE ENVÍO ─────────────────────────────────────────────────

    /**
     * Cálculo de precio de envío sin crear compra (para el frontend en tiempo
     * real).
     * GET /envio/shipping-price?pesoKg=1.5
     */
    /**
     * Oficinas / puntos de entrega orientativos según ciudad o CP (España).
     * GET /envio/puntos-recogida?ciudad=Madrid
     */
    @GetMapping("/puntos-recogida")
    @Operation(summary = "Listado orientativo de puntos de recogida por ciudad")
    public ResponseEntity<?> puntosRecogida(@RequestParam(required = false) String ciudad) {
        return ResponseEntity.ok(Map.of("puntos", puntoRecogidaService.buscarPorCiudadOCp(ciudad)));
    }

    @GetMapping("/shipping-price")
    @Operation(summary = "Calcular precio de envío según peso (sin margen)")
    public ResponseEntity<?> calcularPrecio(@RequestParam double pesoKg) {
        try {
            double precio = shippingPriceService.calculateShippingPrice(pesoKg);
            return ResponseEntity.ok(Map.of("pesoKg", pesoKg, "precio", precio));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * El vendedor puede buscar un envío por código SHIP-XXXXXXXX.
     * GET /envio/codigo/SHIP-A7F4K92D
     */
    @GetMapping("/codigo/{codigoEnvio}")
    @Operation(summary = "Buscar envío por código de envío")
    public ResponseEntity<?> porCodigo(@PathVariable String codigoEnvio) {
        return envioRepository.findByCodigoEnvio(codigoEnvio)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── CONSULTAS ─────────────────────────────────────────────────────────

    @GetMapping("/compra/{compraId}")
    @Operation(summary = "Ver envío de una compra específica")
    public ResponseEntity<?> porCompra(@PathVariable Integer compraId) {
        return envioService.findByCompraId(compraId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/comprador/{usuarioId}")
    @Operation(summary = "Ver todos los pedidos recibidos por el comprador")
    public ResponseEntity<List<Envio>> porComprador(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(envioService.getEnviosComoComprador(usuarioId));
    }

    @GetMapping("/vendedor/{usuarioId}")
    @Operation(summary = "Ver todos los pedidos a enviar por el vendedor")
    public ResponseEntity<List<Envio>> porVendedor(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(envioService.getEnviosComoVendedor(usuarioId));
    }

    // ── VENDEDOR: MARCAR COMO ENVIADO ─────────────────────────────────────

    /**
     * El vendedor introduce el número de seguimiento y marca el pedido como
     * enviado.
     *
     * Angular POST /envio/{id}/enviar con body:
     * {
     * "transportista": "Correos",
     * "numeroSeguimiento": "1Z999AA10123456784",
     * "urlSeguimiento": "https://www.correos.es/seguimiento/...",
     * "diasEntregaEstimados": 3
     * }
     */
    @PostMapping("/{envioId}/enviar")
    @Operation(summary = "Vendedor marca el pedido como enviado con número de seguimiento")
    public ResponseEntity<?> marcarEnviado(
            @PathVariable Integer envioId,
            @RequestBody Map<String, Object> body) {

        try {
            String transportista = (String) body.get("transportista");
            String tracking = (String) body.get("numeroSeguimiento");
            String urlTracking = (String) body.get("urlSeguimiento");
            Integer diasEstimados = body.get("diasEntregaEstimados") != null
                    ? Integer.valueOf(body.get("diasEntregaEstimados").toString())
                    : 5;

            LocalDateTime fechaEstimada = LocalDateTime.now().plusDays(diasEstimados);

            Envio actualizado = envioService.marcarComoEnviado(
                    envioId, transportista, tracking, urlTracking, fechaEstimada);

            return ResponseEntity.ok(actualizado);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{envioId}/refresh-tracking")
    @Operation(summary = "Forzar actualización del tracking del transportista")
    public ResponseEntity<?> refreshTracking(@PathVariable Integer envioId) {
        try {
            return envioService.refrescarTrackingEnvio(envioId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.ok(Map.of("mensaje", "Sin cambios de tracking")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/correos/evento")
    @Operation(summary = "Webhook de transportista para actualizar estado del envío")
    public ResponseEntity<?> eventoTransportista(
            @RequestHeader(value = "X-Carrier-Secret", required = false) String secret,
            @RequestBody Map<String, String> body) {
        try {
            if (carrierWebhookSecret != null && !carrierWebhookSecret.isBlank()
                    && !carrierWebhookSecret.equals(secret)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Secret inválido"));
            }
            String codigoEnvio = body.get("codigoEnvio");
            String estadoRaw = body.get("estado");
            if (codigoEnvio == null || estadoRaw == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "codigoEnvio y estado son obligatorios"));
            }
            EstadoEnvio estado = EstadoEnvio.valueOf(estadoRaw.toUpperCase());
            Envio actualizado = envioService.registrarEventoCorreos(codigoEnvio, estado);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── COMPRADOR: CONFIRMAR RECEPCIÓN ────────────────────────────────────

    /**
     * El comprador confirma que recibió el producto.
     * Esto libera los fondos al vendedor → la transacción se completa.
     *
     * Angular POST /envio/{id}/confirmar con body:
     * { "valoracion": 5, "comentario": "Producto en perfecto estado, envío rápido"
     * }
     */
    @PostMapping("/{envioId}/confirmar")
    @Operation(summary = "Comprador confirma la recepción → fondos liberados al vendedor")
    public ResponseEntity<?> confirmarEntrega(
            @PathVariable Integer envioId,
            @RequestBody(required = false) Map<String, Object> body) {

        try {
            Integer valoracion = null;
            String comentario = null;

            if (body != null) {
                valoracion = body.get("valoracion") != null
                        ? Integer.valueOf(body.get("valoracion").toString())
                        : null;
                comentario = (String) body.get("comentario");
            }

            Envio confirmado = envioService.confirmarEntrega(envioId, valoracion, comentario);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "¡Entrega confirmada! Los fondos han sido liberados al vendedor.",
                    "envio", confirmado));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── VENTA EN PERSONA: CONFIRMAR ENTREGA ───────────────────────────────

    @PostMapping("/{envioId}/confirmar-en-persona")
    @Operation(summary = "Confirmar entrega en persona → completa la transacción")
    public ResponseEntity<?> confirmarEnPersona(@PathVariable Integer envioId) {
        try {
            Envio confirmado = envioService.confirmarEntregaEnPersona(envioId);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "✅ Entrega en persona confirmada. ¡Transacción completada!",
                    "envio", confirmado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DISPUTAS ──────────────────────────────────────────────────────────

    /**
     * El comprador abre una disputa.
     * Los fondos se quedan en escrow hasta que el admin resuelva.
     *
     * Body: { "motivo": "El producto no llegó" }
     */
    @PostMapping("/{envioId}/disputa")
    @Operation(summary = "Abrir disputa — los fondos quedan retenidos hasta resolución")
    public ResponseEntity<?> abrirDisputa(
            @PathVariable Integer envioId,
            @RequestBody Map<String, String> body) {

        try {
            String motivo = body.getOrDefault("motivo", "Sin motivo especificado");
            Envio actualizado = envioService.abrirDisputa(envioId, motivo);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Disputa abierta. El equipo de Nexus revisará el caso en 24-48h.",
                    "envio", actualizado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN: REEMBOLSO ──────────────────────────────────────────────────

    @PostMapping("/reembolsar/{compraId}")
    @Operation(summary = "Admin: procesar reembolso al comprador")
    public ResponseEntity<?> reembolsar(@PathVariable Integer compraId) {
        try {
            envioService.procesarReembolso(compraId);
            return ResponseEntity.ok(Map.of("mensaje", "Reembolso procesado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}