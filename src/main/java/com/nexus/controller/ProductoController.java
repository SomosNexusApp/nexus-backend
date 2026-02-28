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
import com.nexus.entity.TipoOferta;
import com.nexus.service.ModerationService;
import com.nexus.service.ProductoService;
import com.nexus.service.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/producto")
@Tag(name = "Productos", description = "Mercado de segunda mano")
public class ProductoController {

    @Autowired private ProductoService productoService;
    @Autowired private StorageService storageService;
    @Autowired private ModerationService moderationService;
    
    @GetMapping
    public ResponseEntity<List<Producto>> findAll() {
        return ResponseEntity.ok(productoService.findAll());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Producto>> findDisponibles() {
        return ResponseEntity.ok(productoService.findDisponibles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> findById(@PathVariable Integer id) {
        return productoService.findById(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Búsqueda paginada con filtros.
     * GET /producto/filtrar?busqueda=iphone&precioMax=500&pagina=0&tamano=20
     */
    @GetMapping("/filtrar")
    @Operation(summary = "Búsqueda paginada con filtros")
    public ResponseEntity<?> filtrar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) TipoOferta tipoOferta,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) Integer publicadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        try {
            Page<Producto> resultado = productoService.buscarConFiltrosPaginado(
                busqueda, tipoOferta, precioMin, precioMax, publicadorId, PageRequest.of(pagina, tamano));
            return ResponseEntity.ok(Map.of(
                "contenido",      resultado.getContent(),
                "paginaActual",   resultado.getNumber(),
                "totalPaginas",   resultado.getTotalPages(),
                "totalElementos", resultado.getTotalElements(),
                "tamano",         resultado.getSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
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
            return ResponseEntity.ok(Map.of("estadoAnterior", anterior, "estadoNuevo", actualizado.getEstadoProducto()));
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
            // --- VALIDACIÓN DE MODERACIÓN (CREAR) ---
            String tituloAValidar = producto.getTitulo() != null ? producto.getTitulo() : "";
            String descAValidar = producto.getDescripcion() != null ? producto.getDescripcion() : "";
            String textoCompleto = tituloAValidar + " " + descAValidar;

            if (!moderationService.esContenidoApropiado(textoCompleto)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El título o descripción contiene lenguaje inapropiado y no cumple las normas de la comunidad."));
            }
            // ----------------------------------------

            String url = storageService.subirImagen(imagenPrincipal);
            if (url == null) return ResponseEntity.internalServerError().body(Map.of("error", "Error al subir imagen principal"));
            producto.setImagenPrincipal(url);
            
            if (galeria != null) {
                for (int i = 0; i < Math.min(galeria.size(), 5); i++) {
                    String g = storageService.subirImagen(galeria.get(i));
                    if (g != null) producto.addImagenGaleria(g);
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
            // --- VALIDACIÓN DE MODERACIÓN (ACTUALIZAR) ---
            String tituloAValidar = detalles.getTitulo() != null ? detalles.getTitulo() : "";
            String descAValidar = detalles.getDescripcion() != null ? detalles.getDescripcion() : "";
            String textoCompleto = tituloAValidar + " " + descAValidar;

            if (!textoCompleto.trim().isEmpty() && !moderationService.esContenidoApropiado(textoCompleto)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El título o descripción contiene lenguaje inapropiado y no cumple las normas de la comunidad."));
            }
            // ---------------------------------------------

            Optional<Producto> op = productoService.findById(id);
            if (op.isEmpty()) return ResponseEntity.notFound().build();
            
            Producto p = op.get();
            if (detalles.getTitulo() != null) p.setTitulo(detalles.getTitulo());
            if (detalles.getDescripcion() != null) p.setDescripcion(detalles.getDescripcion());
            if (detalles.getPrecio() != null) p.setPrecio(detalles.getPrecio());
            if (detalles.getTipoOferta() != null) p.setTipoOferta(detalles.getTipoOferta());
            
            if (imagenPrincipal != null && !imagenPrincipal.isEmpty()) {
                String url = storageService.subirImagen(imagenPrincipal);
                if (url != null) { 
                    storageService.eliminarImagen(p.getImagenPrincipal()); 
                    p.setImagenPrincipal(url); 
                }
            }
            
            if (galeria != null) {
                for (MultipartFile f : galeria) {
                    if (p.getGaleriaImagenes().size() < 5) {
                        String g = storageService.subirImagen(f); 
                        if (g != null) p.addImagenGaleria(g);
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
}