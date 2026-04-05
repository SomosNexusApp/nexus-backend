package com.nexus.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.Vehiculo;

@Service
public class MarketplaceSearchService {

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private VehiculoService vehiculoService;
    
    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private com.nexus.repository.ContratoRepository contratoRepository;

    @Autowired
    private com.nexus.repository.ProductoRepository productoRepository;

    public Map<String, Object> buscarTodo(
            String q,
            String tipo,
            String categoria,
            Double precioMin,
            Double precioMax,
            String ubicacion,
            Double lat,
            Double lng,
            Double radius,
            Integer usuarioId,
            String orden,
            Pageable pageable) {

        int sizePerType = pageable.getPageSize();

        // 0. Bounding Box (Aproximado)
        Double minLat = null, maxLat = null, minLng = null, maxLng = null;
        if (lat != null && lng != null && radius != null && radius > 0) {
            double deltaLat = radius / 111.1;
            double deltaLng = radius / (111.1 * Math.cos(Math.toRadians(lat)));
            minLat = lat - deltaLat;
            maxLat = lat + deltaLat;
            minLng = lng - deltaLng;
            maxLng = lng + deltaLng;
        }

        // 0.1 Categoría y subcategorías (Recursivo)
        List<String> categorySlugs = new ArrayList<>();
        if (categoria != null && !categoria.isBlank()) {
            categorySlugs.add(categoria);
            categoriaService.findBySlug(categoria).ifPresent(parent -> {
                collectHijosSlugs(parent, categorySlugs);
            });
        }

        // 1. Patrocinados (Inyección Proactiva)
        List<Map<String, Object>> sponsoredItems = fetchSponsoredItems(q, tipo, categorySlugs, precioMin, precioMax, minLat, maxLat, minLng, maxLng);

        // Construir Sort para los sub-servicios
        org.springframework.data.domain.Sort subSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaPublicacion");
        
        if ("precio_asc".equalsIgnoreCase(orden)) {
            subSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "precio");
        } else if ("precio_desc".equalsIgnoreCase(orden)) {
            subSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "precio");
        }

        PageRequest subPage = PageRequest.of(pageable.getPageNumber(), sizePerType, subSort);

        // Determinar orden para ofertas (usan nombres de campo distintos)
        String sortOferta = "fechaPublicacion";
        String dirOferta = "desc";
        if ("precio_asc".equalsIgnoreCase(orden)) {
            sortOferta = "precioOferta";
            dirOferta = "asc";
        } else if ("precio_desc".equalsIgnoreCase(orden)) {
            sortOferta = "precioOferta";
            dirOferta = "desc";
        } else if ("spark".equalsIgnoreCase(orden)) {
            sortOferta = "sparkScore";
            dirOferta = "desc";
        }

        // 2. Buscar Ofertas
        Page<Oferta> paginaOfertas = Page.empty();
        if (tipo == null || "TODOS".equalsIgnoreCase(tipo) || "OFERTA".equalsIgnoreCase(tipo)) {
            // Usar la lista completa de slugs para ofertas también
            String catForSpec = categorySlugs.isEmpty() ? null : String.join(",", categorySlugs);
            paginaOfertas = ofertaService.buscarConFiltrosGeograficos(
                catForSpec, null, precioMin, precioMax, q, true, sortOferta, dirOferta, null, usuarioId,
                minLat, maxLat, minLng, maxLng, subPage);
        }

        // 3. Buscar Productos
        Page<Producto> paginaProductos = Page.empty();
        if (tipo == null || "TODOS".equalsIgnoreCase(tipo) || "PRODUCTO".equalsIgnoreCase(tipo)) {
            // Pasamos null si no hay slugs para que no filtre
            String catForSpec = categorySlugs.isEmpty() ? null : String.join(",", categorySlugs);
            paginaProductos = productoService.buscarConFiltrosPaginado(
                catForSpec, null, precioMin, precioMax, null, q, ubicacion, null, usuarioId != null ? usuarioId : 0,
                minLat, maxLat, minLng, maxLng, subPage);
        }

        // 4. Buscar Vehículos
        boolean esCategoriaVehiculo = false;
        if (categoria != null && !categoria.isBlank()) {
            List<String> slugsMotor = List.of("motor", "vehiculos", "coches", "motos", "furgonetas", "caravanas", "otros-vehiculos", "scooters");
            esCategoriaVehiculo = slugsMotor.contains(categoria.toLowerCase());
        }

        Page<Vehiculo> paginaVehiculos = Page.empty();
        if ((tipo == null || "TODOS".equalsIgnoreCase(tipo) || "VEHICULO".equalsIgnoreCase(tipo))) {
            if (esCategoriaVehiculo || (categoria == null || categoria.isBlank())) {
                paginaVehiculos = vehiculoService.buscarPaginadoGeografico(
                        null, null, null, precioMin, precioMax, null, null, null, null, null, q, null, null, null, null, null,
                        null, null,
                        minLat, maxLat, minLng, maxLng, subPage);
            }
        }

        // 5. Transformar y Combinar
        List<Map<String, Object>> items = new ArrayList<>();

        items.addAll(paginaOfertas.getContent().stream().map(o -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", o.getId());
            m.put("titulo", o.getTitulo());
            m.put("imagenPrincipal", o.getImagenPrincipal());
            m.put("precioOriginal", o.getPrecioOriginal() != null ? o.getPrecioOriginal() : 0);
            m.put("precioOferta", o.getPrecioOferta() != null ? o.getPrecioOferta() : 0);
            m.put("precio", o.getPrecioOferta() != null ? o.getPrecioOferta() : (o.getPrecioOriginal() != null ? o.getPrecioOriginal() : 0));
            m.put("fechaPublicacion", o.getFechaPublicacion());
            m.put("searchType", "OFERTA");
            m.put("ubicacion", o.getCiudadOferta());
            m.put("latitude", o.getLatitude());
            m.put("longitude", o.getLongitude());
            m.put("esOnline", o.getEsOnline());
            if (o.getCategoria() != null) {
                Map<String, Object> catMap = new java.util.HashMap<>();
                catMap.put("nombre", o.getCategoria().getNombre());
                catMap.put("slug", o.getCategoria().getSlug());
                m.put("categoria", catMap);
            }
            if (o.getMiVoto() != null)
                m.put("miVoto", o.getMiVoto());
            m.put("sparkScore", o.getSparkScore() != null ? o.getSparkScore() : 0);
            return m;
        }).collect(Collectors.toList()));

        items.addAll(paginaProductos.getContent().stream().map(p -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", p.getId());
            m.put("titulo", p.getTitulo());
            m.put("imagenPrincipal", p.getImagenPrincipal());
            m.put("precio", p.getPrecio() != null ? p.getPrecio() : 0);
            m.put("fechaPublicacion", p.getFechaPublicacion());
            m.put("searchType", "PRODUCTO");
            m.put("ubicacion", p.getUbicacion());
            m.put("latitude", p.getLatitude());
            m.put("longitude", p.getLongitude());
            if (p.getCategoria() != null) {
                Map<String, Object> catMap = new java.util.HashMap<>();
                catMap.put("nombre", p.getCategoria().getNombre());
                catMap.put("slug", p.getCategoria().getSlug());
                m.put("categoria", catMap);
            }
            if (p.getVendedor() != null) {
                Map<String, Object> vendMap = new java.util.HashMap<>();
                vendMap.put("nombre", p.getVendedor().getNombre() != null ? p.getVendedor().getNombre() : "Usuario");
                vendMap.put("verificado", p.getVendedor().isCuentaVerificada());
                m.put("vendedor", vendMap);
            }
            return m;
        }).collect(Collectors.toList()));

        items.addAll(paginaVehiculos.getContent().stream().map(v -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", v.getId());
            m.put("titulo", v.getTitulo());
            m.put("imagenPrincipal", v.getImagenPrincipal());
            m.put("precio", v.getPrecio() != null ? v.getPrecio() : 0);
            m.put("fechaPublicacion", v.getFechaPublicacion());
            m.put("searchType", "VEHICULO");
            m.put("ubicacion", v.getUbicacion());
            m.put("latitude", v.getLatitude());
            m.put("longitude", v.getLongitude());
            return m;
        }).collect(Collectors.toList()));

        // 6. Unificar con Patrocinados (evitar duplicados)
        List<Map<String, Object>> finalItems = new ArrayList<>(sponsoredItems);
        java.util.Set<String> seenIds = sponsoredItems.stream()
                .map(it -> it.get("searchType") + "_" + it.get("id"))
                .collect(java.util.stream.Collectors.toSet());

        for (Map<String, Object> it : items) {
            String key = it.get("searchType") + "_" + it.get("id");
            if (!seenIds.contains(key)) {
                finalItems.add(it);
                seenIds.add(key);
            }
        }
        items = finalItems;

        // 7. Ordenación Global Unificada
        items.sort((a, b) -> {
            try {
                if ("precio_asc".equalsIgnoreCase(orden)) {
                    double pa = Double.parseDouble(a.get("precio").toString());
                    double pb = Double.parseDouble(b.get("precio").toString());
                    return Double.compare(pa, pb);
                } else if ("precio_desc".equalsIgnoreCase(orden)) {
                    double pa = Double.parseDouble(a.get("precio").toString());
                    double pb = Double.parseDouble(b.get("precio").toString());
                    return Double.compare(pb, pa);
                } else {
                    // Por defecto: Fecha Publicación Descendente
                    Object fa = a.get("fechaPublicacion");
                    Object fb = b.get("fechaPublicacion");
                    if (fa instanceof java.time.LocalDateTime && fb instanceof java.time.LocalDateTime) {
                        return ((java.time.LocalDateTime) fb).compareTo((java.time.LocalDateTime) fa);
                    }
                    return 0;
                }
            } catch (Exception e) {
                return 0;
            }
        });

        long totalCount = paginaOfertas.getTotalElements() + paginaProductos.getTotalElements()
                + paginaVehiculos.getTotalElements();

        return Map.of(
                "items", items,
                "total", totalCount,
                "page", pageable.getPageNumber(),
                "size", pageable.getPageSize());
    }

    private List<Map<String, Object>> fetchSponsoredItems(String q, String tipo, List<String> categorySlugs, Double precioMin, Double precioMax,
                                                           Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<Map<String, Object>> sponsored = new ArrayList<>();
        
        // Si se pide específicamente algo que no sea productos, no devolvemos patrocinados (que son productos)
        if (tipo != null && !"TODOS".equalsIgnoreCase(tipo) && !"PRODUCTO".equalsIgnoreCase(tipo)) {
            return sponsored;
        }

        try {
            // A. De Contratos Activos
            List<com.nexus.entity.Contrato> contratos = contratoRepository.findActiveSponsoredProducts();
            for (com.nexus.entity.Contrato c : contratos) {
                if (c.getProductoId() != null) {
                    productoRepository.findById(c.getProductoId()).ifPresent(p -> {
                        if (p.getEstado() == com.nexus.entity.EstadoProducto.DISPONIBLE) {
                            // Filtro de precio (¡CRÍTICO!)
                            double precio = p.getPrecio() != null ? p.getPrecio() : 0.0;
                            if ((precioMin == null || precio >= precioMin) && (precioMax == null || precio <= precioMax)) {
                                // Filtro básico de query si existe
                                if (q == null || q.isBlank() || p.getTitulo().toLowerCase().contains(q.toLowerCase())) {
                                    // Filtro de categoría si existe
                                    boolean matchCat = categorySlugs.isEmpty() || (p.getCategoria() != null && categorySlugs.contains(p.getCategoria().getSlug()));
                                    if (matchCat) {
                                        // Filtro geográfico
                                        boolean inRange = true;
                                        if (minLat != null && p.getLatitude() != null && (p.getLatitude() < minLat || p.getLatitude() > maxLat)) inRange = false;
                                        if (minLng != null && p.getLongitude() != null && (p.getLongitude() < minLng || p.getLongitude() > maxLng)) inRange = false;
                                        
                                        if (inRange) {
                                            sponsored.add(transformToMap(p));
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            }

            // B. De flag 'patrocinado'
            List<Producto> pPatrocinados = productoRepository.findByEstado(com.nexus.entity.EstadoProducto.DISPONIBLE);
            for (Producto p : pPatrocinados) {
                if (p.getPatrocinado()) {
                     if (sponsored.size() < 5) { // Un máximo de 5 patrocinados por flag
                         boolean alreadyIn = sponsored.stream().anyMatch(it -> it.get("id").equals(p.getId()) && "PRODUCTO".equals(it.get("searchType")));
                         if (!alreadyIn) {
                             // Filtro de precio
                             double precio = p.getPrecio() != null ? p.getPrecio() : 0.0;
                             if ((precioMin == null || precio >= precioMin) && (precioMax == null || precio <= precioMax)) {
                                 if (q == null || q.isBlank() || p.getTitulo().toLowerCase().contains(q.toLowerCase())) {
                                     boolean matchCat = categorySlugs.isEmpty() || (p.getCategoria() != null && categorySlugs.contains(p.getCategoria().getSlug()));
                                     if (matchCat) {
                                         sponsored.add(transformToMap(p));
                                     }
                                 }
                             }
                         }
                     }
                }
            }
        } catch (Exception e) {
            // Log error
        }
        return sponsored;
    }

    private void collectHijosSlugs(com.nexus.entity.Categoria parent, List<String> slugs) {
        if (parent.getHijos() != null) {
            for (com.nexus.entity.Categoria hijo : parent.getHijos()) {
                slugs.add(hijo.getSlug());
                collectHijosSlugs(hijo, slugs);
            }
        }
    }

    private Map<String, Object> transformToMap(Producto p) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", p.getId());
        m.put("titulo", p.getTitulo());
        m.put("imagenPrincipal", p.getImagenPrincipal());
        m.put("precio", p.getPrecio() != null ? p.getPrecio() : 0);
        m.put("fechaPublicacion", p.getFechaPublicacion());
        m.put("searchType", "PRODUCTO");
        m.put("sponsored", true);
        if (p.getCategoria() != null) {
            Map<String, Object> catMap = new java.util.HashMap<>();
            catMap.put("nombre", p.getCategoria().getNombre());
            catMap.put("slug", p.getCategoria().getSlug());
            m.put("categoria", catMap);
        }
        if (p.getVendedor() != null) {
            Map<String, Object> vendMap = new java.util.HashMap<>();
            vendMap.put("nombre", p.getVendedor().getNombre() != null ? p.getVendedor().getNombre() : "Usuario");
            vendMap.put("verificado", p.getVendedor().isCuentaVerificada());
            m.put("vendedor", vendMap);
        }
        return m;
    }
}
