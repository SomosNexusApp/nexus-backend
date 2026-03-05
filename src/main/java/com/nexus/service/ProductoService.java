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

    @Autowired private ProductoRepository productoRepository;
    @Autowired private ActorRepository    actorRepository;
    @Autowired private SynonymService     synonymService;

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
            Pageable pageable) {

        // Expandir el término con sinónimos
        List<String> terms = synonymService.expand(busqueda);

        // Si no hay términos de búsqueda ni filtros de texto, terms queda vacío → trae todo
        String categoriaNorm = (categoria != null && !categoria.isBlank()) ? categoria : null;

        Specification<Producto> spec = ProductoSpecification.buscarConFiltros(
            terms.isEmpty() ? null : terms,
            categoriaNorm,
            precioMin,
            precioMax
        );

        return productoRepository.findAll(spec, pageable);
    }

    /** Alias para compatibilidad con código que llame a la firma corta. */
    @Transactional(readOnly = true)
    public Page<Producto> buscarConFiltros(
            String categoria,
            Double precioMin,
            Double precioMax,
            String busqueda,
            Pageable pageable) {

        return buscarConFiltrosPaginado(
            categoria, null, precioMin, precioMax, null, busqueda, null, pageable);
    }

    // ── Escrituras ────────────────────────────────────────────────────────────

    @Transactional
    public Producto publicar(Producto producto, Integer usuarioId) {
        return actorRepository.findById(usuarioId).map(actor -> {
            producto.setVendedor(actor);
            if (producto.getEstado() == null) producto.setEstado(EstadoProducto.DISPONIBLE);
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

    @Transactional
    public Producto cambiarEstado(Integer id, EstadoProducto nuevo) {
        Producto p = productoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        p.setEstado(nuevo);
        return productoRepository.save(p);
    }
}