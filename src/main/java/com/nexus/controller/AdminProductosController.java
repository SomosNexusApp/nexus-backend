package com.nexus.controller;

import com.nexus.entity.Producto;
import com.nexus.service.AdminProductosService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/productos")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminProductosController {

    private final AdminProductosService service;

    public AdminProductosController(AdminProductosService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Producto>> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer vendedorId,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.buscar(q, categoriaId, estado, vendedorId,
                precioMin, precioMax, fechaDesde,
                PageRequest.of(page, size, Sort.by("fechaPublicacion").descending())));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<Producto> editar(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.editar(id, body));
    }

    @PatchMapping("/{id}/pausar")
    @Transactional
    public ResponseEntity<Producto> pausar(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> body) {
        String motivo = (String) body.get("motivo");
        Integer horas = ((Number) body.get("duracionHoras")).intValue();
        return ResponseEntity.ok(service.pausar(id, motivo, horas));
    }

    @PatchMapping("/{id}/reactivar")
    @Transactional
    public ResponseEntity<Producto> reactivar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.reactivar(id));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id,
                                          @RequestBody(required = false) Map<String, Object> body) {
        String motivo = body != null ? (String) body.get("motivo") : "Sin motivo indicado";
        service.eliminar(id, motivo);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/destacar")
    @Transactional
    public ResponseEntity<Producto> toggleDestacado(@PathVariable Integer id) {
        return ResponseEntity.ok(service.toggleDestacado(id));
    }
}
