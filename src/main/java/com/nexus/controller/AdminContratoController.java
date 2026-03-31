package com.nexus.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.nexus.entity.Contrato;
import com.nexus.service.ContratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/contratos")
@Tag(name = "Admin Contratos", description = "Gestión administrativa de contratos de publicidad")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContratoController {

    @Autowired
    private ContratoService contratoService;

    @GetMapping
    @Operation(summary = "Listar todos los contratos")
    public ResponseEntity<List<Contrato>> findAll() {
        return ResponseEntity.ok(contratoService.findAll());
    }

    @PostMapping("/{empresaId}")
    @Operation(summary = "Enviar propuesta de contrato (presupuesto) a la empresa; notificación in-app")
    public ResponseEntity<Contrato> create(@PathVariable Integer empresaId, @RequestBody Contrato contrato) {
        Contrato nuevo = contratoService.proponerDesdeAdmin(contrato, empresaId);
        if (nuevo == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un contrato existente")
    public ResponseEntity<Contrato> update(@PathVariable Integer id, @RequestBody Contrato contrato) {
        Contrato actualizado = contratoService.update(id, contrato);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un contrato")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        contratoService.delete(id);
        return ResponseEntity.ok().build();
    }
}
