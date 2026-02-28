package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.*;
import com.nexus.repository.*;

@Service
public class CompraService {

    @Autowired private CompraRepository compraRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioService usuarioService;
    @Autowired private EnvioService envioService;

    public List<Compra> findAll() {
        return compraRepository.findAll();
    }

    public Optional<Compra> findById(Integer id) {
        return compraRepository.findById(id);
    }

    public List<Compra> findHistorialUsuario(Integer usuarioId) {
        return compraRepository.findAll().stream()
                .filter(c -> c.getComprador().getId() == usuarioId)
                .toList();
    }

    /**
     * Confirma el pago (llamado desde CompraController tras éxito de Stripe).
     * Reserva el producto y crea el envío con los datos de entrega.
     *
     * @param compraId          ID de la compra creada en /compra/intent
     * @param paymentIntentId   ID de Stripe para futuras operaciones (reembolso)
     * @param metodoEntrega     ENVIO_PAQUETERIA o ENTREGA_EN_PERSONA
     * @param nombreDest        Nombre del destinatario (solo para paquetería)
     * @param direccion         Dirección de entrega
     * @param ciudad            Ciudad
     * @param cp                Código postal
     * @param pais              País
     * @param telefonoDest      Teléfono del destinatario
     * @param precioEnvio       Coste del envío
     */
    @Transactional
    public Compra confirmarPago(Integer compraId, String paymentIntentId,
                                 MetodoEntrega metodoEntrega,
                                 String nombreDest, String direccion,
                                 String ciudad, String cp, String pais,
                                 String telefonoDest, Double precioEnvio) {

        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada: " + compraId));

        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalStateException("La compra ya fue procesada (estado: " + compra.getEstado() + ")");
        }

        Producto producto = compra.getProducto();
        if (producto.getEstadoProducto() != EstadoProducto.DISPONIBLE) {
            throw new IllegalStateException("El producto ya no está disponible");
        }

        // Actualizar compra
        compra.setEstado(EstadoCompra.PAGADO);
        compra.setStripePaymentIntentId(paymentIntentId);
        compra.setMetodoEntrega(metodoEntrega);
        compra.setFechaPago(LocalDateTime.now());

        // Reservar el producto para que nadie más lo compre
        producto.setEstadoProducto(EstadoProducto.RESERVADO);
        productoRepository.save(producto);

        Compra guardada = compraRepository.save(compra);

        // Crear el envío
        envioService.crearEnvio(guardada, metodoEntrega,
                nombreDest, direccion, ciudad, cp, pais, telefonoDest, precioEnvio);

        return guardada;
    }

    
 // --- NUEVO MÉTODO ---
    public Double calcularComisionNexus(Double precio) {
        if (precio == null || precio <= 0) return 0.0;
        if (precio > 1000.0) throw new IllegalArgumentException("El precio máximo permitido para compras en Nexus es de 1000 EUR");
        
        if (precio < 20.0)  return 1.60;
        if (precio < 100.0) return 3.60;
        return 5.60;  // maximo 1000 EUR de precio permitido
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
}