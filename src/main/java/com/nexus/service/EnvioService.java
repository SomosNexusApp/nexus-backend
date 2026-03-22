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

@Service
public class EnvioService {

    @Autowired
    private EnvioRepository envioRepository;
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private StripeService stripeService;
    @Autowired
    private ShippingPriceService shippingPriceService;
    @Autowired
    private ChatWebSocketController chatWebSocketController;
    @Autowired
    private ValoracionService valoracionService;

    /**
     * Crea el envío justo después de confirmar el pago.
     * Solo se llama desde CompraService.confirmarPago().
     */
    @Transactional
    public Envio crearEnvio(Compra compra, MetodoEntrega metodo,
            String nombreDestinatario, String direccion,
            String ciudad, String cp, String pais,
            String telefono, Double precioEnvio,
            Double pesoKg, Transportista transportista) {
        Envio envio = new Envio();
        envio.setCompra(compra);
        envio.setMetodoEntrega(metodo);
        envio.setEstado(EstadoEnvio.PENDIENTE_ENVIO);
        envio.setStripePaymentIntentId(compra.getStripePaymentIntentId());
        envio.setPesoKg(pesoKg);
        envio.setTransportistaEnum(transportista);
        if (transportista != null) {
            envio.setTransportista(transportista.name()); // legado / compatibilidad
        }

        // Generar código único y QR
        String codigo = shippingPriceService.generateShippingCode();
        // Asegurar unicidad (reintentar en caso de colisión muy improbable)
        while (envioRepository.findByCodigoEnvio(codigo).isPresent()) {
            codigo = shippingPriceService.generateShippingCode();
        }
        envio.setCodigoEnvio(codigo);
        envio.setQrBase64(shippingPriceService.generateQrBase64(codigo));

        if (MetodoEntrega.ENVIO_PAQUETERIA.equals(metodo)) {
            envio.setNombreDestinatario(nombreDestinatario);
            envio.setDireccion(direccion);
            envio.setCiudad(ciudad);
            envio.setCodigoPostal(cp);
            envio.setPais(pais);
            envio.setTelefono(telefono);
            envio.setPrecioEnvio(precioEnvio != null ? precioEnvio : 0.0);
        }

        Envio guardado = envioRepository.save(envio);

        // Notificar en el chat automáticamente
        notificarEnChat(compra, "📦 Pago confirmado. Esperando que el vendedor envíe el producto.");

        return guardado;
    }

    /**
     * El vendedor marca el producto como enviado e introduce el número de
     * seguimiento.
     */
    @Transactional
    public Envio marcarComoEnviado(Integer envioId, String transportista,
            String numeroSeguimiento, String urlSeguimiento,
            LocalDateTime fechaEstimadaEntrega) {
        Envio envio = findByIdOrThrow(envioId);

        if (envio.getEstado() != EstadoEnvio.PENDIENTE_ENVIO) {
            throw new IllegalStateException("El envío no está en estado PENDIENTE_ENVIO");
        }

        envio.setEstado(EstadoEnvio.ENVIADO);
        envio.setTransportista(transportista);
        envio.setNumeroSeguimiento(numeroSeguimiento);
        envio.setUrlSeguimiento(urlSeguimiento);
        envio.setFechaEnvio(LocalDateTime.now());
        envio.setFechaEstimadaEntrega(fechaEstimadaEntrega);

        // Actualizar estado de la compra
        Compra compra = envio.getCompra();
        compra.setEstado(EstadoCompra.ENVIADO);
        compra.setFechaEnvio(LocalDateTime.now());
        compraRepository.save(compra);

        Envio actualizado = envioRepository.save(envio);

        String msg = String.format("🚚 ¡Pedido enviado! Transportista: %s. Nº seguimiento: %s",
                transportista, numeroSeguimiento);
        notificarEnChat(compra, msg);

        return actualizado;
    }

