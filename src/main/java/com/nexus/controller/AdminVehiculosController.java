package com.nexus.controller;

import com.nexus.entity.Vehiculo;
import com.nexus.service.AdminVehiculosService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/vehiculos")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminVehiculosController {

    private final AdminVehiculosService service;

    public AdminVehiculosController(AdminVehiculosService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Vehiculo>> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) Integer anioMin,
            @RequestParam(required = false) Integer kmMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.buscar(q, tipo, estado, precioMin, precioMax, anioMin, kmMax,
                PageRequest.of(page, size, Sort.by("fechaPublicacion").descending())));
    }

    @PatchMapping("/{id}/pausar")
    @Transactional
    public ResponseEntity<Vehiculo> pausar(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.pausar(id, (String) body.get("motivo")));
    }

    @PatchMapping("/{id}/reactivar")
    @Transactional
    public ResponseEntity<Vehiculo> reactivar(@PathVariable Integer id) {
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
}
