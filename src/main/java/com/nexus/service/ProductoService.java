package com.nexus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.ProductoRepository;
import com.nexus.repository.ProductoSpecification;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private BloqueoService bloqueoService;
    @Autowired
    private SynonymService synonymService;

    // ── Lecturas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Producto> findAll() {
        return productoRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Producto> findDisponibles() {
        return productoRepository.findByEstado(EstadoProducto.DISPONIBLE);
    }

    @Transactional(readOnly = true)
    public Optional<Producto> findById(Integer id) {
        return productoRepository.findById(id);
    }

    /**
     * Búsqueda principal con sinónimos y filtros.
     * "ps5" encontrará "PlayStation 5", "portatil" encontrará "laptop", etc.
     */
    @Transactional(readOnly = true)
    public Page<Producto> buscarConFiltrosPaginado(
            String categoria,
            String tipoOferta,
            Double precioMin,
            Double precioMax,
            String condicion,
            String busqueda,
            String ubicacion,
            Integer vendedorId,
            Integer currentUserId,
            Pageable pageable) {

        // Expandir el término con sinónimos
        List<String> terms = synonymService.expand(busqueda);

        // Si no hay términos de búsqueda ni filtros de texto, terms queda vacío → trae
        // todo
        String categoriaNorm = (categoria != null && !categoria.isBlank()) ? categoria : null;

        Specification<Producto> spec = ProductoSpecification.buscarConFiltros(
                terms.isEmpty() ? null : terms,
                categoriaNorm,
                precioMin,
                precioMax,
                vendedorId,
                bloqueoService.getRelacionesBloqueo(currentUserId));

        return productoRepository.findAll(spec, pageable);
    }

    /** Alias para compatibilidad con código que llame a la firma corta. */
    @Transactional(readOnly = true)
    public Page<Producto> buscarConFiltros(
            String categoria,
            Double precioMin,
            Double precioMax,
            String busqueda,
            Integer currentUserId,
            Pageable pageable) {

        return buscarConFiltrosPaginado(
                categoria, null, precioMin, precioMax, null, busqueda, null, null, currentUserId, pageable);
    }

    // ── Escrituras ────────────────────────────────────────────────────────────

    @Transactional
    public Producto publicar(Producto producto, Integer usuarioId) {
        return actorRepository.findById(usuarioId).map(actor -> {
            producto.setVendedor(actor);
            if (producto.getEstado() == null)
                producto.setEstado(EstadoProducto.DISPONIBLE);
            return productoRepository.save(producto);
        }).orElse(null);
    }

    @Transactional
    public Producto update(Integer id, Producto detalles) {
        return productoRepository.save(detalles);
    }

    @Transactional
    public void delete(Integer id) {
        productoRepository.findById(id).ifPresent(p -> {
            p.setEstado(EstadoProducto.ELIMINADO);
            productoRepository.save(p);
        });
    }

    @Autowired
    private com.nexus.repository.SparkVotoRepository sparkVotoRepository;

    @Transactional
    public java.util.Map<String, Object> votarProducto(Integer actorId, Integer productoId, Boolean isUpvote) {
        int valor = Boolean.TRUE.equals(isUpvote) ? 1 : -1;
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        com.nexus.entity.Actor actor = actorRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));

        Boolean nuevoMiVoto = isUpvote;
        java.util.Optional<com.nexus.entity.SparkVoto> prev = sparkVotoRepository.findByActorIdAndProductoId(actorId,
                productoId);

        if (prev.isPresent()) {
            com.nexus.entity.SparkVoto v = prev.get();
            if (v.getValor() == valor) {
                sparkVotoRepository.deleteByActorAndProducto(actorId, productoId);
                nuevoMiVoto = null;
            } else {
                v.setValor(valor);
                sparkVotoRepository.save(v);
            }
        } else {
            sparkVotoRepository.save(new com.nexus.entity.SparkVoto(actor, producto, Boolean.TRUE.equals(isUpvote)));
        }

        return java.util.Map.of(
                "miVoto", nuevoMiVoto == null ? "NONE" : (nuevoMiVoto ? "SPARK" : "DRIP"));
    }

    @Transactional
    public Producto cambiarEstado(Integer id, EstadoProducto nuevo) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        p.setEstado(nuevo);
        return productoRepository.save(p);
    }
}