    /**
     * El comprador confirma que recibió el producto.
     * Esto libera los fondos al vendedor en Stripe y completa la transacción.
     */
    @Transactional
    public Envio confirmarEntrega(Integer envioId, Integer valoracion, String comentario) {
        Envio envio = findByIdOrThrow(envioId);

        if (envio.getEstado() != EstadoEnvio.ENVIADO && envio.getEstado() != EstadoEnvio.EN_TRANSITO) {
            throw new IllegalStateException("El pedido no está en estado ENVIADO o EN_TRANSITO");
        }

        envio.setEstado(EstadoEnvio.ENTREGADO);
        envio.setFechaConfirmacionEntrega(LocalDateTime.now());

        if (valoracion != null && valoracion >= 1 && valoracion <= 5) {
            envio.setValoracionVendedor(valoracion);
            envio.setComentarioValoracion(comentario);
        }

        Compra compra = envio.getCompra();
        compra.setEstado(EstadoCompra.ENTREGADO);
        compra.setFechaEntrega(LocalDateTime.now());
        compraRepository.save(compra);

        Envio actualizado = envioRepository.save(envio);

        // Completar la compra y liberar fondos
        completarCompra(compra);

        // Crear valoración pública si se ha proporcionado
        if (valoracion != null && valoracion >= 1) {
            try {
                valoracionService.valorar(compra.getComprador().getId(), compra.getId(), valoracion, comentario);
            } catch (Exception e) {
                // No bloquear la confirmación si la valoración falla
                System.err.println("⚠️ Error creando valoración automática: " + e.getMessage());
            }
        }

        notificarEnChat(compra, "✅ Entrega confirmada. Fondos liberados al vendedor. ¡Gracias por usar Nexus!");

        return actualizado;
    }

    /**
     * Para ventas en persona: el vendedor confirma que entregó el producto en mano.
     */
    @Transactional
    public Envio confirmarEntregaEnPersona(Integer envioId) {
        Envio envio = findByIdOrThrow(envioId);
        envio.setEstado(EstadoEnvio.ENTREGADO);
        envio.setFechaConfirmacionEntrega(LocalDateTime.now());

        Compra compra = envio.getCompra();
        compra.setEstado(EstadoCompra.ENTREGADO);
        compra.setFechaEntrega(LocalDateTime.now());
        compraRepository.save(compra);

        Envio actualizado = envioRepository.save(envio);
        completarCompra(compra);

        notificarEnChat(compra, "🤝 Entrega en persona confirmada. ¡Transacción completada!");

        return actualizado;
    }

    /**
     * El comprador abre una disputa (producto no llegó, no corresponde a lo
     * anunciado).
     * Los fondos permanecen en escrow hasta resolver.
     */
    @Transactional
    public Envio abrirDisputa(Integer envioId, String motivo) {
        Envio envio = findByIdOrThrow(envioId);
        envio.setEstado(EstadoEnvio.INCIDENCIA);

        Compra compra = envio.getCompra();
        compra.setEstado(EstadoCompra.EN_DISPUTA);
        compraRepository.save(compra);

        envioRepository.save(envio);
        notificarEnChat(compra, "⚠️ Disputa abierta: " + motivo + ". El equipo de Nexus revisará el caso.");

        return envio;
    }

    /**
     * Procesa el reembolso al comprador.
     * Llamado por admin tras resolver disputa o por cancelación.
     */
    @Transactional
    public void procesarReembolso(Integer compraId) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));

        try {
            if (compra.getStripePaymentIntentId() != null) {
                stripeService.reembolsar(compra.getStripePaymentIntentId());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar reembolso en Stripe: " + e.getMessage());
        }

        compra.setEstado(EstadoCompra.REEMBOLSADA);
        compra.getProducto().setEstadoProducto(EstadoProducto.DISPONIBLE); // Volver a disponible
        compraRepository.save(compra);
        notificarEnChat(compra, "💸 Reembolso procesado. El dinero volverá a tu cuenta en 3-5 días hábiles.");
    }

    public Optional<Envio> findByCompraId(Integer compraId) {
        return envioRepository.findByCompraId(compraId);
    }

    public List<Envio> getEnviosComoComprador(Integer usuarioId) {
        return envioRepository.findByCompradorId(usuarioId);
    }

    public List<Envio> getEnviosComoVendedor(Integer usuarioId) {
        return envioRepository.findByVendedorId(usuarioId);
    }

    // ── Privados ──────────────────────────────────────────────────────────

    private Envio findByIdOrThrow(Integer id) {
        return envioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado con id: " + id));
    }

    private void completarCompra(Compra compra) {
        compra.setEstado(EstadoCompra.COMPLETADA);
        compra.setFechaCompletada(LocalDateTime.now());
        compra.getProducto().setEstadoProducto(EstadoProducto.VENDIDO);
        compraRepository.save(compra);

        // Aumentar reputación del vendedor
        Usuario vendedor = (Usuario) compra.getProducto().getPublicador();
        vendedor.setReputacion(vendedor.getReputacion() + 1);
    }

    private void notificarEnChat(Compra compra, String texto) {
        try {
            Integer productoId = compra.getProducto().getId();
            Integer compradorId = compra.getComprador().getId();
            Integer vendedorId = compra.getProducto().getPublicador().getId();
            chatWebSocketController.publicarMensajeSistema(productoId, vendedorId, compradorId, texto);
        } catch (Exception e) {
            // No interrumpir la operación principal si el chat falla
            System.err.println("⚠️ Error notificando en chat: " + e.getMessage());
        }
    }
}