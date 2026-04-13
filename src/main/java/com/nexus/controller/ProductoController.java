package com.nexus.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import com.nexus.service.ProductoService;
import com.nexus.service.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/producto")
@Tag(name = "Productos", description = "Mercado de segunda mano")
public class ProductoController {

    @Autowired
    private ProductoService productoService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private com.nexus.service.BloqueoService bloqueoService;

    @GetMapping
    public ResponseEntity<List<Producto>> findAll() {
        return ResponseEntity.ok(productoService.findAll());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Producto>> findDisponibles() {
        return ResponseEntity.ok(productoService.findDisponibles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> findById(@PathVariable Integer id, @RequestParam(required = false) Integer usuarioId) {
        Optional<Producto> op = productoService.findById(id);
        if (op.isPresent()) {
            Producto p = op.get();
            if (usuarioId != null) {
                Integer vendedorId = p.getVendedor().getId();
                if (bloqueoService.estaBloqueado(usuarioId, vendedorId) || bloqueoService.estaBloqueado(vendedorId, usuarioId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            return ResponseEntity.ok(p);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Búsqueda paginada con filtros.
     * GET /producto/filtrar?busqueda=iphone&precioMax=500&pagina=0&tamano=20
     */
    @GetMapping("/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String condicion,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) Integer vendedorId,
            @RequestParam(required = false) Boolean conEnvio,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) Boolean garantia,
            @RequestParam(required = false) Boolean itv,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Double minLat = null, maxLat = null, minLng = null, maxLng = null;
        if (lat != null && lng != null && radius != null && radius > 0) {
            double deltaLat = radius / 111.1;
            double deltaLng = radius / (111.1 * Math.cos(Math.toRadians(lat)));
            minLat = lat - deltaLat;
            maxLat = lat + deltaLat;
            minLng = lng - deltaLng;
            maxLng = lng + deltaLng;
        }

        org.springframework.data.domain.Sort sort = getSort(orden);

        Page<Producto> r = productoService.buscarConFiltrosPaginado(
                categoria, null, precioMin, precioMax, condicion, busqueda, ubicacion, vendedorId,
                vendedorId != null ? vendedorId : 0, 
                minLat, maxLat, minLng, maxLng,
                PageRequest.of(page, size, sort));

        return ResponseEntity.ok(Map.of(
                "contenido", r.getContent(),
                "totalElementos", r.getTotalElements(),
                "totalPaginas", r.getTotalPages(),
                "paginaActual", r.getNumber()));
    }

    private org.springframework.data.domain.Sort getSort(String orden) {
        if (orden == null) return org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaPublicacion");
        return switch (orden) {
            case "precio_asc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "precio");
            case "precio_desc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "precio");
            case "fecha_asc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "fechaPublicacion");
            case "fecha_desc" -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaPublicacion");
            default -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaPublicacion");
        };
    }

    /**
     * PATCH /producto/{id}/estado — Body: { "estado": "RESERVADO" }
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado (DISPONIBLE, RESERVADO, VENDIDO)")
    public ResponseEntity<?> cambiarEstado(@PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String estadoStr = body.get("estado");
        if (estadoStr == null || estadoStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Campo 'estado' requerido"));
        }
        try {
            EstadoProducto nuevo = EstadoProducto.valueOf(estadoStr.toUpperCase());
            EstadoProducto anterior = productoService.findById(id)
                    .map(Producto::getEstadoProducto).orElse(null);
            Producto actualizado = productoService.cambiarEstado(id, nuevo);
            return ResponseEntity
                    .ok(Map.of("estadoAnterior", anterior, "estadoNuevo", actualizado.getEstadoProducto()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Estado inválido. Valores: DISPONIBLE, RESERVADO, VENDIDO"));
        }
    }

    @PostMapping(value = "/publicar/{usuarioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publicar producto con imagen y galería")
    public ResponseEntity<Object> publicar(
            @RequestPart("producto") Producto producto,
            @RequestPart("imagenPrincipal") MultipartFile imagenPrincipal,
            @RequestPart(value = "galeria", required = false) List<MultipartFile> galeria,
            @PathVariable Integer usuarioId) {
        try {
            // Validation is now handled inside productoService.publicar

            String url = storageService.subirImagen(imagenPrincipal);
            if (url == null)
                return ResponseEntity.internalServerError().body(Map.of("error", "Error al subir imagen principal"));
            producto.setImagenPrincipal(url);

            if (galeria != null) {
                for (int i = 0; i < Math.min(galeria.size(), 5); i++) {
                    String g = storageService.subirImagen(galeria.get(i));
                    if (g != null)
                        producto.addImagenGaleria(g);
                }
            }

            Producto nuevo = productoService.publicar(producto, usuarioId);
            return nuevo != null ? ResponseEntity.status(HttpStatus.CREATED).body(nuevo)
                    : ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Actualizar producto")
    public ResponseEntity<Object> update(
            @PathVariable Integer id,
            @RequestPart("producto") Producto detalles,
            @RequestPart(value = "imagenPrincipal", required = false) MultipartFile imagenPrincipal,
            @RequestPart(value = "galeria", required = false) List<MultipartFile> galeria) {
        try {
            // Validation is now handled inside productoService.update

            Optional<Producto> op = productoService.findById(id);
            if (op.isEmpty())
                return ResponseEntity.notFound().build();

            Producto p = op.get();
            if (detalles.getTitulo() != null) p.setTitulo(detalles.getTitulo());
            if (detalles.getDescripcion() != null) p.setDescripcion(detalles.getDescripcion());
            if (detalles.getPrecio() != null) p.setPrecio(detalles.getPrecio());
            if (detalles.getTipoOferta() != null) p.setTipoOferta(detalles.getTipoOferta());
            if (detalles.getMarca() != null) p.setMarca(detalles.getMarca());
            if (detalles.getModelo() != null) p.setModelo(detalles.getModelo());
            if (detalles.getUbicacion() != null) p.setUbicacion(detalles.getUbicacion());
            if (detalles.getCondicion() != null) p.setCondicion(detalles.getCondicion());
            if (detalles.getPeso() != null) p.setPeso(detalles.getPeso());
            if (detalles.getAdmiteEnvio() != null) p.setAdmiteEnvio(detalles.getAdmiteEnvio());
            if (detalles.getLatitude() != null) p.setLatitude(detalles.getLatitude());
            if (detalles.getLongitude() != null) p.setLongitude(detalles.getLongitude());
            
            if (detalles.getCategoria() != null && detalles.getCategoria().getId() != null) {
                p.setCategoria(detalles.getCategoria());
            }

            if (imagenPrincipal != null && !imagenPrincipal.isEmpty()) {
                String url = storageService.subirImagen(imagenPrincipal);
                if (url != null) {
                    if (p.getImagenPrincipal() != null) storageService.eliminarImagen(p.getImagenPrincipal());
                    p.setImagenPrincipal(url);
                }
            }

            if (galeria != null) {
                for (MultipartFile f : galeria) {
                    if (p.getGaleriaImagenes().size() < 5) {
                        String g = storageService.subirImagen(f);
                        if (g != null)
                            p.addImagenGaleria(g);
                    }
                }
            }

            return ResponseEntity.ok(productoService.update(id, p));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return productoService.findById(id).map(p -> {
            storageService.eliminarImagen(p.getImagenPrincipal());
            p.getGaleriaImagenes().forEach(storageService::eliminarImagen);
            productoService.delete(id);
            return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado"));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/votar")
    @Operation(summary = "Dar Spark o Drip a un producto")
    public ResponseEntity<?> votar(
            @PathVariable Integer id,
            @RequestParam Integer usuarioId,
            @RequestParam Boolean esSpark) {
        try {
            return ResponseEntity.ok(productoService.votarProducto(usuarioId, id, esSpark));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/renovar")
    @Operation(summary = "Reactivar producto caducado (nueva vigencia)")
    public ResponseEntity<?> renovar(@PathVariable Integer id, @RequestParam Integer vendedorId) {
        try {
            return ResponseEntity.ok(productoService.renovar(id, vendedorId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}