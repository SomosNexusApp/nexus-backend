package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.*;
import com.nexus.repository.*;
import com.nexus.controller.ChatWebSocketController;

@Service
public class DevolucionService {

    @Autowired
    private DevolucionRepository devolucionRepository;
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private EnvioService envioService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ChatWebSocketController chatWebSocketController;

    /**
     * El comprador solicita una devolución.
     * Solo permitida si el estado del envío es ENTREGADO y han pasado menos de 15
     * días.
     */
    @Transactional
    public Devolucion solicitar(Integer compraId, MotivoDevolucion motivo,
            String descripcion, String direccionEnvio, List<MultipartFile> fotos) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));

        if (compra.getEstado() != EstadoCompra.COMPLETADA && compra.getEstado() != EstadoCompra.ENTREGADO)
            throw new IllegalStateException("Solo se puede solicitar devolución de compras completadas");

        if (devolucionRepository.findByCompraId(compraId).isPresent())
            throw new IllegalStateException("Ya existe una solicitud de devolución para esta compra");

        Devolucion d = new Devolucion();
        d.setCompra(compra);
        d.setMotivo(motivo);
        d.setDescripcion(descripcion);
        d.setDireccionEnvio(direccionEnvio);

        // Subir fotos del problema
        if (fotos != null) {
            for (MultipartFile foto : fotos) {
                String url = storageService.subirImagen(foto);
                if (url != null)
                    d.getFotos().add(url);
            }
        }

        Devolucion guardada = devolucionRepository.save(d);

        // Notificar al vendedor por email y chat
        notificarEnChat(compra, "↩️ El comprador ha solicitado una devolución. Motivo: " + motivo.name());
        String vendedorEmail = compra.getProducto().getPublicador().getEmail();
        emailService.enviarEmail(vendedorEmail, "Solicitud de devolución — Nexus",
                "El comprador ha solicitado devolver el producto <b>" +
                        compra.getProducto().getTitulo() + "</b>. Tienes 48 horas para responder.");

        return guardada;
    }

    /**
     * El vendedor acepta o rechaza la solicitud de devolución.
     */
    @Transactional
    public Devolucion responder(Integer devolucionId, boolean aceptada, String nota) {
        Devolucion d = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));

        if (d.getEstado() != EstadoDevolucion.SOLICITADA)
            throw new IllegalStateException("Esta solicitud ya fue procesada");

        d.setEstado(aceptada ? EstadoDevolucion.ACEPTADA : EstadoDevolucion.RECHAZADA);
        d.setNotaVendedor(nota);
        d.setFechaResolucion(LocalDateTime.now());

        Devolucion actualizada = devolucionRepository.save(d);

        String msgComprador = aceptada
                ? "✅ El vendedor ha ACEPTADO tu devolución. Envía el producto de vuelta."
                : "❌ El vendedor ha RECHAZADO tu devolución. Motivo: " + nota;
        notificarEnChat(d.getCompra(), msgComprador);

        // Notificar al comprador por email
        emailService.enviarEmail(d.getCompra().getComprador().getEmail(),
                "Respuesta a tu devolución — Nexus", msgComprador);

        return actualizada;
    }

    /**
     * El comprador confirma que ha enviado el producto de vuelta al vendedor.
     */
    @Transactional
    public Devolucion marcarDevolucionEnviada(Integer devolucionId,
            String transportista, String tracking) {
        Devolucion d = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));

        if (d.getEstado() != EstadoDevolucion.ACEPTADA)
            throw new IllegalStateException("La devolución no está en estado ACEPTADA");

        d.setEstado(EstadoDevolucion.DEVOLUCION_ENVIADA);
        d.setTransportistaDevolucion(transportista);
        d.setTrackingDevolucion(tracking);

        Devolucion actualizada = devolucionRepository.save(d);
        notificarEnChat(d.getCompra(), "📦 El comprador ha enviado el producto de vuelta. Tracking: " + tracking);
        return actualizada;
    }

    /**
     * El vendedor confirma que recibió el producto devuelto.
     * Esto procesa el reembolso automáticamente.
     */
    @Transactional
    public Devolucion confirmarRecepcionDevolucion(Integer devolucionId) {
        Devolucion d = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new IllegalArgumentException("Devolución no encontrada"));

        if (d.getEstado() != EstadoDevolucion.DEVOLUCION_ENVIADA)
            throw new IllegalStateException("La devolución no está en estado DEVOLUCION_ENVIADA");

        d.setEstado(EstadoDevolucion.COMPLETADA);
        d.setFechaResolucion(LocalDateTime.now());
        Devolucion actualizada = devolucionRepository.save(d);

        // Procesar reembolso en Stripe
        envioService.procesarReembolso(d.getCompra().getId());

        // El producto vuelve a estar disponible para la venta
        Producto producto = d.getCompra().getProducto();
        producto.setEstadoProducto(EstadoProducto.DISPONIBLE);
        productoRepository.save(producto);

        notificarEnChat(d.getCompra(), "💸 Devolución completada. Reembolso procesado. ¡Gracias por usar Nexus!");
        return actualizada;
    }

    public List<Devolucion> getDevolucionesComoComprador(Integer id) {
        return devolucionRepository.findByCompradorId(id);
    }

    public List<Devolucion> getDevolucionesComoVendedor(Integer id) {
        return devolucionRepository.findByVendedorId(id);
    }

    public Optional<Devolucion> findById(Integer id) {
        return devolucionRepository.findById(id);
    }

    private void notificarEnChat(Compra compra, String texto) {
        try {
            chatWebSocketController.publicarMensajeSistema(
                    compra.getProducto().getId(),
                    compra.getProducto().getPublicador().getId(),
                    compra.getComprador().getId(), texto);
        } catch (Exception e) {
            System.err.println("⚠️ Error chat devolución: " + e.getMessage());
        }
    }
}