package com.nexus.controller;

import com.nexus.entity.*;
import com.nexus.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminPanelController {

    @Autowired private ActorRepository actorRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private ReporteRepository reporteRepo;
    @Autowired private CompraRepository compraRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private OfertaRepository ofertaRepo;
    @Autowired private AuditLogRepository auditLogRepo;

    // ════════════════════════════════ SISTEMA ════════════════════════════════

    @GetMapping("/sistema/health")
    public ResponseEntity<Map<String, Object>> health() {
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long s = uptimeMs / 1000;
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("version", "1.0.0");
        res.put("status", "UP");
        res.put("uptime", String.format("%dh %dm %ds", s / 3600, (s % 3600) / 60, s % 60));
        return ResponseEntity.ok(res);
    }

    // ════════════════════════════════ ESTADÍSTICAS ════════════════════════════

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> kpis() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("usuariosTotal", usuarioRepo.count());
        res.put("usuariosDelta", 0);
        res.put("productosActivos", productoRepo.countByEstado(EstadoProducto.DISPONIBLE));
        res.put("productosDelta", 0);
        res.put("ofertasActivas", ofertaRepo.countByEstado(EstadoOferta.ACTIVA));
        res.put("ofertasDelta", 0);
        res.put("comprasHoy", 0);
        res.put("comprasDelta", 0);
        res.put("revenueMes", 0.0);
        res.put("revenueDelta", 0);
        res.put("reportesPendientes", reporteRepo.countByEstado(EstadoReporte.PENDIENTE));
        res.put("reportesDelta", 0);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/estadisticas/top-vendedores")
    public ResponseEntity<List<Object>> topVendedores() {
        List<Object> res = new ArrayList<>();
        for (Usuario u : usuarioRepo.findTopVendedores(PageRequest.of(0, 5))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId()); m.put("user", u.getUser()); m.put("avatar", u.getAvatar());
            m.put("totalVentas", 0); m.put("revenue", 0.0); m.put("valoracion", u.getReputacion());
            res.add(m);
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/estadisticas/ultimas-compras")
    public ResponseEntity<List<Object>> ultimasCompras() {
        List<Object> res = new ArrayList<>();
        for (var c : compraRepo.findAll(PageRequest.of(0, 10, Sort.by("fechaCompra").descending()))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId()); m.put("precioFinal", c.getPrecioFinal());
            m.put("estado", c.getEstado() != null ? c.getEstado().name() : "COMPLETADA");
            m.put("fechaCompra", c.getFechaCompra());
            m.put("comprador", miniActor(c.getComprador()));
            m.put("producto", miniProducto(c.getProducto()));
            res.add(m);
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/estadisticas/ultimos-reportes")
    public ResponseEntity<List<Object>> ultimosReportes() {
        List<Object> res = new ArrayList<>();
        for (Reporte r : reporteRepo.findAll(PageRequest.of(0, 5, Sort.by("fecha").descending()))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("tipo", r.getTipo() != null ? r.getTipo().name() : "");
            m.put("motivo", r.getMotivo());
            m.put("estado", r.getEstado() != null ? r.getEstado().name() : "");
            m.put("fecha", r.getFecha());
            if (r.getReportador() != null) m.put("reportador", miniActor(r.getReportador()));
            res.add(m);
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/estadisticas/usuarios-dia")
    public ResponseEntity<List<Object>> usuariosDia() { return ResponseEntity.ok(List.of()); }

    @GetMapping("/estadisticas/compras-dia")
    public ResponseEntity<List<Object>> comprasDia() { return ResponseEntity.ok(List.of()); }

    @GetMapping("/estadisticas/productos-categoria")
    public ResponseEntity<List<Object>> productosCategoria() { return ResponseEntity.ok(List.of()); }

    // ════════════════════════════════ USUARIOS ════════════════════════════════

    @GetMapping("/usuarios")
    public ResponseEntity<Map<String, Object>> usuarios(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<Actor> paged = actorRepo.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        List<Object> content = paged.stream().map(this::mapUsuario).map(m -> (Object) m).toList();
        return ResponseEntity.ok(buildPage(content, paged));
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Object> getUsuario(@PathVariable Integer id) {
        return actorRepo.findById(id)
            .map(u -> ResponseEntity.ok((Object) mapUsuario(u)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reportes/{id}/verificar")
    @Transactional
    public ResponseEntity<Void> verificarReporte(@PathVariable Integer id, @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        var r = reporteRepo.findById(id).orElseThrow();
        r.setEstado(EstadoReporte.RESUELTO);
        reporteRepo.save(r);
        audit(ud, "VERIFICAR_REPORTE", "REPORTE", id.longValue(), "Reporte verificado", req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/usuarios/{id}/verificar")
    @Transactional
    public ResponseEntity<Void> verificar(@PathVariable Integer id, @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        var u = actorRepo.findById(id).orElseThrow();
        u.setCuentaVerificada(true);
        actorRepo.save(u);
        audit(ud, "VERIFICAR_USUARIO", "USUARIO", id.longValue(), "Usuario verificado", req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/usuarios/{id}/suspender")
    @Transactional
    public ResponseEntity<Void> suspender(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        var u = actorRepo.findById(id).orElseThrow();
        int horas = Integer.parseInt(body.get("duracionHoras").toString());
        u.setSuspendidoHasta(LocalDateTime.now().plusHours(horas));
        u.setMotivoSuspension(body.get("motivo").toString());
        actorRepo.save(u);
        audit(ud, "SUSPENDER_USUARIO", "USUARIO", id.longValue(), horas + "h: " + body.get("motivo"), req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/usuarios/{id}/banear")
    @Transactional
    public ResponseEntity<Void> banear(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                       @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        var u = actorRepo.findById(id).orElseThrow();
        u.setBaneado(true);
        u.setMotivoBan(body.get("motivo").toString());
        actorRepo.save(u);
        audit(ud, "BANEAR_USUARIO", "USUARIO", id.longValue(), body.get("motivo").toString(), req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/usuarios/{id}/desbanear")
    @Transactional
    public ResponseEntity<Void> desbanear(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        var u = actorRepo.findById(id).orElseThrow();
        if (u instanceof Usuario) {
            ((Usuario) u).setBaneado(false);
            ((Usuario) u).setMotivoBan(null);
        }
        u.setSuspendidoHasta(null);
        u.setMotivoSuspension(null);
        actorRepo.save(u);
        audit(ud, "DESBANEAR_USUARIO", "USUARIO", id.longValue(), body.getOrDefault("motivo", "").toString(), req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/usuarios/{id}/impersonar")
    public ResponseEntity<Map<String, Object>> impersonar(@PathVariable Integer id,
                                                           @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        audit(ud, "IMPERSONAR_USUARIO", "USUARIO", id.longValue(), "Impersonación iniciada", req);
        return ResponseEntity.ok(Map.of("token", "IMP:" + id + ":" + System.currentTimeMillis()));
    }

    @PostMapping("/notificaciones")
    public ResponseEntity<Void> enviarAviso(@RequestBody Map<String, Object> body,
                                             @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        int uid = Integer.parseInt(body.get("usuarioId").toString());
        audit(ud, "ENVIAR_AVISO", "USUARIO", (long) uid, body.getOrDefault("mensaje", "").toString(), req);
        return ResponseEntity.ok().build();
    }

    // ════════════════════════════════ REPORTES ════════════════════════════════

    @GetMapping("/reportes")
    public ResponseEntity<Map<String, Object>> reportes(
        @RequestParam(required = false) String estado,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<Reporte> paged = (estado != null)
            ? reporteRepo.findByEstado(EstadoReporte.valueOf(estado), pageable)
            : reporteRepo.findAll(pageable);
        List<Object> content = paged.stream().map(this::mapReporte).map(m -> (Object) m).toList();
        return ResponseEntity.ok(buildPage(content, paged));
    }

    @GetMapping("/reportes/count-pendientes")
    public ResponseEntity<Map<String, Object>> countPendientes() {
        return ResponseEntity.ok(Map.of("total", reporteRepo.countByEstado(EstadoReporte.PENDIENTE)));
    }

    @PatchMapping("/reportes/{id}")
    @Transactional
    public ResponseEntity<Void> updateReporte(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                               @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        Reporte r = reporteRepo.findById(id).orElseThrow();
        if (body.containsKey("estado")) r.setEstado(EstadoReporte.valueOf(body.get("estado").toString()));
        if (body.containsKey("resolucion")) r.setResolucion(body.get("resolucion").toString());
        reporteRepo.save(r);
        audit(ud, "UPDATE_REPORTE", "REPORTE", r.getId() != null ? r.getId().longValue() : null, body.toString(), req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/acciones/suspender-y-resolver")
    @Transactional
    public ResponseEntity<Void> suspenderYResolver(@RequestBody Map<String, Object> body,
                                                    @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        int uid = Integer.parseInt(body.get("usuarioId").toString());
        int dur = Integer.parseInt(body.get("duracionHoras").toString());
        String mot = body.get("motivo").toString();
        var u = actorRepo.findById(uid).orElseThrow();
        u.setSuspendidoHasta(LocalDateTime.now().plusHours(dur));
        u.setMotivoSuspension(mot);
        actorRepo.save(u);
        if (body.containsKey("reporteId")) {
            int rid = Integer.parseInt(body.get("reporteId").toString());
            Reporte r = reporteRepo.findById(rid).orElseThrow();
            r.setEstado(EstadoReporte.RESUELTO);
            r.setResolucion("Suspensión " + dur + "h: " + mot);
            reporteRepo.save(r);
        }
        audit(ud, "SUSPENDER_Y_RESOLVER", "ACTOR", (long) uid, mot, req);
        return ResponseEntity.ok().build();
    }

    // ════════════════════════════════ SANCIONES ════════════════════════════════

    @GetMapping("/sanciones")
    public ResponseEntity<Map<String, Object>> sanciones(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Page<Actor> paged = actorRepo.findAll(PageRequest.of(page, size));
        List<Object> content = new ArrayList<>();
        for (Actor u : paged) {
            if (!u.isBaneado() && u.getSuspendidoHasta() == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId()); m.put("user", u.getUser()); m.put("avatar", u.getAvatar());
            m.put("tipo", u.isBaneado() ? "BAN" : "SUSPENSION");
            m.put("motivo", u.isBaneado() ? u.getMotivoBan() : u.getMotivoSuspension());
            m.put("fechaFin", u.getSuspendidoHasta()); m.put("activo", true);
            content.add(m);
        }
        return ResponseEntity.ok(buildPage(content, paged));
    }

    // ════════════════════════════════ FRAUDE ════════════════════════════════

    @GetMapping("/fraude/flags")
    public ResponseEntity<List<Object>> fraudeFlags() {
        var list = actorRepo.findAll().stream().filter(Actor::isFlagFraude).map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId()); m.put("user", u.getUser()); m.put("avatar", u.getAvatar());
            m.put("motivo", u.getMotivoFlag()); m.put("nReportes", 0); m.put("nVentasFallidas", 0);
            m.put("estado", "PENDIENTE");
            return (Object) m;
        }).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/fraude/productos-sospechosos")
    public ResponseEntity<List<Object>> productosSospechosos() { return ResponseEntity.ok(List.of()); }

    @GetMapping("/fraude/estadisticas")
    public ResponseEntity<List<Object>> fraudeEstadisticas() { return ResponseEntity.ok(List.of()); }

    @PatchMapping("/fraude/flags/{userId}/revisado")
    public ResponseEntity<Void> marcarRevisado(@PathVariable Integer userId,
                                                @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        var u = actorRepo.findById(userId).orElseThrow();
        u.setFlagFraude(false);
        actorRepo.save(u);
        audit(ud, "FRAUDE_REVISADO", "ACTOR", userId.longValue(), "Flag eliminado", req);
        return ResponseEntity.ok().build();
    }

    // ════════════════════════════════ AUDIT LOG ════════════════════════════════

    @GetMapping("/audit")
    public ResponseEntity<Map<String, Object>> auditLog(
        @RequestParam(required = false) String admin,
        @RequestParam(required = false) String accion,
        @RequestParam(required = false) String entidadTipo,
        @RequestParam(required = false) String desde,
        @RequestParam(required = false) String hasta,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size
    ) {
        LocalDateTime d = desde != null ? LocalDateTime.parse(desde + "T00:00:00") : null;
        LocalDateTime h = hasta != null ? LocalDateTime.parse(hasta + "T23:59:59") : null;
        var paged = auditLogRepo.filter(admin, accion, entidadTipo, d, h, PageRequest.of(page, size));
        List<Object> content = new ArrayList<>(paged.getContent());
        return ResponseEntity.ok(buildPage(content, paged));
    }

    @GetMapping("/audit/export")
    public ResponseEntity<String> exportAudit() {
        var all = auditLogRepo.findAll(Sort.by("timestamp").descending());
        var sb = new StringBuilder("id,admin,accion,entidad,entidadId,detalle,ip,timestamp\n");
        for (AuditLog e : all) {
            sb.append(e.getId()).append(",").append(e.getAdminUser()).append(",").append(e.getAccion())
              .append(",").append(e.getEntidadTipo()).append(",").append(e.getEntidadId())
              .append(",").append(q(e.getDetalle())).append(",").append(e.getIp())
              .append(",").append(e.getTimestamp()).append("\n");
        }
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=audit-log.csv")
            .header("Content-Type", "text/csv; charset=UTF-8")
            .body(sb.toString());
    }

    // ════════════════════════════════ DEVOLUCIONES ════════════════════════════

    @GetMapping("/devoluciones")
    public ResponseEntity<Map<String, Object>> devoluciones(
        @RequestParam(required = false) String estado,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        // Proxy through DevolucionRepository
        return ResponseEntity.ok(buildPage(List.of(), Page.empty()));
    }

    // ════════════════════════════════ HELPERS ════════════════════════════════

    private void audit(UserDetails ud, String accion, String entidad, Long eid, String detalle, HttpServletRequest req) {
        AuditLog a = new AuditLog();
        a.setAccion(accion); a.setEntidadTipo(entidad); a.setEntidadId(eid);
        a.setDetalle(detalle); a.setIp(req.getRemoteAddr());
        if (ud != null) {
            actorRepo.findByUsername(ud.getUsername()).ifPresent(u -> {
                a.setAdminId(u.getId().longValue()); a.setAdminUser(u.getUser());
            });
        }
        // Fallback si no se encontró en ActorRepo (por si acaso)
        if (a.getAdminUser() == null && ud != null) {
            a.setAdminUser(ud.getUsername());
            a.setAdminId(0L); // ID ficticio para evitar el NOT NULL
        }
        auditLogRepo.save(a);
    }

    private Map<String, Object> mapUsuario(Actor a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId()); m.put("user", a.getUser()); m.put("email", a.getEmail());
        m.put("nombre", a.getNombre()); m.put("apellidos", a.getApellidos()); m.put("avatar", a.getAvatar());
        m.put("baneado", a.isBaneado()); m.put("motivoBan", a.getMotivoBan());
        m.put("suspendidoHasta", a.getSuspendidoHasta()); m.put("motivoSuspension", a.getMotivoSuspension());
        m.put("flagFraude", a.isFlagFraude()); m.put("motivoFlag", a.getMotivoFlag());
        m.put("fechaRegistro", a.getFechaRegistro());
        m.put("cuentaVerificada", a.isCuentaVerificada());

        if (a instanceof Usuario) {
            Usuario u = (Usuario) a;
            m.put("rol", "USUARIO");
            m.put("tipoCuenta", u.getTipoCuenta() != null ? u.getTipoCuenta().name() : null);
            m.put("esVerificado", u.isEsVerificado());
            m.put("reputacion", u.getReputacion());
        } else if (a instanceof Empresa) {
            m.put("rol", "EMPRESA");
            m.put("esVerificado", ((Empresa) a).isVerificada());
        } else if (a instanceof Admin) {
            m.put("rol", "ADMIN");
        }

        m.put("totalVentas", 0); m.put("reportesRecibidos", 0);
        return m;
    }

    private Map<String, Object> mapReporte(Reporte r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId()); m.put("tipo", r.getTipo() != null ? r.getTipo().name() : null);
        m.put("motivo", r.getMotivo()); m.put("descripcion", r.getDescripcion());
        m.put("estado", r.getEstado() != null ? r.getEstado().name() : null);
        m.put("fecha", r.getFecha()); m.put("resolucion", r.getResolucion());
        if (r.getReportador() != null) m.put("reportador", miniActor(r.getReportador()));
        if (r.getActorDenunciado() != null) m.put("actorDenunciado", miniActor(r.getActorDenunciado()));
        if (r.getProductoDenunciado() != null) {
            var pd = new LinkedHashMap<String, Object>();
            pd.put("id", r.getProductoDenunciado().getId()); pd.put("titulo", r.getProductoDenunciado().getTitulo());
            pd.put("imagenPrincipal", r.getProductoDenunciado().getImagenPrincipal());
            m.put("productoDenunciado", pd);
        }
        return m;
    }

    private Map<String, Object> miniActor(com.nexus.entity.Actor a) {
        if (a == null) return null;
        return Map.of("id", a.getId(), "user", a.getUser(), "avatar", a.getAvatar() != null ? a.getAvatar() : "");
    }

    private Map<String, Object> miniProducto(com.nexus.entity.Producto p) {
        if (p == null) return null;
        return Map.of("id", p.getId(), "titulo", p.getTitulo(), "imagenPrincipal", p.getImagenPrincipal() != null ? p.getImagenPrincipal() : "");
    }

    private Map<String, Object> buildPage(List<Object> content, Page<?> page) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content", content); m.put("totalElements", page.getTotalElements());
        m.put("totalPages", page.getTotalPages()); m.put("number", page.getNumber()); m.put("size", page.getSize());
        return m;
    }

    private String q(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
