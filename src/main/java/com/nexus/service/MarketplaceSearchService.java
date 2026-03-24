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

    public Map<String, Object> buscarTodo(
            String q,
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
        // Construir Sort para los sub-servicios
        org.springframework.data.domain.Sort subSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaPublicacion");
        
        if ("precio_asc".equalsIgnoreCase(orden)) {
            subSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "precio");
        } else if ("precio_desc".equalsIgnoreCase(orden)) {
            subSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "precio");
        }

        PageRequest subPage = PageRequest.of(pageable.getPageNumber(), sizePerType, subSort);

        // Bounding Box (Aproximado)
        Double minLat = null, maxLat = null, minLng = null, maxLng = null;
        if (lat != null && lng != null && radius != null && radius > 0) {
            double deltaLat = radius / 111.1;
            double deltaLng = radius / (111.1 * Math.cos(Math.toRadians(lat)));
            minLat = lat - deltaLat;
            maxLat = lat + deltaLat;
            minLng = lng - deltaLng;
            maxLng = lng + deltaLng;
        }

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

        // 1. Buscar Ofertas
        Page<Oferta> paginaOfertas = ofertaService.buscarConFiltrosGeograficos(
                categoria, null, precioMin, precioMax, q, true, sortOferta, dirOferta, null, usuarioId,
                minLat, maxLat, minLng, maxLng, subPage);

        // 2. Buscar Productos
        Page<Producto> paginaProductos = productoService.buscarConFiltrosPaginado(
                categoria, null, precioMin, precioMax, null, q, ubicacion, null, usuarioId != null ? usuarioId : 0,
                minLat, maxLat, minLng, maxLng, subPage);

        // 3. Buscar Vehículos
        boolean esCategoriaVehiculo = false;
        if (categoria == null || categoria.isBlank()) {
            esCategoriaVehiculo = true;
        } else {
            List<String> slugsMotor = List.of("vehiculos", "coches", "motos", "furgonetas", "caravanas", "otros-vehiculos", "scooters");
            esCategoriaVehiculo = slugsMotor.contains(categoria.toLowerCase());
        }

        Page<Vehiculo> paginaVehiculos = Page.empty();
        if (esCategoriaVehiculo) {
            paginaVehiculos = vehiculoService.buscarPaginadoGeografico(
                    null, null, null, precioMin, precioMax, null, null, null, null, null, q, null, null, null, null, null,
                    null, null,
                    minLat, maxLat, minLng, maxLng, subPage);
        }

        // 4. Transformar y Combinar
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
            return m;
        }).collect(Collectors.toList()));

        // 5. Ordenación Global Unificada
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

        long total = paginaOfertas.getTotalElements() + paginaProductos.getTotalElements()
                + paginaVehiculos.getTotalElements();

        return Map.of(
                "items", items,
                "total", total,
                "page", pageable.getPageNumber(),
                "size", pageable.getPageSize());
    }
}
