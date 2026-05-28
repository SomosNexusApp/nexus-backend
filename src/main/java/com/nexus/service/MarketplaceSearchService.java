package com.nexus.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.Categoria;
import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.Vehiculo;

@Service
@Transactional(readOnly = true)
public class MarketplaceSearchService {

    @Autowired private OfertaService ofertaService;
    @Autowired private ProductoService productoService;
    @Autowired private VehiculoService vehiculoService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private com.nexus.repository.ContratoRepository contratoRepository;
    @Autowired private com.nexus.repository.ProductoRepository productoRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Punto de entrada público
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> buscarTodo(
            String q, String tipo, String categoria,
            Double precioMin, Double precioMax, String condicion, String ubicacion,
            Double lat, Double lng, Double radius,
            Integer usuarioId, String orden,
            Pageable pageable) {
        try {
            return buscarTodoInternal(q, tipo, categoria, precioMin, precioMax,
                    condicion, ubicacion, lat, lng, radius, usuarioId, orden, pageable);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lógica interna
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buscarTodoInternal(
            String q, String tipo, String categoria,
            Double precioMin, Double precioMax, String condicion, String ubicacion,
            Double lat, Double lng, Double radius,
            Integer usuarioId, String orden,
            Pageable pageable) {

        int sizePerType = pageable.getPageSize();

        // ── 0. Bounding Box geográfico ────────────────────────────────────────
        Double minLat = null, maxLat = null, minLng = null, maxLng = null;
        if (lat != null && lng != null && radius != null && radius > 0) {
            double deltaLat = radius / 111.1;
            double deltaLng = radius / (111.1 * Math.cos(Math.toRadians(lat)));
            minLat = lat - deltaLat;
            maxLat = lat + deltaLat;
            minLng = lng - deltaLng;
            maxLng = lng + deltaLng;
        }

        // ── 1. Resolver slugs de categoría + hijos ────────────────────────────
        List<String> categorySlugs = new ArrayList<>();
        boolean esCategoriaVehiculo = false;

        if (categoria != null && !categoria.isBlank()) {
            categorySlugs.add(categoria);
            Optional<Categoria> catOpt = categoriaService.findBySlug(categoria);
            if (catOpt.isPresent()) {
                Categoria parent = catOpt.get();
                collectHijosSlugs(parent, categorySlugs);

                // ¿Es una categoría de vehículos?
                esCategoriaVehiculo = "vehiculos".equalsIgnoreCase(parent.getSlug())
                        || (parent.getParent() != null
                                && "vehiculos".equalsIgnoreCase(parent.getParent().getSlug()));
            }

            // Fallback heurístico por slug sin necesidad de BD
            if (!esCategoriaVehiculo) {
                List<String> slugsMotor = List.of("vehiculos", "coches", "motos",
                        "furgonetas", "caravanas", "otros-vehiculos", "scooters");
                esCategoriaVehiculo = slugsMotor.contains(categoria.toLowerCase());
            }
        }

        // String listo para pasar a los servicios (null = sin filtro)
        String catForSpec = categorySlugs.isEmpty() ? null : String.join(",", categorySlugs);

        // ── 2. Patrocinados ───────────────────────────────────────────────────
        List<Map<String, Object>> sponsoredItems = fetchSponsoredItems(
                q, tipo, categorySlugs, precioMin, precioMax,
                minLat, maxLat, minLng, maxLng);

        // ── 3. Sort para sub-servicios ────────────────────────────────────────
        org.springframework.data.domain.Sort subSort =
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "fechaPublicacion");
        if ("precio_asc".equalsIgnoreCase(orden)) {
            subSort = org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.ASC, "precio");
        } else if ("precio_desc".equalsIgnoreCase(orden)) {
            subSort = org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "precio");
        }

        PageRequest subPage = PageRequest.of(pageable.getPageNumber(), sizePerType, subSort);

        // Sort específico para ofertas
        String sortOferta = "fechaPublicacion";
        String dirOferta  = "desc";
        if ("precio_asc".equalsIgnoreCase(orden)) {
            sortOferta = "precioOferta"; dirOferta = "asc";
        } else if ("precio_desc".equalsIgnoreCase(orden)) {
            sortOferta = "precioOferta"; dirOferta = "desc";
        } else if ("spark".equalsIgnoreCase(orden)) {
            sortOferta = "sparkScore"; dirOferta = "desc";
        }

