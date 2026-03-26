package com.nexus.controller;

import com.nexus.entity.Oferta;
import com.nexus.service.AdminOfertasService;
import com.nexus.service.AdminOfertasService.FlashOfertaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/ofertas")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminOfertasController {

    private final AdminOfertasService service;

    public AdminOfertasController(AdminOfertasService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Oferta>> buscar(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.buscar(estado,
                PageRequest.of(page, size, Sort.by("fechaPublicacion").descending())));
    }

    @PatchMapping("/{id}/aprobar")
    @Transactional
    public ResponseEntity<Oferta> aprobar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.aprobar(id));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/rechazar")
    @Transactional
    public ResponseEntity<Oferta> rechazar(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.rechazar(id, (String) body.get("motivo")));
    }

    @PatchMapping("/{id}/destacar")
    @Transactional
    public ResponseEntity<Oferta> toggleDestacada(@PathVariable Integer id) {
        return ResponseEntity.ok(service.toggleDestacada(id));
    }

    @PostMapping("/flash")
    @Transactional
    public ResponseEntity<Oferta> crearFlash(@RequestBody Map<String, Object> body) {
        FlashOfertaRequest req = new FlashOfertaRequest(
                (String) body.get("titulo"),
                (String) body.get("descripcion"),
                body.get("precioEspecial") != null ? ((Number) body.get("precioEspecial")).doubleValue() : null,
                body.get("precioOriginal") != null ? ((Number) body.get("precioOriginal")).doubleValue() : null,
                parseDateTime((String) body.get("flashInicio")),
                parseDateTime((String) body.get("flashFin")),
                body.get("limiteUnidades") != null ? ((Number) body.get("limiteUnidades")).intValue() : null
        );
        return ResponseEntity.ok(service.crearFlash(req));
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.contains("T")) return LocalDateTime.parse(s);
            return java.time.LocalDate.parse(s).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }
}
