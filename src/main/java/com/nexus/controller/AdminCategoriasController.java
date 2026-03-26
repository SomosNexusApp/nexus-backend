package com.nexus.controller;

import com.nexus.entity.Categoria;
import com.nexus.service.AdminCategoriasService;
import com.nexus.service.AdminCategoriasService.CategoriaRequest;
import com.nexus.service.AdminCategoriasService.ReordenarItem;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminCategoriasController {

    private final AdminCategoriasService service;

    public AdminCategoriasController(AdminCategoriasService service) {
        this.service = service;
    }

    /** Devuelve el árbol completo (raíz con hijos). */
    @GetMapping
    public ResponseEntity<List<Categoria>> getArbol() {
        return ResponseEntity.ok(service.getArbolRaiz());
    }

    /** Validación de slug en tiempo real. */
    @GetMapping("/check-slug")
    public ResponseEntity<Map<String, Boolean>> checkSlug(@RequestParam String slug) {
        return ResponseEntity.ok(Map.of("exists", service.checkSlug(slug)));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Categoria> crear(@RequestBody CategoriaRequest req) {
        return ResponseEntity.ok(service.crear(req));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<Categoria> editar(@PathVariable Integer id,
                                             @RequestBody CategoriaRequest req) {
        return ResponseEntity.ok(service.editar(id, req));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @Transactional
    public ResponseEntity<Categoria> toggleActiva(@PathVariable Integer id) {
        return ResponseEntity.ok(service.toggleActiva(id));
    }

    @PatchMapping("/reordenar")
    @Transactional
    public ResponseEntity<Void> reordenar(@RequestBody List<ReordenarItem> items) {
        service.reordenar(items);
        return ResponseEntity.ok().build();
    }
}