        // ── 4. Ofertas ────────────────────────────────────────────────────────
        Page<Oferta> paginaOfertas = Page.empty();
        boolean buscarOfertas = tipo == null
                || "TODOS".equalsIgnoreCase(tipo)
                || "OFERTA".equalsIgnoreCase(tipo);
        if (buscarOfertas && (condicion == null || condicion.isBlank())) {
            paginaOfertas = ofertaService.buscarConFiltrosGeograficos(
                    catForSpec, null, precioMin, precioMax, q, true, ubicacion,
                    sortOferta, dirOferta, null, usuarioId,
                    minLat, maxLat, minLng, maxLng, subPage);
        }

        // ── 5. Productos ──────────────────────────────────────────────────────
        Page<Producto> paginaProductos = Page.empty();
        boolean buscarProductos = tipo == null
                || "TODOS".equalsIgnoreCase(tipo)
                || "PRODUCTO".equalsIgnoreCase(tipo);
        if (buscarProductos) {
            paginaProductos = productoService.buscarConFiltrosPaginado(
                    catForSpec, null, precioMin, precioMax, condicion, q, ubicacion,
                    null, usuarioId != null ? usuarioId : 0,
                    minLat, maxLat, minLng, maxLng, subPage);
        }

        // ── 6. Vehículos ──────────────────────────────────────────────────────
        Page<Vehiculo> paginaVehiculos = Page.empty();
        boolean buscarVehiculos = tipo == null
                || "TODOS".equalsIgnoreCase(tipo)
                || "VEHICULO".equalsIgnoreCase(tipo);
        if (buscarVehiculos) {
            // Solo ejecutar si la categoría es de vehículos o no hay categoría
            boolean sinFiltroCategoria = categoria == null || categoria.isBlank();
            if (esCategoriaVehiculo || sinFiltroCategoria) {
                paginaVehiculos = vehiculoService.buscarPaginadoGeografico(
                        null, null, null, precioMin, precioMax,
                        null, null, null, null, null,
                        q, condicion, ubicacion, null, null, null, null, null, null, null,
                        minLat, maxLat, minLng, maxLng, subPage);
            }
        }

        // ── 7. Transformar y combinar ─────────────────────────────────────────
        List<Map<String, Object>> items = new ArrayList<>();

        items.addAll(paginaOfertas.getContent().stream()
                .map(this::ofertaToMap)
                .collect(Collectors.toList()));

        items.addAll(paginaProductos.getContent().stream()
                .map(this::productoToMap)
                .collect(Collectors.toList()));

        items.addAll(paginaVehiculos.getContent().stream()
                .map(this::vehiculoToMap)
                .collect(Collectors.toList()));

        // ── 8. Inyectar patrocinados sin duplicar ─────────────────────────────
        List<Map<String, Object>> finalItems = new ArrayList<>(sponsoredItems);
        Set<String> seenIds = sponsoredItems.stream()
                .map(it -> it.get("searchType") + "_" + it.get("id"))
                .collect(Collectors.toSet());

        for (Map<String, Object> it : items) {
            String key = it.get("searchType") + "_" + it.get("id");
            if (seenIds.add(key)) {
                finalItems.add(it);
            }
        }

        // ── 9. Ordenación global ──────────────────────────────────────────────
        finalItems.sort((a, b) -> {
            try {
                if ("precio_asc".equalsIgnoreCase(orden)) {
                    return Double.compare(
                            toDouble(a.get("precio")),
                            toDouble(b.get("precio")));
                } else if ("precio_desc".equalsIgnoreCase(orden)) {
                    return Double.compare(
                            toDouble(b.get("precio")),
                            toDouble(a.get("precio")));
                } else {
                    Object fa = a.get("fechaPublicacion");
                    Object fb = b.get("fechaPublicacion");
                    if (fa instanceof java.time.LocalDateTime la
                            && fb instanceof java.time.LocalDateTime lb) {
                        return lb.compareTo(la);
                    }
                    return 0;
                }
            } catch (Exception e) {
                return 0;
            }
        });

        long totalCount = paginaOfertas.getTotalElements()
                + paginaProductos.getTotalElements()
                + paginaVehiculos.getTotalElements();

