package com.nexus.controller;

import com.nexus.entity.Cupon;
import com.nexus.entity.CuponUso;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.CuponRepository;
import com.nexus.service.AdminCuponesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
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

    // Formatter that accepts both "HH:mm" and "HH:mm:ss" (datetime-local sends without seconds)
    private static final DateTimeFormatter DT_FLEX = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
            .toFormatter();

    public AdminCuponesController(AdminCuponesService adminCuponesService,
                                   CuponRepository cuponRepo,
                                   ActorRepository actorRepo) {
        this.adminCuponesService = adminCuponesService;
        this.cuponRepo = cuponRepo;
        this.actorRepo = actorRepo;
    }

    // ── GET ──────────────────────────────────────────────────────────────

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

    @GetMapping("/check")
    public Map<String, Boolean> checkCodigo(@RequestParam String codigo) {
        return Map.of("disponible", !cuponRepo.existsByCodigo(codigo.toUpperCase()));
    }

    @GetMapping("/{id}/usos")
    public Page<CuponUso> getUsos(@PathVariable Integer id, Pageable pageable) {
        return adminCuponesService.getUsos(id, pageable);
    }

    // ── POST (crear) ─────────────────────────────────────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Map<String, Object> body) {
        Cupon cupon = buildCuponFromBody(body);
        Cupon saved = adminCuponesService.crear(cupon);

        // Return a minimal safe response — avoids Jackson serializing lazy Actor references
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id",      saved.getId());
        resp.put("codigo",  saved.getCodigo());
        resp.put("activo",  saved.isActivo());
        resp.put("alcance", saved.getAlcance() != null ? saved.getAlcance().name() : null);
        return ResponseEntity.ok(resp);
    }

    // ── PATCH ────────────────────────────────────────────────────────────

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Integer id,
                                                       @RequestBody Map<String, Object> body) {
        Cupon datos = buildCuponFromBody(body);
        Cupon saved = adminCuponesService.editar(id, datos);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id",     saved.getId());
        resp.put("codigo", saved.getCodigo());
        resp.put("activo", saved.isActivo());
        return ResponseEntity.ok(resp);
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

    // ── DELETE ───────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Transactional
    public void eliminar(@PathVariable Integer id) {
        adminCuponesService.eliminar(id);
    }

    // ── Helper: build Cupon from Map ─────────────────────────────────────

    private Cupon buildCuponFromBody(Map<String, Object> body) {
        Cupon cupon = new Cupon();

        cupon.setCodigo((String) body.get("codigo"));
        cupon.setDescripcionInterna((String) body.getOrDefault("descripcionInterna", ""));

        // Tipo de descuento
        String tipo = (String) body.get("tipo");
        if (tipo != null && !tipo.isBlank()) {
            cupon.setTipo(com.nexus.entity.TipoDescuento.valueOf(tipo));
        }

        // Valores numéricos
        cupon.setValor(toBigDecimal(body.get("valor")));
        cupon.setValorFijo(toBigDecimal(body.get("valorFijo")));
        cupon.setValorPorcentaje(toBigDecimal(body.get("valorPorcentaje")));
        cupon.setImporteMinimo(toBigDecimal(body.get("importeMinimo")));
        cupon.setTopeMaximo(toBigDecimal(body.get("topeMaximo")));

        // Alcance
        String alcance = (String) body.get("alcance");
        if (alcance != null && !alcance.isBlank()) {
            cupon.setAlcance(com.nexus.entity.AlcanceCupon.valueOf(alcance));
        }

        // Usuario específico (alcance = USUARIO): frontend envía {id, user, ...}
        Object usuarioObj = body.get("usuario");
        if (usuarioObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> uMap = (Map<String, Object>) usuarioObj;
            Object uid = uMap.get("id");
            if (uid != null) {
                actorRepo.findById(((Number) uid).intValue())
                         .ifPresent(cupon::setUsuario);
            }
        }

        cupon.setGrupoObjetivo((String) body.get("grupoObjetivo"));

        // Límites de uso
        cupon.setLimiteUsoTotal(toInteger(body.get("limiteUsoTotal")));
        cupon.setLimiteUsoPorUsuario(toInteger(body.get("limiteUsoPorUsuario")));

        // Fechas (datetime-local → "yyyy-MM-dd'T'HH:mm" sin segundos)
        cupon.setFechaInicio(parseDateTime(body.get("fechaInicio")));
        cupon.setFechaFin(parseDateTime(body.get("fechaFin")));

        // Categorías: frontend envía null (todas) o "1,2,3"
        Object categoriasIds = body.get("categoriasIds");
        if (categoriasIds instanceof String s && !s.isBlank()) {
            cupon.setCategoriasIds(s);
        }

        // Estado activo
        Object activoObj = body.get("activo");
        if (activoObj instanceof Boolean b) {
            cupon.setActivo(b);
        }

        return cupon;
    }

    // ── Type-conversion helpers ───────────────────────────────────────────

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private LocalDateTime parseDateTime(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        if (s.isBlank()) return null;
        try {
            // Handles both "yyyy-MM-dd'T'HH:mm" and "yyyy-MM-dd'T'HH:mm:ss"
            return LocalDateTime.parse(s, DT_FLEX);
        } catch (Exception e) {
            try {
                // Fallback: date only → start of day
                return java.time.LocalDate.parse(s).atStartOfDay();
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
