package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.*;
import com.nexus.repository.*;

@Service
public class OfertaService {

    @Autowired
    private OfertaRepository ofertaRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private SparkVotoRepository sparkVotoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private BloqueoService bloqueoService;
    @Autowired
    private ModerationService moderationService;

    /**
     * Traduce campo Java (camelCase) → columna SQL (snake_case).
     *
     * NECESARIO porque buscarConFiltros usa nativeQuery=true.
     * En native queries Spring Data NO convierte camelCase a snake_case
     * automáticamente — hay que hacerlo aquí o PostgreSQL lanza
     * "no existe la columna o.fechapublicacion".
     */
    private static final Map<String, String> JAVA_TO_SQL = Map.ofEntries(
            Map.entry("fechaPublicacion", "fecha_publicacion"),
            Map.entry("fechaExpiracion", "fecha_expiracion"),
            Map.entry("precioOferta", "precio_oferta"),
            Map.entry("precioOriginal", "precio_original"),
            Map.entry("sparkCount", "spark_count"),
            Map.entry("dripCount", "drip_count"),
            Map.entry("sparkScore", "spark_score"),
            Map.entry("numeroVistas", "numero_vistas"),
            Map.entry("numeroComentarios", "numero_comentarios"),
            Map.entry("numeroCompartidos", "numero_compartidos"),
            Map.entry("titulo", "titulo"),
            Map.entry("tienda", "tienda"));

    /** Devuelve siempre un nombre de COLUMNA SQL válido (snake_case). */
    private String sanitizarSort(String campo) {
        if (campo == null || campo.isBlank())
            return "fecha_publicacion";
        // Alias del frontend → clave del mapa
        String java = switch (campo) {
            case "fecha" -> "fechaPublicacion";
            case "precio" -> "precioOferta";
            case "spark", "popularidad" -> "sparkScore";
            case "vistas" -> "numeroVistas";
            default -> campo;
        };
        return JAVA_TO_SQL.getOrDefault(java, "fecha_publicacion");
    }

    // ── CRUD ─────────────────────────────────────────────────────────────

    public List<Oferta> findAll() {
        return ofertaRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPublicacion"));
    }

    public Optional<Oferta> findById(Integer id) {
        return ofertaRepository.findById(id);
    }

    public Optional<Oferta> findByIdWithVoto(Integer id, Integer usuarioId) {
        return findById(id).map(o -> {
            if (usuarioId != null) {
                sparkVotoRepository.findByActorIdAndOfertaId(usuarioId, id)
                        .ifPresent(v -> o.setMiVoto(v.getValor() == 1 ? "SPARK" : "DRIP"));
            }
            if (o.getMiVoto() == null)
                o.setMiVoto("NONE");
            return o;
        });
    }

    public void poblarVotos(List<Oferta> ofertas, Integer usuarioId) {
        if (usuarioId == null) {
            ofertas.forEach(o -> o.setMiVoto("NONE"));
            return;
        }
        ofertas.forEach(o -> {
            sparkVotoRepository.findByActorIdAndOfertaId(usuarioId, o.getId())
                    .ifPresentOrElse(
                            v -> o.setMiVoto(v.getValor() == 1 ? "SPARK" : "DRIP"),
                            () -> o.setMiVoto("NONE"));
        });
    }

    public Oferta findByIdOrThrow(Integer id) {
        return ofertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Oferta no encontrada: " + id));
    }

    @Transactional
    public Oferta save(Oferta oferta) {
        validarModeracion(oferta);
        if (oferta.getFechaPublicacion() == null)
            oferta.setFechaPublicacion(LocalDateTime.now());
        oferta.actualizarBadge();
        return ofertaRepository.save(oferta);
    }

    @Transactional
    public void deleteById(Integer id) {
        ofertaRepository.deleteById(id);
    }

    @Transactional
    public Oferta cambiarEstado(Integer id, EstadoOferta nuevo) {
        Oferta o = findByIdOrThrow(id);
        
        if (nuevo == EstadoOferta.AGOTADA && o.getEstado() != EstadoOferta.AGOTADA) {
            o.setFechaFinalizado(LocalDateTime.now());
        } else if (nuevo != EstadoOferta.AGOTADA) {
            o.setFechaFinalizado(null);
        }
        
        o.setEstado(nuevo);
        return ofertaRepository.save(o);
    }

    // ── Crear con imagenes ───────────────────────────────────────────────