        return Map.of(
                "items",  finalItems,
                "total",  totalCount,
                "page",   pageable.getPageNumber(),
                "size",   pageable.getPageSize());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Patrocinados
    // ─────────────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> fetchSponsoredItems(
            String q, String tipo, List<String> categorySlugs,
            Double precioMin, Double precioMax,
            Double minLat, Double maxLat, Double minLng, Double maxLng) {

        List<Map<String, Object>> sponsored = new ArrayList<>();

        if (tipo != null && !"TODOS".equalsIgnoreCase(tipo) && !"PRODUCTO".equalsIgnoreCase(tipo)) {
            return sponsored;
        }

        try {
            // A. Productos de contratos de publicidad activos
            List<com.nexus.entity.Contrato> contratos =
                    contratoRepository.findActiveSponsoredProducts();
            for (com.nexus.entity.Contrato c : contratos) {
                if (c.getProductoId() == null) continue;
                productoRepository.findById(c.getProductoId()).ifPresent(p -> {
                    if (p.getEstado() == EstadoProducto.DISPONIBLE
                            && matchesPrecio(p, precioMin, precioMax)
                            && matchesQuery(p, q)
                            && matchesCategoria(p, categorySlugs)
                            && matchesGeo(p, minLat, maxLat, minLng, maxLng)) {
                        sponsored.add(transformToMap(p));
                    }
                });
            }

            // B. Productos con flag patrocinado (máximo 5)
            if (sponsored.size() < 5) {
                List<Producto> flagged = productoRepository
                        .findByEstado(EstadoProducto.DISPONIBLE);
                for (Producto p : flagged) {
                    if (sponsored.size() >= 5) break;
                    if (!Boolean.TRUE.equals(p.getPatrocinado())) continue;
                    boolean alreadyIn = sponsored.stream()
                            .anyMatch(it -> it.get("id").equals(p.getId()));
                    if (!alreadyIn
                            && matchesPrecio(p, precioMin, precioMax)
                            && matchesQuery(p, q)
                            && matchesCategoria(p, categorySlugs)
                            && matchesGeo(p, minLat, maxLat, minLng, maxLng)) {
                        sponsored.add(transformToMap(p));
                    }
                }
            }
        } catch (Exception e) {
            // No bloquear la búsqueda principal por fallo en patrocinados
        }
        return sponsored;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de filtrado para patrocinados
    // ─────────────────────────────────────────────────────────────────────────

    private boolean matchesPrecio(Producto p, Double min, Double max) {
        double precio = p.getPrecio() != null ? p.getPrecio() : 0.0;
        return (min == null || precio >= min) && (max == null || precio <= max);
    }

    private boolean matchesQuery(Producto p, String q) {
        return q == null || q.isBlank()
                || p.getTitulo().toLowerCase().contains(q.toLowerCase());
    }

    private boolean matchesCategoria(Producto p, List<String> slugs) {
        return slugs.isEmpty()
                || (p.getCategoria() != null && slugs.contains(p.getCategoria().getSlug()));
    }

    private boolean matchesGeo(Producto p, Double minLat, Double maxLat,
                                Double minLng, Double maxLng) {
        if (minLat == null) return true;
        if (p.getLatitude() == null || p.getLongitude() == null) return false;
        return p.getLatitude() >= minLat && p.getLatitude() <= maxLat
                && p.getLongitude() >= minLng && p.getLongitude() <= maxLng;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transformadores entidad → Map
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> ofertaToMap(Oferta o) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id",                  o.getId());
        m.put("titulo",              o.getTitulo());
        m.put("imagenPrincipal",     o.getImagenPrincipal());
        m.put("precioOriginal",      o.getPrecioOriginal() != null  ? o.getPrecioOriginal()  : 0);
        m.put("precioOferta",        o.getPrecioOferta()   != null  ? o.getPrecioOferta()    : 0);
        m.put("precio",              o.getPrecioOferta()   != null  ? o.getPrecioOferta()
                                   : o.getPrecioOriginal() != null  ? o.getPrecioOriginal()  : 0);
        m.put("fechaPublicacion",    o.getFechaPublicacion());
        m.put("searchType",          "OFERTA");
        m.put("ubicacion",           o.getCiudadOferta());
        m.put("latitude",            o.getLatitude());
        m.put("longitude",           o.getLongitude());
        m.put("esOnline",            o.getEsOnline());
        m.put("tienda",              o.getTienda());
        m.put("urlOferta",           o.getUrlOferta());
        m.put("urlExterna",          o.getUrlOferta());
        m.put("numeroComentarios",   o.getNumeroComentarios());
        m.put("sparkScore",          o.getSparkScore() != null ? o.getSparkScore() : 0);
        m.put("codigoDescuento",     o.getCodigoDescuento());
        m.put("badge",               o.getBadge() != null ? o.getBadge().name() : null);
        m.put("destacada",           o.getDestacada());
        m.put("esFlash",             o.getEsFlash());
        if (o.getMiVoto() != null)   m.put("miVoto", o.getMiVoto());

        if (o.getCategoria() != null) {
            m.put("categoria", Map.of(
                    "nombre", o.getCategoria().getNombre(),
                    "slug",   o.getCategoria().getSlug()));
        }
        if (o.getActor() != null) {
            String nombre = o.getActor().getNombre() != null
                    ? o.getActor().getNombre() : o.getActor().getUser();
            java.util.Map<String, Object> vendMap = new java.util.HashMap<>();
            vendMap.put("nombre", nombre);
            vendMap.put("verificado", o.getActor().isCuentaVerificada());
            vendMap.put("user", o.getActor().getUser());
            vendMap.put("avatar", o.getActor().getAvatar());
            vendMap.put("googleAvatarUrl", o.getActor().getGoogleAvatarUrl());
            vendMap.put("avatarSource", o.getActor().getAvatarSource());
            vendMap.put("customAvatarUrl", o.getActor().getCustomAvatarUrl());
            m.put("vendedor", vendMap);
        }
        return m;
    }

    private Map<String, Object> productoToMap(Producto p) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id",               p.getId());
        m.put("titulo",           p.getTitulo());
        m.put("imagenPrincipal",  p.getImagenPrincipal());
        m.put("precio",           p.getPrecio() != null ? p.getPrecio() : 0);
        m.put("fechaPublicacion", p.getFechaPublicacion());
        m.put("searchType",       "PRODUCTO");
        m.put("ubicacion",        p.getUbicacion());
        m.put("latitude",         p.getLatitude());
        m.put("longitude",        p.getLongitude());

        if (p.getCategoria() != null) {
            m.put("categoria", Map.of(
                    "nombre", p.getCategoria().getNombre(),
                    "slug",   p.getCategoria().getSlug()));
        }
        if (p.getVendedor() != null) {
            String nombre = p.getVendedor().getNombre() != null
                    ? p.getVendedor().getNombre() : "Usuario";
            java.util.Map<String, Object> vendMap = new java.util.HashMap<>();
            vendMap.put("nombre", nombre);
            vendMap.put("verificado", p.getVendedor().isCuentaVerificada());
            vendMap.put("user", p.getVendedor().getUser());
            vendMap.put("avatar", p.getVendedor().getAvatar());
            vendMap.put("googleAvatarUrl", p.getVendedor().getGoogleAvatarUrl());
            vendMap.put("avatarSource", p.getVendedor().getAvatarSource());
            vendMap.put("customAvatarUrl", p.getVendedor().getCustomAvatarUrl());
            m.put("vendedor", vendMap);
        }

        if (p.getEstado() == EstadoProducto.VENDIDO && p.getFechaVenta() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDateTime.now(), 
                p.getFechaVenta().plusDays(14)
            );
            m.put("diasRestantesVendido", Math.max(0, days));
            m.put("estado", "VENDIDO");
        } else {
            m.put("estado", p.getEstado().name());
        }

        return m;
    }

    private Map<String, Object> vehiculoToMap(Vehiculo v) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id",               v.getId());
        m.put("titulo",           v.getTitulo());
        m.put("imagenPrincipal",  v.getImagenPrincipal());
        m.put("precio",           v.getPrecio() != null ? v.getPrecio() : 0);
        m.put("fechaPublicacion", v.getFechaPublicacion());
        m.put("searchType",       "VEHICULO");
        m.put("ubicacion",        v.getUbicacion());
        m.put("latitude",         v.getLatitude());
        m.put("longitude",        v.getLongitude());

        if (v.getCategoria() != null) {
            m.put("categoria", Map.of(
                    "nombre", v.getCategoria().getNombre(),
                    "slug",   v.getCategoria().getSlug()));
        }
        if (v.getPublicador() != null) {
            String nombre = v.getPublicador().getNombre() != null
                    ? v.getPublicador().getNombre() : v.getPublicador().getUser();
            java.util.Map<String, Object> vendMap = new java.util.HashMap<>();
            vendMap.put("nombre", nombre);
            vendMap.put("verificado", v.getPublicador().isCuentaVerificada());
            vendMap.put("user", v.getPublicador().getUser());
            vendMap.put("avatar", v.getPublicador().getAvatar());
            vendMap.put("googleAvatarUrl", v.getPublicador().getGoogleAvatarUrl());
            vendMap.put("avatarSource", v.getPublicador().getAvatarSource());
            vendMap.put("customAvatarUrl", v.getPublicador().getCustomAvatarUrl());
            m.put("vendedor", vendMap);
        }
        return m;
    }

    private Map<String, Object> transformToMap(Producto p) {
        Map<String, Object> m = productoToMap(p);
        m.put("sponsored", true);
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────────────────

    /** Recorre el árbol de hijos y acumula sus slugs. */
    private void collectHijosSlugs(Categoria parent, List<String> slugs) {
        if (parent.getHijos() == null) return;
        for (Categoria hijo : parent.getHijos()) {
            if (hijo.getSlug() != null && !slugs.contains(hijo.getSlug())) {
                slugs.add(hijo.getSlug());
                collectHijosSlugs(hijo, slugs);
            }
        }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
