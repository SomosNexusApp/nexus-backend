package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.*;
import com.nexus.repository.*;
import com.nexus.controller.ChatWebSocketController;

// servicio que gestiona todo el ciclo de vida de una compra:
// creacion -> pago -> envio -> notificaciones -> mensajes automaticos en chat
@Service
public class CompraService {

    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private EnvioService envioService;
    @Autowired
    // el servicio de notificaciones para avisar a comprador y vendedor
    private NotificacionService notificacionService;
    @Autowired
    private EmailService emailService;
    @Autowired
    // usamos el websocket para mandar mensajes automaticos al chat de la compra
    private ChatWebSocketController chatWebSocketController;

    // metodos basicos de consulta
    public List<Compra> findAll() {
        return compraRepository.findAll();
    }

    public Optional<Compra> findById(Integer id) {
        return compraRepository.findById(id);
    }

    public List<Compra> findHistorialUsuario(Integer usuarioId) {
        return compraRepository.findByCompradorIdOrderByFechaCompraDesc(usuarioId);
    }

    public List<Compra> findMisVentas(Integer usuarioId) {
        return compraRepository.findByVendedorId(usuarioId);
    }

    /**
     * Confirma el pago tras el exito de Stripe.
     * Este es el metodo principal del servicio: reserva el producto, crea el envio,
     * manda notificaciones y emails, y publica mensajes automaticos en el chat.
     *
     * OJO: este metodo NUNCA se llama desde el webhook de Stripe, solo desde el frontend
     * despues de que el usuario termina el checkout. Ver CompraController para mas detalles.
     */
    @Transactional
    public Compra confirmarPago(Integer compraId, String paymentIntentId,
            MetodoEntrega metodoEntrega,
            String nombreDest, String direccion,
            String ciudad, String cp, String pais,
            String telefonoDest, Double precioEnvio,
            Double pesoKg) {

        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada: " + compraId));

        // verificamos que la compra sigue en estado PENDIENTE para evitar confirmar dos veces
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalStateException("La compra ya fue procesada (estado: " + compra.getEstado() + ")");
        }

        Producto producto = compra.getProducto();
        // comprobamos que el producto sigue disponible en el momento de confirmar el pago
        // puede que alguien mas lo haya comprado mientras el primer usuario pagaba
        if (producto.getEstadoProducto() != EstadoProducto.DISPONIBLE) {
            throw new IllegalStateException("El producto ya no está disponible");
        }

        // actualizamos el estado de la compra con los datos del pago
        compra.setEstado(EstadoCompra.PAGADO);
        compra.setStripePaymentIntentId(paymentIntentId);
        compra.setMetodoEntrega(metodoEntrega);
        compra.setFechaPago(LocalDateTime.now());

        // Marcamos el producto como VENDIDO para que ya no aparezca disponible en la plataforma
        producto.setEstadoProducto(EstadoProducto.VENDIDO);
        productoRepository.save(producto);

        Compra guardada = compraRepository.save(compra);

        // creamos el envio con todos los datos necesarios (siempre con Correos)
        envioService.crearEnvio(guardada, metodoEntrega,
                nombreDest, direccion, ciudad, cp, pais, telefonoDest, precioEnvio,
                pesoKg);

        // notificamos tanto al comprador como al vendedor
        Producto p = guardada.getProducto();
        String titulo = p.getTitulo();
        Actor vendedor = p.getPublicador();
        Actor comprador = guardada.getComprador();
        notificacionService.notificarNuevaCompraVendedor(vendedor.getId(), titulo, guardada.getId(), true);
        notificacionService.notificarCompraConfirmadaComprador(comprador.getId(), titulo, guardada.getId());
        // mandamos email al comprador y al vendedor si tienen email configurado
        if (comprador.getEmail() != null) {
            emailService.enviarConfirmacionCompra(comprador.getEmail(), titulo, guardada.getPrecioFinal());
            emailService.enviarResumenPagoComprador(
                    comprador.getEmail(),
                    guardada.getId(),
                    titulo,
                    guardada.getPrecioFinal(),
                    producto.getPrecio(),
                    guardada.getCostoEnvio(),
                    guardada.getComisionNexus());
        }
        if (vendedor.getEmail() != null) {
            // usamos el nombre si lo tiene, si no el username
            String nombreC = comprador.getNombre() != null && !comprador.getNombre().isBlank()
                    ? comprador.getNombre() : comprador.getUser();
            emailService.enviarNuevaVentaVendedor(vendedor.getEmail(), titulo, guardada.getId(), nombreC);
        }
        // enviamos mensajes automaticos al chat de la transaccion
        // si falla (ej: no hay chat abierto) no rompemos la compra, lo ignoramos
        try {
            chatWebSocketController.publicarMensajeSistema(
                    guardada.getProducto().getId(),
                    vendedor.getId(),
                    comprador.getId(),
                    "✅ Han comprado tu producto. Pago confirmado de «" + titulo
                            + "». Revisa la guía de envío para continuar.",
                    true, false);
            chatWebSocketController.publicarMensajeSistema(
                    guardada.getProducto().getId(),
                    comprador.getId(),
                    vendedor.getId(),
                    "✅ Compra confirmada de «" + titulo
                            + "». Te iremos avisando en este chat con cada actualización del envío.",
                    true, false);
        } catch (Exception ignored) {
        }

        return guardada;
    }

    // calcula la comision que cobra Nexus por cada transaccion
    // hay tres tramos segun el precio: menos de 20€, entre 20 y 100€, y mas de 100€
    // el maximo precio permitido es 1000 EUR (limite de la plataforma)
    public Double calcularComisionNexus(Double precio) {
        if (precio == null || precio <= 0)
            return 0.0;
        if (precio > 1000.0)
            throw new IllegalArgumentException("El precio máximo permitido para compras en Nexus es de 1000 EUR");

        if (precio < 20.0)
            return 1.60; // comision minima para compras baratas
        if (precio < 100.0)
            return 3.60;
        return 5.60; // comision maxima (para precios de 100 a 1000 EUR)
    }

    /**
     * Cancela una compra pendiente de pago o pendiente de envío.
     * Si ya fue pagada, genera reembolso automático.
     */
    @Transactional
    public Compra cancelar(Integer compraId) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));

        if (compra.getEstado() == EstadoCompra.COMPLETADA || compra.getEstado() == EstadoCompra.REEMBOLSADA) {
            throw new IllegalStateException("No se puede cancelar una compra ya completada o reembolsada");
        }

        // Si ya pagó, procesar reembolso
        if (compra.getEstado() == EstadoCompra.PAGADO || compra.getEstado() == EstadoCompra.ENVIADO) {
            envioService.procesarReembolso(compraId);
        }

        compra.setEstado(EstadoCompra.CANCELADA);
        compra.getProducto().setEstadoProducto(EstadoProducto.DISPONIBLE);
        productoRepository.save(compra.getProducto());

        return compraRepository.save(compra);
    }

    /**
     * Webhook Stripe: no marca la compra como pagada aquí. El flujo completo (envío, notificaciones,
     * escrow) solo ocurre en {@link #confirmarPago} llamado por el cliente tras el pago.
     * Marcar PAGADO solo desde el webhook dejaba la compra sin envío y bloqueaba confirmar-pago.
     */
    @Transactional
    public void confirmarPagoPorStripeId(String stripeId) {
        // Reservado para diagnóstico o reconciliación futura; la fuente de verdad es POST /confirmar-pago.
    }
}