    @Transactional
    public Oferta crear(Oferta oferta, Integer actorId, List<MultipartFile> imagenes) {
        validarModeracion(oferta);
        Actor actor = actorRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));
        oferta.setActor(actor);
        oferta.setFechaPublicacion(LocalDateTime.now());
        oferta.setEsActiva(true);
        if (oferta.getSparkCount() == null)
            oferta.setSparkCount(0);
        if (oferta.getDripCount() == null)
            oferta.setDripCount(0);

        if (imagenes != null) {
            for (MultipartFile img : imagenes) {
                String url = storageService.subirImagen(img);
                if (url != null) {
                    if (oferta.getImagenPrincipal() == null)
                        oferta.setImagenPrincipal(url);
                    else
                        oferta.addImagenGaleria(url);
                }
            }
        }
        oferta.actualizarBadge();
        Oferta guardada = ofertaRepository.save(oferta);
        notificacionService.notificarSparkEnOferta(actorId, guardada.getTitulo());
        return guardada;
    }

    private void validarModeracion(Oferta o) {
        moderationService.validarYBloquear(o.getTitulo(), "oferta", "el título");
        moderationService.validarYBloquear(o.getDescripcion(), "oferta", "la descripción");
    }

    @Transactional
    public void setCategoriaByNombre(Oferta oferta, String nombre) {
        if (nombre == null || nombre.isBlank())
            return;
        categoriaRepository.findByNombre(nombre)
                .or(() -> categoriaRepository.findBySlug(
                        nombre.toLowerCase().replaceAll("[^a-z0-9]", "-")))
                .ifPresent(oferta::setCategoria);
    }

    // ── Listados especiales ──────────────────────────────────────────────

    public List<Oferta> obtenerDestacadas() {
        return ofertaRepository.findDestacadas(LocalDateTime.now().minusDays(7), PageRequest.of(0, 20));
    }

    public List<Oferta> obtenerFlash() {
        return ofertaRepository.findByEsFlashTrueAndEsActivaTrueOrderByFechaPublicacionDesc();
    }

    public List<Oferta> obtenerTrending() {
        return ofertaRepository.findTrending(LocalDateTime.now().minusHours(24), PageRequest.of(0, 20));
    }

    public List<Oferta> obtenerTopSpark() {
        return ofertaRepository.findTopBySparkScore(PageRequest.of(0, 20));
    }

    public List<Oferta> obtenerProximasExpirar() {
        return ofertaRepository.findProximasExpirar(LocalDateTime.now(), LocalDateTime.now().plusHours(24));
    }

    public List<Oferta> getRecientes(int limite) {
        return ofertaRepository.findRecientes(PageRequest.of(0, limite));
    }

    public List<Oferta> getByCategoria(String c) {
        return ofertaRepository.findByCategoria(c);
    }

    public List<Oferta> getByBadge(BadgeOferta b) {
        return ofertaRepository.findByBadgeAndEsActivaTrue(b);
    }

    public List<Oferta> buscarTexto(String q) {
        return ofertaRepository.buscarPorTexto(q);
    }

    public List<Oferta> getByActorId(Integer id) {
        return ofertaRepository.findByActorId(id);
    }

    // ── Búsqueda con filtros ─────────────────────────────────────────────

    public Page<Oferta> buscarConFiltros(String categoria, String tienda,
            Double precioMin, Double precioMax,
            String busqueda, Boolean soloActivas,
            String sortField, String sortDir,
            Integer actorId, Integer currentUserId,
            Double lat, Double lng, Double radius,
            Pageable pageable) {
        boolean solo = Boolean.TRUE.equals(soloActivas);

        Double minLat = null, maxLat = null, minLng = null, maxLng = null;
        if (lat != null && lng != null && radius != null && radius > 0) {
            double deltaLat = radius / 111.1;
            double deltaLng = radius / (111.1 * Math.cos(Math.toRadians(lat)));
            minLat = lat - deltaLat;
            maxLat = lat + deltaLat;
            minLng = lng - deltaLng;
            maxLng = lng + deltaLng;
        }

        String columna = sanitizarSort(sortField);
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(Sort.Direction.ASC, columna)
                : Sort.by(Sort.Direction.DESC, columna);
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        List<Integer> excluded = bloqueoService.getRelacionesBloqueo(currentUserId);
        boolean hasExcluded = (excluded != null && !excluded.isEmpty());
        if (excluded != null && excluded.isEmpty()) excluded = null;

        return ofertaRepository.buscarConFiltrosGeograficos(
                categoria, tienda, precioMin, precioMax, busqueda, solo, actorId, 
                excluded, hasExcluded,
                minLat, maxLat, minLng, maxLng, pageable);
    }

    public Page<Oferta> buscarConFiltrosGeograficos(String categoria, String tienda,
            Double precioMin, Double precioMax,
            String busqueda, Boolean soloActivas,
            String sortField, String sortDir,
            Integer actorId, Integer currentUserId,
            Double minLat, Double maxLat, Double minLng, Double maxLng,
            Pageable pageable) {
        boolean solo = Boolean.TRUE.equals(soloActivas);
        String columna = sanitizarSort(sortField);
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(Sort.Direction.ASC, columna)
                : Sort.by(Sort.Direction.DESC, columna);
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        List<Integer> excluded = bloqueoService.getRelacionesBloqueo(currentUserId);
        boolean hasExcluded = (excluded != null && !excluded.isEmpty());
        if (excluded != null && excluded.isEmpty()) excluded = null;

        return ofertaRepository.buscarConFiltrosGeograficos(
                categoria, tienda, precioMin, precioMax, busqueda, solo, actorId, 
                excluded, hasExcluded,
                minLat, maxLat, minLng, maxLng, pageable);
    }

    public Page<Oferta> buscarConFiltros(String categoria, String tienda,
            Double precioMin, Double precioMax,
            String busqueda, boolean soloActivas,
            Integer actorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "fecha_publicacion"));
        
        List<Integer> excluded = bloqueoService.getRelacionesBloqueo(null);
        boolean hasExcluded = excluded != null && !excluded.isEmpty();
        if (excluded != null && excluded.isEmpty()) excluded = null;

        return ofertaRepository.buscarConFiltrosGeograficos(
                categoria, tienda, precioMin, precioMax, busqueda, soloActivas, actorId, 
                excluded, hasExcluded,
                null, null, null, null, pageable);
    }

    // ── Interacciones ────────────────────────────────────────────────────

    @Transactional
    public void incrementarVistas(Integer id) {
        ofertaRepository.findById(id).ifPresent(o -> {
            o.setNumeroVistas(o.getNumeroVistas() != null ? o.getNumeroVistas() + 1 : 1);
            ofertaRepository.save(o);
        });
    }

    @Transactional
    public void incrementarCompartidos(Integer id) {
        ofertaRepository.findById(id).ifPresent(o -> {
            o.setNumeroCompartidos(o.getNumeroCompartidos() != null ? o.getNumeroCompartidos() + 1 : 1);
            ofertaRepository.save(o);
        });
    }

    // ── Votos ────────────────────────────────────────────────────────────

    @Transactional
    public java.util.Map<String, Object> votarOferta(Integer actorId, Integer ofertaId, Boolean isUpvote) {
        int valor = Boolean.TRUE.equals(isUpvote) ? 1 : -1;
        Oferta oferta = findByIdOrThrow(ofertaId);
        com.nexus.entity.Actor actor = actorRepository.findById(actorId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Actor no encontrado"));

        String nuevoMiVoto;
        java.util.Optional<com.nexus.entity.SparkVoto> prev = sparkVotoRepository.findByActorIdAndOfertaId(actorId,
                ofertaId);

        if (prev.isPresent()) {
            com.nexus.entity.SparkVoto v = prev.get();
            if (v.getValor() == valor) {
                // Quitar voto si es el mismo
                if (valor == 1)
                    oferta.setSparkCount(Math.max(0, oferta.getSparkCount() - 1));
                else
                    oferta.setDripCount(Math.max(0, oferta.getDripCount() - 1));
                sparkVotoRepository.deleteByActorAndOferta(actorId, ofertaId);
                nuevoMiVoto = "NONE";
            } else {
                // Cambiar voto (de spark a drip o viceversa)
                if (valor == 1) {
                    oferta.setSparkCount(oferta.getSparkCount() + 1);
                    oferta.setDripCount(Math.max(0, oferta.getDripCount() - 1));
                } else {
                    oferta.setDripCount(oferta.getDripCount() + 1);
                    oferta.setSparkCount(Math.max(0, oferta.getSparkCount() - 1));
                }
                v.setValor(valor);
                sparkVotoRepository.save(v);
                nuevoMiVoto = isUpvote ? "SPARK" : "DRIP";
            }
        } else {
            // Nuevo voto
            sparkVotoRepository.save(new com.nexus.entity.SparkVoto(actor, oferta, Boolean.TRUE.equals(isUpvote)));
            if (valor == 1)
                oferta.setSparkCount(oferta.getSparkCount() + 1);
            else
                oferta.setDripCount(oferta.getDripCount() + 1);
            nuevoMiVoto = isUpvote ? "SPARK" : "DRIP";
        }

        oferta.recalcularScore();
        Oferta saved = ofertaRepository.save(oferta);
        saved.actualizarBadge();

        return java.util.Map.of(
                "sparkScore", saved.getSparkScore(),
                "badge", saved.getBadge() != null ? saved.getBadge().toString() : "NUEVA",
                "miVoto", nuevoMiVoto);
    }

    // ── Meta-datos ───────────────────────────────────────────────────────

    public List<String> getCategorias() {
        return ofertaRepository.findCategoriasDistintas();
    }

    public List<String> getTiendas() {
        return ofertaRepository.findTiendasDistintas();
    }

    public Map<String, Object> getEstadisticas() {
        return Map.of("totalActivas", ofertaRepository.countActivas(),
                "categorias", getCategorias(),
                "tiendas", getTiendas());
    }
}