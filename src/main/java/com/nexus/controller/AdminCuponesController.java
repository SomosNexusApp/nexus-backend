package com.nexus.controller;

import com.nexus.entity.Cupon;
import com.nexus.entity.CuponUso;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.CuponRepository;
import com.nexus.service.AdminCuponesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/cupones")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminCuponesController {

    private final AdminCuponesService adminCuponesService;
    private final CuponRepository cuponRepo;
    private final ActorRepository actorRepo;

    public AdminCuponesController(AdminCuponesService adminCuponesService, CuponRepository cuponRepo, ActorRepository actorRepo) {
        this.adminCuponesService = adminCuponesService;
        this.cuponRepo = cuponRepo;
        this.actorRepo = actorRepo;
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
    public Cupon crear(@RequestBody Map<String, Object> body) {
        Cupon cupon = new Cupon();
        cupon.setCodigo((String) body.get("codigo"));
        cupon.setDescripcionInterna((String) body.getOrDefault("descripcionInterna", ""));

        // Tipo
        String tipo = (String) body.get("tipo");
        if (tipo != null) cupon.setTipo(com.nexus.entity.TipoDescuento.valueOf(tipo));

        // Valores numéricos (BigDecimal)
        cupon.setValor(toBigDecimal(body.get("valor")));
        cupon.setValorFijo(toBigDecimal(body.get("valorFijo")));
        cupon.setValorPorcentaje(toBigDecimal(body.get("valorPorcentaje")));
        cupon.setImporteMinimo(toBigDecimal(body.get("importeMinimo")));
        cupon.setTopeMaximo(toBigDecimal(body.get("topeMaximo")));

        // Alcance
        String alcance = (String) body.get("alcance");
        if (alcance != null) cupon.setAlcance(com.nexus.entity.AlcanceCupon.valueOf(alcance));

        // Usuario (for USUARIO scope) — frontend sends { id, user, avatar, ... }
        Object usuarioObj = body.get("usuario");
        if (usuarioObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> uMap = (Map<String, Object>) usuarioObj;
            Object uid = uMap.get("id");
            if (uid != null) {
                int userId = ((Number) uid).intValue();
                actorRepo.findById(userId).ifPresent(cupon::setUsuario);
            }
        }

        cupon.setGrupoObjetivo((String) body.get("grupoObjetivo"));

        // Límites
        cupon.setLimiteUsoTotal(toInteger(body.get("limiteUsoTotal")));
        cupon.setLimiteUsoPorUsuario(toInteger(body.get("limiteUsoPorUsuario")));

        // Fechas
        cupon.setFechaInicio(parseDateTime((String) body.get("fechaInicio")));
        cupon.setFechaFin(parseDateTime((String) body.get("fechaFin")));

        // Categorías
        Object categoriasIds = body.get("categoriasIds");
        if (categoriasIds instanceof String) {
            cupon.setCategoriasIds((String) categoriasIds);
        }

        // Estado
        Object activoObj = body.get("activo");
        if (activoObj instanceof Boolean) cupon.setActivo((Boolean) activoObj);

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

    // ── Helper methods ────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.contains("T")) return LocalDateTime.parse(s);
            return java.time.LocalDate.parse(s).atStartOfDay();
        } catch (Exception e) { return null; }
    }
}
