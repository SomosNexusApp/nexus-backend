package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

// servicio de productos: CRUD, busqueda con filtros y gestion de estados
@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private BloqueoService bloqueoService;
    @Autowired
    // servicio de sinonimos: convierte "ps5" en ["ps5", "playstation 5", "sony playstation 5"], etc.
    private SynonymService synonymService;
    @Autowired
    // detecta contenido inapropiado en titulos y descripciones antes de guardar
    private ModerationService moderationService;

    @Value("${nexus.anuncio.vida-dias:180}")
    private int vidaAnuncioDias; // duracion de un anuncio en dias (configurable)

    // ── Lecturas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Producto> findAll() {
        return productoRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Producto> findDisponibles() {
        return productoRepository.findByEstadoOrderByPatrocinadoDescFechaPublicacionDesc(EstadoProducto.DISPONIBLE);
    }

    @Transactional(readOnly = true)
    public Optional<Producto> findById(Integer id) {
        return productoRepository.findById(id);
    }

    /**
     * Busqueda principal del marketplace: aplica sinonimos, filtros de precio, categoria,
     * condicion, ubicacion (por bounding box) y excluye usuarios bloqueados.
     * "ps5" encontrara "PlayStation 5", "portatil" encontrara "laptop", etc.
     * Los resultados vienen ordenados con patrocinados primero, luego por fecha.
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
            Double minLat, Double maxLat, Double minLng, Double maxLng,
            Pageable pageable) {

        // el servicio de sinonimos expande el termino de busqueda con variantes equivalentes
        List<String> terms = synonymService.expand(busqueda);

        // si terms esta vacio, la specification no filtrara por texto (devuelve todo)
        String categoriaNorm = (categoria != null && !categoria.isBlank()) ? categoria : null;

        Specification<Producto> spec = ProductoSpecification.buscarConFiltros(
                terms.isEmpty() ? null : terms,
                categoriaNorm,
                precioMin,
                precioMax,
                condicion,
                vendedorId,
                // excluimos productos de usuarios que se han bloqueado mutuamente con el comprador
                bloqueoService.getRelacionesBloqueo(currentUserId),
                minLat, maxLat, minLng, maxLng);

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
                categoria, null, precioMin, precioMax, null, busqueda, null, null, currentUserId,
                null, null, null, null, pageable);
    }

    // ── Escrituras ────────────────────────────────────────────────────────────

    @Transactional
    public Producto publicar(Producto producto, Integer usuarioId) {
        validarModeracion(producto);
        return actorRepository.findById(usuarioId).map(actor -> {
            producto.setVendedor(actor);
            if (producto.getEstado() == null)
                producto.setEstado(EstadoProducto.DISPONIBLE);
            return productoRepository.save(producto);
        }).orElse(null);
    }

    @Transactional
    public Producto update(Integer id, Producto detalles) {
        validarModeracion(detalles);
        return productoRepository.save(detalles);
    }

    private void validarModeracion(Producto p) {
        moderationService.validarYBloquear(p.getTitulo(), "producto", "el título");
        moderationService.validarYBloquear(p.getDescripcion(), "producto", "la descripción");
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

    // sistema de votos Spark/Drip: cada usuario puede dar thumbs up (SPARK) o thumbs down (DRIP) a un producto
    // si vota lo mismo que ya tenia, se cancela el voto (queda en NONE)
    // si vota lo contrario, cambia el voto
    @Transactional
    public java.util.Map<String, Object> votarProducto(Integer actorId, Integer productoId, Boolean isUpvote) {
        int valor = Boolean.TRUE.equals(isUpvote) ? 1 : -1;
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        com.nexus.entity.Actor actor = actorRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));

        Boolean nuevoMiVoto = isUpvote; // este valor lo devolvemos al cliente
        java.util.Optional<com.nexus.entity.SparkVoto> prev = sparkVotoRepository.findByActorIdAndProductoId(actorId,
                productoId);

        if (prev.isPresent()) {
            com.nexus.entity.SparkVoto v = prev.get();
            if (v.getValor() == valor) {
                // voto igual: lo cancelamos (toggle)
                sparkVotoRepository.deleteByActorAndProducto(actorId, productoId);
                nuevoMiVoto = null; // null significa que ya no tiene voto
            } else {
                // voto diferente: cambiamos el valor
                v.setValor(valor);
                sparkVotoRepository.save(v);
            }
        } else {
            // voto nuevo: lo creamos
            sparkVotoRepository.save(new com.nexus.entity.SparkVoto(actor, producto, Boolean.TRUE.equals(isUpvote)));
        }

        return java.util.Map.of(
                "miVoto", nuevoMiVoto == null ? "NONE" : (nuevoMiVoto ? "SPARK" : "DRIP"));
    }

    @Transactional
    public Producto cambiarEstado(Integer id, EstadoProducto nuevo) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));

        if (nuevo == EstadoProducto.VENDIDO && p.getEstado() != EstadoProducto.VENDIDO) {
            p.setFechaVenta(LocalDateTime.now());
        } else if (nuevo != EstadoProducto.VENDIDO) {
            p.setFechaVenta(null);
        }

        p.setEstado(nuevo);
        return productoRepository.save(p);
    }

    /** Reactiva un anuncio caducado dando una nueva ventana de vigencia completa. */
    @Transactional
    public Producto renovar(Integer id, Integer vendedorId) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        // solo el vendedor puede renovar su propio producto
        if (!p.getVendedor().getId().equals(vendedorId)) {
            throw new IllegalStateException("No autorizado");
        }
        // solo se puede renovar si esta en estado EXPIRADO
        if (p.getEstado() != EstadoProducto.EXPIRADO) {
            throw new IllegalStateException("Solo se puede renovar un producto expirado");
        }
        LocalDateTime now = LocalDateTime.now();
        p.setEstado(EstadoProducto.DISPONIBLE);
        p.setFechaPublicacion(now);
        p.setFechaCaducidad(now.plusDays(vidaAnuncioDias)); // nueva ventana de vigencia
        p.setUltimoAvisoCaducidadDias(null); // reseteamos el contador de avisos
        return productoRepository.save(p);
    }
}