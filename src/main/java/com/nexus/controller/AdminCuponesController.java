package com.nexus.controller;

import com.nexus.entity.Cupon;
import com.nexus.entity.CuponUso;
import com.nexus.repository.CuponRepository;
import com.nexus.service.AdminCuponesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/cupones")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminCuponesController {

    private final AdminCuponesService adminCuponesService;
    private final CuponRepository cuponRepo;

    public AdminCuponesController(AdminCuponesService adminCuponesService, CuponRepository cuponRepo) {
        this.adminCuponesService = adminCuponesService;
        this.cuponRepo = cuponRepo;
    }

    @GetMapping
    public Page<Cupon> buscar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean caducado,
            Pageable pageable) {
        return adminCuponesService.buscar(activo, caducado, pageable);
    }

    @GetMapping("/stats")
    public AdminCuponesService.CuponStats getStats() {
        return adminCuponesService.getStats();
    }

    @PostMapping
    @Transactional
    public Cupon crear(@RequestBody Cupon cupon) {
        return adminCuponesService.crear(cupon);
    }

    @PatchMapping("/{id}")
    @Transactional
    public Cupon editar(@PathVariable Integer id, @RequestBody Cupon datos) {
        return adminCuponesService.editar(id, datos);
    }

    @PatchMapping("/{id}/desactivar")
    @Transactional
    public void desactivar(@PathVariable Integer id) {
        adminCuponesService.desactivar(id);
    }

    @PatchMapping("/{id}/reactivar")
    @Transactional
    public void reactivar(@PathVariable Integer id) {
        adminCuponesService.reactivar(id);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void eliminar(@PathVariable Integer id) {
        adminCuponesService.eliminar(id);
    }

    @GetMapping("/{id}/usos")
    public Page<CuponUso> getUsos(@PathVariable Integer id, Pageable pageable) {
        return adminCuponesService.getUsos(id, pageable);
    }

    @GetMapping("/check")
    public Map<String, Boolean> checkCodigo(@RequestParam String codigo) {
        return Map.of("disponible", !cuponRepo.existsByCodigo(codigo.toUpperCase()));
    }
}
