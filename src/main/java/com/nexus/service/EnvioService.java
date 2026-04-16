package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private ActorRepository actorRepository;
    @Autowired
    private StripeService stripeService;
    @Autowired
    private ShippingPriceService shippingPriceService;
    @Autowired
    private CarrierApiService carrierApiService;
    @Autowired
    private ChatWebSocketController chatWebSocketController;
    @Autowired
    private ValoracionService valoracionService;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private PuntoRecogidaService puntoRecogidaService;

    @Value("${nexus.envio.plazo-dias:10}")
    private int plazoEnvioDias;

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

        envio.setFechaLimiteEnvio(LocalDateTime.now().plusDays(plazoEnvioDias));
        Envio guardado = envioRepository.save(envio);

        // Notificar en el chat automáticamente
        notificarEnChat(compra, "📦 Pago confirmado. Esperando que el vendedor envíe el producto.");

        Producto prod = compra.getProducto();
        Actor vendedor = prod.getPublicador();
        String urlEnvio = "/compras/" + compra.getId() + "/enviar";
        String ubicacionProd = prod.getUbicacion();
        String puntosHint = puntoRecogidaService.buscarPorCiudadOCp(ubicacionProd).stream()
                .findFirst()
                .map(p -> p.getNombre() + " (" + p.getCiudad() + ")")
                .orElse("oficina de Correos / SEUR / MRW más cercana");
        notificacionService.notificarGuiaEnvioVendedor(
                vendedor.getId(),
                prod.getTitulo(),
                guardado.getCodigoEnvio(),
                ubicacionProd != null ? ubicacionProd + " — ej. " + puntosHint : puntosHint,
                urlEnvio,
                plazoEnvioDias);
        if (vendedor.getEmail() != null) {
            emailService.enviarGuiaEnvioConQrVendedor(
                    vendedor.getEmail(),
                    prod.getTitulo(),
                    compra.getId(),
                    guardado.getCodigoEnvio(),
                    guardado.getQrBase64(),
                    guardado.getTransportista(),
                    guardado.getCiudad(),
                    plazoEnvioDias);
        }

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

        String trackingFinal = numeroSeguimiento;
        if (trackingFinal == null || trackingFinal.isBlank()) {
            throw new IllegalStateException("Debes introducir el número de seguimiento real del transportista.");
        }

        envio.setEstado(EstadoEnvio.ENVIADO);
        String transportistaFinal = (transportista != null && !transportista.isBlank())
                ? transportista
                : (envio.getTransportistaEnum() != null ? envio.getTransportistaEnum().name() : "CORREOS");
        envio.setTransportista(transportistaFinal);
        envio.setNumeroSeguimiento(trackingFinal);
        envio.setUrlSeguimiento((urlSeguimiento != null && !urlSeguimiento.isBlank())
                ? urlSeguimiento
                : buildTrackingUrl(transportistaFinal, trackingFinal));
        envio.setFechaEnvio(LocalDateTime.now());
        envio.setFechaEstimadaEntrega(fechaEstimadaEntrega);

        // Actualizar estado de la compra
        Compra compra = envio.getCompra();
        compra.setEstado(EstadoCompra.ENVIADO);
        compra.setFechaEnvio(LocalDateTime.now());
        compraRepository.save(compra);

        Envio actualizado = envioRepository.save(envio);

        String msg = String.format("🚚 ¡Pedido enviado! Transportista: %s. Nº seguimiento: %s",
                transportistaFinal, trackingFinal);
        notificarEnChat(compra, msg);

        Producto prod = compra.getProducto();
        Actor comprador = compra.getComprador();
        Actor seller = prod.getPublicador();
        notificacionService.notificarEnvio(comprador.getId(), prod.getTitulo());
        notificacionService.notificarSeguimientoVendedor(seller.getId(), prod.getTitulo(), trackingFinal,
                "/compras/" + compra.getId() + "/enviar");
        if (comprador.getEmail() != null) {
            emailService.enviarNotificacionEnvio(comprador.getEmail(), prod.getTitulo(),
                    trackingFinal, transportistaFinal);
            emailService.enviarActualizacionTracking(comprador.getEmail(), prod.getTitulo(), trackingFinal,
                    "ENVIADO", actualizado.getUrlSeguimiento());
        }
        if (seller.getEmail() != null) {
            emailService.enviarActualizacionTracking(seller.getEmail(), prod.getTitulo(), trackingFinal,
                    "ENVIADO", actualizado.getUrlSeguimiento());
        }

        return actualizado;
    }

    /**
     * El comprador confirma que recibió el producto.
     * Esto libera los fondos al vendedor en Stripe y completa la transacción.
     */
    @Transactional
    public Envio confirmarEntrega(Integer envioId, Integer valoracion, String comentario) {
        Envio envio = findByIdOrThrow(envioId);

        if (envio.getEstado() != EstadoEnvio.ENVIADO
                && envio.getEstado() != EstadoEnvio.EN_TRANSITO
                && envio.getEstado() != EstadoEnvio.EN_REPARTO) {
            throw new IllegalStateException("El pedido no está en estado ENVIADO, EN_TRANSITO o EN_REPARTO");
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
            notificacionService.notificarNuevaValoracion(
                    compra.getProducto().getPublicador().getId(), valoracion);
        }

        notificarEnChat(compra, "✅ Entrega confirmada. Fondos liberados al vendedor. ¡Gracias por usar Nexus!");
        notificacionService.crear(
                compra.getComprador().getId(),
                TipoNotificacion.SISTEMA,
                "¡Tu pedido fue entregado!",
                "Deja una reseña al vendedor para cerrar tu experiencia en Nexus.",
                "/perfil?tab=compras&compraId=" + compra.getId(),
                true,
                "{\"compraId\":" + compra.getId() + ",\"accion\":\"solicitar_resena\"}");

        if (compra.getComprador().getEmail() != null) {
            emailService.enviarEntregaConfirmada(compra.getComprador().getEmail(), compra.getProducto().getTitulo(), true);
        }
        if (compra.getProducto().getPublicador().getEmail() != null) {
            emailService.enviarEntregaConfirmada(compra.getProducto().getPublicador().getEmail(),
                    compra.getProducto().getTitulo(), false);
        }

        return actualizado;
    }

    @Transactional
    public Envio registrarEventoTransportista(String codigoEnvio, EstadoEnvio estado) {
        Envio envio = envioRepository.findByCodigoEnvio(codigoEnvio)
                .orElseThrow(() -> new IllegalArgumentException("Código de envío no encontrado"));
        return avanzarEstadoTracking(envio, estado, "Evento recibido del transportista");
    }

    @Transactional
    public Optional<Envio> refrescarTrackingEnvio(Integer envioId) {
        Envio envio = findByIdOrThrow(envioId);
        return consultarYActualizarTracking(envio);
    }

    @Transactional
    public int refrescarTrackingPendientes() {
        List<Envio> envios = envioRepository.findByEstadoIn(List.of(
                EstadoEnvio.ENVIADO, EstadoEnvio.EN_TRANSITO, EstadoEnvio.EN_REPARTO));
        int cambios = 0;
        for (Envio envio : envios) {
            Optional<Envio> actualizado = consultarYActualizarTracking(envio);
            if (actualizado.isPresent()) {
                cambios++;
            }
        }
        return cambios;
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
        if (compra.getComprador().getEmail() != null) {
            emailService.enviarEntregaConfirmada(compra.getComprador().getEmail(), compra.getProducto().getTitulo(), true);
        }
        if (compra.getProducto().getPublicador().getEmail() != null) {
            emailService.enviarEntregaConfirmada(compra.getProducto().getPublicador().getEmail(), compra.getProducto().getTitulo(), false);
        }

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
        if (compra.getComprador().getEmail() != null) {
            emailService.enviarEmailHtml(compra.getComprador().getEmail(), "Disputa abierta en tu pedido",
                    "<h2>Hemos recibido tu disputa</h2><p>Tu incidencia para el pedido de <b>"
                            + compra.getProducto().getTitulo() + "</b> ya está en revisión.</p><p>Motivo: " + motivo + "</p>");
        }
        if (compra.getProducto().getPublicador().getEmail() != null) {
            emailService.enviarEmailHtml(compra.getProducto().getPublicador().getEmail(), "Se abrió una disputa en una venta",
                    "<h2>Disputa abierta por el comprador</h2><p>El pedido de <b>"
                            + compra.getProducto().getTitulo() + "</b> está en estado de incidencia.</p><p>Motivo: " + motivo + "</p>");
        }

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
        envioRepository.findByCompraId(compraId).ifPresent(e -> {
            e.setEstado(EstadoEnvio.CANCELADO);
            envioRepository.save(e);
        });
        compraRepository.save(compra);
        notificarEnChat(compra, "💸 Reembolso procesado. El dinero volverá a tu cuenta en 3-5 días hábiles.");
        if (compra.getComprador().getEmail() != null) {
            emailService.enviarAdminReembolsoComprador(compra.getComprador().getEmail(), compra.getId(),
                    compra.getProducto().getTitulo(), "Reembolso procesado");
        }
        if (compra.getProducto().getPublicador().getEmail() != null) {
            emailService.enviarAdminReembolsoVendedor(compra.getProducto().getPublicador().getEmail(), compra.getId(),
                    compra.getProducto().getTitulo(), "Reembolso procesado");
        }
    }

    /**
     * Vendedor no envió a tiempo: reembolso al comprador + notificaciones y emails.
     */
    @Transactional
    public void procesarReembolsoPorPlazoEnvio(Integer compraId) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
        if (compra.getEstado() != EstadoCompra.PAGADO) {
            return;
        }
        try {
            if (compra.getStripePaymentIntentId() != null) {
                stripeService.reembolsar(compra.getStripePaymentIntentId());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar reembolso en Stripe: " + e.getMessage());
        }
        compra.setEstado(EstadoCompra.REEMBOLSADA);
        compra.getProducto().setEstadoProducto(EstadoProducto.DISPONIBLE);
        envioRepository.findByCompraId(compraId).ifPresent(e -> {
            e.setEstado(EstadoEnvio.CANCELADO);
            envioRepository.save(e);
        });
        compraRepository.save(compra);

        Producto prod = compra.getProducto();
        String titulo = prod.getTitulo();
        Actor comprador = compra.getComprador();
        Actor vendedor = prod.getPublicador();
        notificacionService.notificarReembolsoAutomatico(comprador.getId(), "Compra reembolsada",
                "El vendedor no envió «" + titulo + "» a tiempo. Se ha reembolsado el importe.",
                "/perfil?tab=compras");
        notificacionService.notificarAccionAdmin(vendedor.getId(), "Pedido cancelado por plazo de envío",
                "Se reembolsó al comprador de «" + titulo + "» por no enviar en el plazo.",
                "/perfil?tab=compras");
        if (comprador.getEmail() != null) {
            emailService.enviarReembolsoPlazoVencidoComprador(comprador.getEmail(), titulo);
        }
        notificarEnChat(compra, "💸 Compra reembolsada: plazo de envío superado sin que el vendedor enviara el paquete.");
    }

    /**
     * Tarea programada: envíos pendientes cuya fecha límite ya pasó.
     */
    @Transactional
    public int procesarEnviosPendientesPlazoVencido() {
        List<Envio> vencidos = envioRepository.findByEstadoAndFechaLimiteEnvioBefore(
                EstadoEnvio.PENDIENTE_ENVIO, LocalDateTime.now());
        int n = 0;
        for (Envio e : vencidos) {
            try {
                if (e.getCompra() != null && e.getCompra().getEstado() == EstadoCompra.PAGADO) {
                    procesarReembolsoPorPlazoEnvio(e.getCompra().getId());
                    n++;
                }
            } catch (Exception ex) {
                System.err.println("⚠️ Reembolso plazo envío compra " + e.getCompra().getId() + ": " + ex.getMessage());
            }
        }
        return n;
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
        
        Producto p = compra.getProducto();
        p.setEstadoProducto(EstadoProducto.VENDIDO);
        p.setFechaVenta(LocalDateTime.now());
        
        compraRepository.save(compra);

        // Aumentar reputación del vendedor (si es Usuario particular)
        if (compra.getProducto().getPublicador() instanceof Usuario vendedor) {
            vendedor.setReputacion(vendedor.getReputacion() + 1);
            actorRepository.save(vendedor);
        }
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

    private Optional<Envio> consultarYActualizarTracking(Envio envio) {
        CarrierApiService.TrackingResult result = carrierApiService.consultarTracking(
                envio.getTransportista(), envio.getNumeroSeguimiento(), envio.getUrlSeguimiento());
        if (result.getEstado() == null) {
            return Optional.empty();
        }
        return Optional.of(avanzarEstadoTracking(envio, result.getEstado(), result.getDescripcion()));
    }

    private Envio avanzarEstadoTracking(Envio envio, EstadoEnvio nuevoEstado, String motivo) {
        if (envio.getEstado() == nuevoEstado || !isValidProgression(envio.getEstado(), nuevoEstado)) {
            return envio;
        }

        if (nuevoEstado == EstadoEnvio.ENTREGADO) {
            return confirmarEntrega(envio.getId(), null, null);
        }

        envio.setEstado(nuevoEstado);
        Envio actualizado = envioRepository.save(envio);
        Compra compra = envio.getCompra();
        Producto prod = compra.getProducto();
        Actor comprador = compra.getComprador();
        Actor vendedor = prod.getPublicador();
        String textoEstado = nuevoEstado.name().replace("_", " ");
        notificacionService.crear(comprador.getId(), TipoNotificacion.ENVIO_ACTUALIZADO, "Actualización de envío",
                "Tu pedido de «" + prod.getTitulo() + "» está en " + textoEstado + ".", "/perfil?tab=compras");
        notificacionService.crear(vendedor.getId(), TipoNotificacion.ENVIO_ACTUALIZADO, "Envío en curso",
                "El pedido de «" + prod.getTitulo() + "» pasó a " + textoEstado + ".", "/perfil?tab=ventas");
        notificarEnChat(compra, "📍 Estado de envío actualizado: " + textoEstado + ". " + (motivo != null ? motivo : ""));
        if (comprador.getEmail() != null) {
            emailService.enviarActualizacionTracking(
                    comprador.getEmail(),
                    prod.getTitulo(),
                    envio.getNumeroSeguimiento(),
                    textoEstado,
                    envio.getUrlSeguimiento());
        }
        if (vendedor.getEmail() != null) {
            emailService.enviarActualizacionTracking(
                    vendedor.getEmail(),
                    prod.getTitulo(),
                    envio.getNumeroSeguimiento(),
                    textoEstado,
                    envio.getUrlSeguimiento());
        }
        return actualizado;
    }

    private boolean isValidProgression(EstadoEnvio actual, EstadoEnvio siguiente) {
        if (actual == EstadoEnvio.ENVIADO) {
            return siguiente == EstadoEnvio.EN_TRANSITO || siguiente == EstadoEnvio.EN_REPARTO
                    || siguiente == EstadoEnvio.ENTREGADO;
        }
        if (actual == EstadoEnvio.EN_TRANSITO) {
            return siguiente == EstadoEnvio.EN_REPARTO || siguiente == EstadoEnvio.ENTREGADO;
        }
        return actual == EstadoEnvio.EN_REPARTO && siguiente == EstadoEnvio.ENTREGADO;
    }

    private String buildTrackingUrl(String transportista, String tracking) {
        String t = transportista != null ? transportista.toUpperCase() : "CORREOS";
        if ("SEUR".equals(t)) {
            return "https://www.seur.com/livetracking/pages/seguimiento-online.do?segOnlineIdentificador="
                    + tracking;
        }
        if ("MRW".equals(t)) {
            return "https://www.mrw.es/seguimiento_envios/MRW_tracking.asp?codigo=" + tracking;
        }
        return "https://www.correos.es/es/es/herramientas/localizador/envios/detalle?tracking-number=" + tracking;
    }
}