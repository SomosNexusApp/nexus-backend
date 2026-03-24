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

import com.nexus.entity.TipoCombustible;
import com.nexus.entity.TipoVehiculo;
import com.nexus.entity.Vehiculo;
import com.nexus.service.StorageService;
import com.nexus.service.VehiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/vehiculo")
@Tag(name = "Vehículos", description = "Compraventa de vehículos")
public class VehiculoController {

    @Autowired private VehiculoService vehiculoService;
    @Autowired private StorageService storageService;

    @GetMapping
    public ResponseEntity<List<Vehiculo>> findAll()                          { return ResponseEntity.ok(vehiculoService.findAll()); }
    @GetMapping("/disponibles")
    public ResponseEntity<List<Vehiculo>> findDisponibles()                  { return ResponseEntity.ok(vehiculoService.findDisponibles()); }
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Vehiculo>> findByTipo(@PathVariable TipoVehiculo tipo) { return ResponseEntity.ok(vehiculoService.findByTipo(tipo)); }
    @GetMapping("/marcas")
    public ResponseEntity<List<String>> getMarcas()                          { return ResponseEntity.ok(vehiculoService.getMarcasDisponibles()); }
    @GetMapping("/usuario/{uid}")
    public ResponseEntity<List<Vehiculo>> byUsuario(@PathVariable Integer uid) { return ResponseEntity.ok(vehiculoService.getVehiculosDeUsuario(uid)); }

    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> findById(@PathVariable Integer id) {
        return vehiculoService.findById(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam(required = false) TipoVehiculo tipo,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) Integer anioMin,
            @RequestParam(required = false) Integer anioMax,
            @RequestParam(required = false) Integer kmMax,
            @RequestParam(required = false) TipoCombustible combustible,
            @RequestParam(required = false) String cambio,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Integer potenciaMin,
            @RequestParam(required = false) Integer cilindradaMin,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer numeroPuertas,
            @RequestParam(required = false) Integer plazas,
            @RequestParam(required = false) Boolean garantia,
            @RequestParam(required = false) Boolean itv,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
            
        org.springframework.data.domain.Sort sort = getSort(orden);

        Page<Vehiculo> r = vehiculoService.buscarPaginado(tipo, marca, modelo,
                precioMin, precioMax, anioMin, anioMax, kmMax, combustible,
                cambio, busqueda, potenciaMin, cilindradaMin, color, numeroPuertas, plazas,
                garantia, itv, PageRequest.of(page, size, sort));
                
        return ResponseEntity.ok(Map.of(
                "contenido", r.getContent(), 
                "paginaActual", r.getNumber(),
                "totalPaginas", r.getTotalPages(), 
                "totalElementos", r.getTotalElements()));
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

    @PostMapping(value = "/publicar/{usuarioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publicar vehículo con imágenes")
    public ResponseEntity<Object> publicar(
            @RequestPart("vehiculo") Vehiculo vehiculo,
            @RequestPart("imagenPrincipal") MultipartFile img,
            @RequestPart(value = "galeria", required = false) List<MultipartFile> galeria,
            @PathVariable Integer usuarioId) {
        try {
            String url = storageService.subirImagen(img);
            if (url == null) return ResponseEntity.internalServerError().body("Error al subir imagen");
            vehiculo.setImagenPrincipal(url);
            if (galeria != null) for (int i = 0; i < Math.min(galeria.size(), 5); i++) {
                String g = storageService.subirImagen(galeria.get(i)); if (g != null) vehiculo.addImagenGaleria(g);
            }
            Vehiculo nuevo = vehiculoService.publicar(vehiculo, usuarioId);
            return nuevo != null ? ResponseEntity.status(HttpStatus.CREATED).body(nuevo)
                                 : ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado"));
        } catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> update(@PathVariable Integer id,
            @RequestPart("vehiculo") Vehiculo d,
            @RequestPart(value = "imagenPrincipal", required = false) MultipartFile img,
            @RequestPart(value = "galeria", required = false) List<MultipartFile> galeria) {
        try {
            Optional<Vehiculo> ov = vehiculoService.findById(id);
            if (ov.isEmpty()) return ResponseEntity.notFound().build();
            if (img != null && !img.isEmpty()) {
                String url = storageService.subirImagen(img);
                if (url != null) { storageService.eliminarImagen(ov.get().getImagenPrincipal()); d.setImagenPrincipal(url); }
            }
            if (galeria != null) for (MultipartFile f : galeria) {
                if (ov.get().getGaleriaImagenes().size() < 5) { String g = storageService.subirImagen(f); if (g != null) ov.get().addImagenGaleria(g); }
            }
            return ResponseEntity.ok(vehiculoService.update(id, d));
        } catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Integer id) {
        return vehiculoService.findById(id).map(v -> {
            storageService.eliminarImagen(v.getImagenPrincipal());
            v.getGaleriaImagenes().forEach(storageService::eliminarImagen);
            vehiculoService.delete(id);
            return ResponseEntity.ok((Object) Map.of("mensaje", "Vehículo eliminado"));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}