package com.nexus.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.dto.FavoritoDTO;
import com.nexus.entity.Favorito;
import com.nexus.service.FavoritoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "Favoritos", description = "Gestión de favoritos del usuario")
public class FavoritoController {

    @Autowired
    private FavoritoService favoritoService;

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Ver favoritos de un usuario")
    public ResponseEntity<List<FavoritoDTO>> listar(@PathVariable Integer usuarioId) {
        // Ahora devuelve FavoritoDTO en lugar de la entidad Favorito
        return ResponseEntity.ok(favoritoService.obtenerPorUsuario(usuarioId));
    }
    
    
    @PostMapping("/oferta/{usuarioId}/{ofertaId}")
    @Operation(summary = "Guardar oferta como favorita")
    public ResponseEntity<?> guardarOferta(
            @PathVariable Integer usuarioId,
            @PathVariable Integer ofertaId) {
        try {
            Favorito favorito = favoritoService.guardarOferta(usuarioId, ofertaId);
            return ResponseEntity.status(HttpStatus.CREATED).body(favorito);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/producto/{usuarioId}/{productoId}")
    @Operation(summary = "Guardar producto en favoritos")
    public ResponseEntity<?> guardarProducto(
            @PathVariable Integer usuarioId,
            @PathVariable Integer productoId) {
            
        // Tu lógica actual de guardado (seguramente llame a un servicio)
        favoritoService.guardarProducto(usuarioId, productoId); 
        
        // 🔥 EL CAMBIO ESTÁ AQUÍ: Devolvemos un JSON simple, no la Entidad completa
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Favorito guardado correctamente"));
    }
    
    @PostMapping("/vehiculo/{usuarioId}/{vehiculoId}")
    @Operation(summary = "Guardar vehículo en favoritos")
    public ResponseEntity<?> guardarVehiculo(
            @PathVariable Integer usuarioId,
            @PathVariable Integer vehiculoId) {
        favoritoService.guardarVehiculo(usuarioId, vehiculoId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Vehículo guardado correctamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar favorito")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        favoritoService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Favorito eliminado"));
    }
    

    @DeleteMapping("/vehiculo/{usuarioId}/{vehiculoId}")
    @Operation(summary = "Eliminar vehículo de favoritos")
    public ResponseEntity<?> eliminarPorVehiculo(
            @PathVariable Integer usuarioId,
            @PathVariable Integer vehiculoId) {
        favoritoService.eliminarPorUsuarioYVehiculo(usuarioId, vehiculoId);
        return ResponseEntity.ok(Map.of("mensaje", "Vehículo eliminado de favoritos"));
    }

    @DeleteMapping("/producto/{usuarioId}/{productoId}")
    @Operation(summary = "Eliminar producto de favoritos")
    public ResponseEntity<?> eliminarPorProducto(
            @PathVariable Integer usuarioId,
            @PathVariable Integer productoId) {
        favoritoService.eliminarPorUsuarioYProducto(usuarioId, productoId);
        return ResponseEntity.ok(Map.of("mensaje", "Favorito eliminado del producto"));
    }

    @DeleteMapping("/oferta/{usuarioId}/{ofertaId}")
    @Operation(summary = "Eliminar oferta de favoritos")
    public ResponseEntity<?> eliminarPorOferta(
            @PathVariable Integer usuarioId,
            @PathVariable Integer ofertaId) {
        favoritoService.eliminarPorUsuarioYOferta(usuarioId, ofertaId);
        return ResponseEntity.ok(Map.of("mensaje", "Favorito eliminado de la oferta"));
    }
}