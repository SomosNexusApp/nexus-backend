package com.nexus.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Contrato;
import com.nexus.service.ContratoService;
import com.nexus.service.EmpresaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/contratos")
@Tag(name = "Admin Contratos", description = "Gestión administrativa de contratos de publicidad")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContratoController {

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private EmpresaService empresaService;

    // ── DTO interno para evitar serialización circular de la entidad Empresa ──
    public record EmpresaListaDTO(Integer id, String nombreComercial, String cif, String logo) {}

    @GetMapping
    @Operation(summary = "Listar todos los contratos")
    public ResponseEntity<List<Contrato>> findAll() {
        return ResponseEntity.ok(contratoService.findAll());
    }

    /**
     * Endpoint dedicado para el autocomplete del admin: devuelve DTOs ligeros de empresa
     * sin riesgo de serialización circular de JPA.
     */
    @GetMapping("/empresas-lista")
    @Operation(summary = "Lista simplificada de empresas para el selector de contratos (sin riesgo de serialización circular)")
    public ResponseEntity<List<EmpresaListaDTO>> getEmpresasLista() {
        List<EmpresaListaDTO> lista = empresaService.findAll().stream()
                .map(e -> new EmpresaListaDTO(
                        e.getId(),
                        e.getNombreComercial() != null ? e.getNombreComercial() : e.getUser(),
                        e.getCif() != null ? e.getCif() : "",
                        e.getLogo()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{empresaId}")
    @Operation(summary = "Enviar propuesta de contrato (presupuesto) a la empresa; notificación in-app + email")
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
