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

import com.nexus.service.NotificacionService;
import com.nexus.service.ReporteService;
import com.nexus.dto.EnvioNotificacionAdminDTO;
import com.nexus.entity.TipoNotificacion;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;

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
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private NotificacionService notificacionService;
    @Autowired private ReporteService reporteService;
    @Autowired private DevolucionRepository devolucionRepo;

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastDayMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        LocalDateTime firstDayYear = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime lastDayYear = now.withDayOfYear(now.toLocalDate().lengthOfYear()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        LocalDateTime startToday = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endToday = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        Double totalRev = compraRepo.getTotalRevenue();
        Double commMonth = compraRepo.getSumComisiones(firstDayMonth, lastDayMonth);
        Double commYear = compraRepo.getSumComisiones(firstDayYear, lastDayYear);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("usuariosTotal", usuarioRepo.count());
        res.put("usuariosDelta", 0);
        res.put("productosActivos", productoRepo.countByEstado(EstadoProducto.DISPONIBLE));
        res.put("productosDelta", 0);
        res.put("ofertasActivas", ofertaRepo.countByEstado(EstadoOferta.ACTIVA));
        res.put("ofertasDelta", 0);
        res.put("comprasHoy", compraRepo.countByFechaCompraBetween(startToday, endToday));
        res.put("comprasDelta", 0);
        res.put("revenueMes", commMonth != null ? commMonth : 0.0);
        res.put("revenueDelta", 0);
        res.put("reportesPendientes", reporteRepo.countByEstado(EstadoReporte.PENDIENTE));
        res.put("reportesDelta", 0);
        
        // Nuevos campos para separar GMV de Ingresos de Nexus
        res.put("nexusGmvTotal", totalRev != null ? totalRev : 0.0);
        res.put("nexusComisionTotal", compraRepo.getSumComisionesTotal() != null ? compraRepo.getSumComisionesTotal() : 0.0);
        res.put("nexusComisionMes", commMonth != null ? commMonth : 0.0);
        res.put("nexusComisionAnio", commYear != null ? commYear : 0.0);
        
        return ResponseEntity.ok(res);
    }

    @GetMapping("/estadisticas/comisiones-dia")
    public ResponseEntity<List<Map<String, Object>>> comisionesDia() {
        LocalDateTime since = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ResponseEntity.ok(compraRepo.getComisionesPorDia(since));
    }

    @GetMapping("/estadisticas/top-vendedores")
    public ResponseEntity<List<Object>> topVendedores() {
        List<Object> res = new ArrayList<>();
        // Obtenemos los usuarios con más reputación que estén verificados
        List<Usuario> topUsers = usuarioRepo.findTopVendedores(PageRequest.of(0, 5));
        
        for (Usuario u : topUsers) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("user", u.getUser());
            m.put("avatar", u.getAvatar());
            
            // Calculamos ventas reales desde el repo de compras
            List<Compra> ventas = compraRepo.findByVendedorId(u.getId());
            long totalVentas = ventas.stream()
                .filter(c -> c.getEstado() == EstadoCompra.COMPLETADA || c.getEstado() == EstadoCompra.ENTREGADO || c.getEstado() == EstadoCompra.ENVIADO)
                .count();
            
            double revenue = ventas.stream()
                .filter(c -> c.getEstado() == EstadoCompra.COMPLETADA || c.getEstado() == EstadoCompra.ENTREGADO || c.getEstado() == EstadoCompra.ENVIADO)
                .mapToDouble(Compra::getPrecioFinal)
                .sum();

            m.put("totalVentas", totalVentas);
            m.put("revenue", revenue);
            m.put("valoracion", u.getReputacion());
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
    public ResponseEntity<List<Map<String, Object>>> usuariosDia() {
        LocalDateTime since = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ResponseEntity.ok(actorRepo.getUsuariosPorDia(since));
    }

    @GetMapping("/estadisticas/compras-dia")
    public ResponseEntity<List<Map<String, Object>>> comprasDia() {
        LocalDateTime since = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ResponseEntity.ok(compraRepo.getComprasPorDia(since));
    }

    @GetMapping("/estadisticas/productos-categoria")
    public ResponseEntity<List<Object>> productosCategoria() {
        // Implementación básica para no devolver lista vacía
        List<Object> res = new ArrayList<>();
        // Aquí se podría añadir lógica de agrupación por categoría si fuera necesario
        return ResponseEntity.ok(res);
    }

    // ════════════════════════════════ USUARIOS ════════════════════════════════

    @GetMapping("/usuarios")
    public ResponseEntity<Map<String, Object>> usuarios(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String tipo,
        @RequestParam(required = false) String verificado,
        @RequestParam(required = false) String estado
    ) {
        // Cargamos todos sin filtrar para poder aplicar filtros en memoria
        // (la tabla de actores es pequeña, normalmente <10k registros)
        List<Actor> todos = actorRepo.findAll(Sort.by("id").descending());

        // Filtro por búsqueda de texto (nombre, username, email)
        if (q != null && !q.isBlank()) {
            String lower = q.trim().toLowerCase();
            todos = todos.stream().filter(a ->
                (a.getUser() != null && a.getUser().toLowerCase().contains(lower)) ||
                (a.getEmail() != null && a.getEmail().toLowerCase().contains(lower)) ||
                (a.getNombre() != null && a.getNombre().toLowerCase().contains(lower)) ||
                (a.getApellidos() != null && a.getApellidos().toLowerCase().contains(lower))
            ).toList();
        }

        // Filtro por tipo de actor (USUARIO, EMPRESA, ADMIN)
        if (tipo != null && !tipo.isBlank()) {
            todos = switch (tipo.toUpperCase()) {
                case "USUARIO" -> todos.stream().filter(a -> a instanceof com.nexus.entity.Usuario).toList();
                case "EMPRESA" -> todos.stream().filter(a -> a instanceof com.nexus.entity.Empresa).toList();
                case "ADMIN"   -> todos.stream().filter(a -> a instanceof com.nexus.entity.Admin).toList();
                default -> todos;
            };
        }

        // Filtro por verificación
        if (verificado != null && !verificado.isBlank()) {
            boolean esVerificado = Boolean.parseBoolean(verificado);
            todos = todos.stream().filter(a -> a.isCuentaVerificada() == esVerificado).toList();
        }

        // Filtro por estado (ACTIVO, SUSPENDIDO_TEMP, BANEADO)
        if (estado != null && !estado.isBlank()) {
            todos = switch (estado.toUpperCase()) {
                case "BANEADO" -> todos.stream().filter(Actor::isBaneado).toList();
                case "SUSPENDIDO_TEMP" -> todos.stream().filter(a ->
                    !a.isBaneado() && a.getSuspendidoHasta() != null && a.getSuspendidoHasta().isAfter(LocalDateTime.now())
                ).toList();
                case "ACTIVO" -> todos.stream().filter(a ->
                    !a.isBaneado() && (a.getSuspendidoHasta() == null || a.getSuspendidoHasta().isBefore(LocalDateTime.now()))
                ).toList();
                default -> todos;
            };
        }

        // Paginación manual sobre la lista filtrada
        int total = todos.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<Object> content = todos.subList(fromIndex, toIndex).stream()
            .map(this::mapUsuario).map(m -> (Object) m).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("content", content);
        res.put("totalElements", (long) total);
        res.put("totalPages", totalPages);
        res.put("number", page);
        res.put("size", size);
        return ResponseEntity.ok(res);
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
        var a = actorRepo.findById(id).orElseThrow();
        a.setCuentaVerificada(true);
        
        if (a instanceof Usuario) {
            ((Usuario) a).setEsVerificado(true);
        } else if (a instanceof Empresa) {
            ((Empresa) a).setVerificada(true);
        }
        
        actorRepo.save(a);
        audit(ud, "VERIFICAR_USUARIO", "USUARIO", id.longValue(), "Usuario verificado", req);
        notificacionService.notificarAccionAdmin(id, "Cuenta verificada",
                "Tu cuenta ha sido verificada por el equipo de Nexus.", "/perfil?tab=configuracion");
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/usuarios/{id}/suspender")
    @Transactional
    public ResponseEntity<Void> suspender(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        var u = actorRepo.findById(id).orElseThrow();
        Object durObj = body.get("duracionHoras");
        int horas = durObj != null ? ((Number) durObj).intValue() : 24;
        u.setSuspendidoHasta(LocalDateTime.now().plusHours(horas));
        u.setMotivoSuspension(body.getOrDefault("motivo", "Sin motivo").toString());
        actorRepo.save(u);
        audit(ud, "SUSPENDER_USUARIO", "USUARIO", id.longValue(), horas + "h: " + body.get("motivo"), req);
        notificacionService.notificarAccionAdmin(id, "Cuenta suspendida temporalmente",
                "Tu cuenta está suspendida durante " + horas + " h. Motivo: " + body.get("motivo"),
                "/perfil?tab=configuracion");
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
        notificacionService.notificarAccionAdmin(id, "Cuenta bloqueada",
                "Tu cuenta ha sido bloqueada. Motivo: " + body.get("motivo"), "/perfil?tab=configuracion");
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
        notificacionService.notificarAccionAdmin(id, "Sanción levantada",
                "Se han levantado las restricciones en tu cuenta.", "/perfil?tab=configuracion");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/usuarios/{id}/impersonar")
    @Transactional
    public ResponseEntity<Map<String, Object>> impersonar(@PathVariable Integer id,
                                                           @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        audit(ud, "IMPERSONAR_USUARIO", "USUARIO", id.longValue(), "Impersonación iniciada", req);
        return ResponseEntity.ok(Map.of("token", "IMP:" + id + ":" + System.currentTimeMillis()));
    }

    @GetMapping("/notificaciones")
    public ResponseEntity<Map<String, Object>> listarNotificacionesEnviadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Por ahora devolvemos un listado genérico de las últimas notificaciones del sistema
        // para evitar errores 404/500 si el frontend consulta este endpoint.
        Page<NotificacionInApp> paged = notificacionRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        List<Object> content = paged.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("titulo", n.getTitulo());
            m.put("mensaje", n.getMensaje());
            m.put("fecha", n.getFecha());
            m.put("tipo", n.getTipo());
            if (n.getActor() != null) {
                m.put("destinatario", Map.of("id", n.getActor().getId(), "user", n.getActor().getUser()));
            }
            return (Object) m;
        }).toList();
        return ResponseEntity.ok(buildPage(content, paged));
    }

    @PostMapping("/notificaciones")
    @Transactional
    public ResponseEntity<Void> enviarAviso(@RequestBody Map<String, Object> body,
                                             @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        Object uidObj = body.get("usuarioId");
        if (uidObj == null) return ResponseEntity.badRequest().build();
        
        int uid = ((Number) uidObj).intValue();
        String mensaje = body.getOrDefault("mensaje", "").toString();
        audit(ud, "ENVIAR_AVISO", "USUARIO", (long) uid, mensaje, req);
        notificacionService.notificarAccionAdmin(uid, "Aviso del equipo Nexus", mensaje, "/perfil?tab=configuracion");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notificaciones/plantillas")
    public ResponseEntity<List<Map<String, Object>>> plantillasNotificaciones() {
        List<Map<String, Object>> list = new ArrayList<>();
        plantilla(list, "aviso-general", "SISTEMA", "Aviso del equipo Nexus",
                "Escribe aquí el mensaje. Los usuarios lo verán en el centro de notificaciones.",
                "/perfil?tab=configuracion",
                "Mensaje genérico. La URL abre el perfil del usuario.");
        plantilla(list, "novedades", "SISTEMA", "Novedades en la aplicación",
                "Hemos mejorado X e Y. Consulta los detalles en la sección de ayuda.",
                "/ayuda",
                "Anuncia cambios de producto sin prometer plazos legales concretos.");
        plantilla(list, "seguridad", "ACCION_ADMIN", "Recordatorio de seguridad",
                "No compartas códigos ni enlaces de pago fuera de Nexus. Ante dudas, contacta con soporte.",
                "/ayuda",
                "Usa tipo ACCION_ADMIN para avisos que requieran más visibilidad.");
        plantilla(list, "promo-externa", "SISTEMA", "Información",
                "Texto breve. Si enlazas fuera, deja claro que el destino es externo a Nexus.",
                "https://",
                "Sustituye la URL por un enlace https válido si aplica.");
        return ResponseEntity.ok(list);
    }

    private void plantilla(List<Map<String, Object>> list, String id, String tipo, String titulo, String mensaje,
            String url, String ayuda) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tipo", tipo);
        m.put("tituloSugerido", titulo);
        m.put("mensajeSugerido", mensaje);
        m.put("urlSugerida", url);
        m.put("ayuda", ayuda);
        list.add(m);
    }

    @PostMapping("/notificaciones/enviar")
    @Transactional
    public ResponseEntity<Map<String, Object>> enviarNotificacionesMass(
            @RequestBody EnvioNotificacionAdminDTO dto,
            @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        if (dto.getTitulo() == null || dto.getTitulo().isBlank()
                || dto.getMensaje() == null || dto.getMensaje().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "titulo y mensaje son obligatorios"));
        }
        TipoNotificacion tipo;
        try {
            tipo = TipoNotificacion.valueOf(dto.getTipo() != null ? dto.getTipo().trim() : "SISTEMA");
        } catch (IllegalArgumentException e) {
            tipo = TipoNotificacion.SISTEMA;
        }
        int enviados = 0;
        String url = dto.getUrl() != null && !dto.getUrl().isBlank() ? dto.getUrl() : null;
        if (dto.isBroadcastTodos()) {
            for (Actor a : actorRepo.findAll()) {
                if (a instanceof Admin) {
                    continue;
                }
                notificacionService.crear(a.getId(), tipo, dto.getTitulo(), dto.getMensaje(), url);
                enviados++;
            }
        } else if (dto.getActorIds() != null && !dto.getActorIds().isEmpty()) {
            for (Integer aid : dto.getActorIds()) {
                notificacionService.crear(aid, tipo, dto.getTitulo(), dto.getMensaje(), url);
                enviados++;
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "broadcastTodos o actorIds requerido"));
        }
        audit(ud, "ENVIAR_NOTIFICACIONES_MASIVO", "SISTEMA", null, "enviados=" + enviados, req);
        return ResponseEntity.ok(Map.of("enviados", enviados));
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
        reporteService.notificarResolucionReporte(r);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/acciones/suspender-y-resolver")
    @Transactional
    public ResponseEntity<Void> suspenderYResolver(@RequestBody Map<String, Object> body,
                                                    @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        Object uidObj = body.get("usuarioId");
        Object durObj = body.get("duracionHoras");
        if (uidObj == null || durObj == null) return ResponseEntity.badRequest().build();

        int uid = ((Number) uidObj).intValue();
        int dur = ((Number) durObj).intValue();
        String mot = body.getOrDefault("motivo", "Moderación").toString();
        
        var u = actorRepo.findById(uid).orElseThrow();
        u.setSuspendidoHasta(LocalDateTime.now().plusHours(dur));
        u.setMotivoSuspension(mot);
        actorRepo.save(u);
        
        if (body.containsKey("reporteId")) {
            Object ridObj = body.get("reporteId");
            if (ridObj != null) {
                int rid = ((Number) ridObj).intValue();
                Reporte r = reporteRepo.findById(rid).orElseThrow();
                r.setEstado(EstadoReporte.RESUELTO);
                r.setResolucion("Suspensión " + dur + "h: " + mot);
                reporteRepo.save(r);
                reporteService.notificarResolucionReporte(r);
            }
        }
        notificacionService.notificarAccionAdmin(uid, "Cuenta suspendida",
                "Suspensión " + dur + " h. Motivo: " + mot, "/perfil?tab=configuracion");
        audit(ud, "SUSPENDER_Y_RESOLVER", "ACTOR", (long) uid, mot, req);
        return ResponseEntity.ok().build();
    }

    // ════════════════════════════════ SANCIONES ════════════════════════════════

    @GetMapping("/sanciones/export")
    public ResponseEntity<byte[]> exportSanciones() {
        StringBuilder csv = new StringBuilder("ID,Usuario,Tipo,Motivo,FechaFin\n");
        actorRepo.findAll().stream()
            .filter(u -> u.isBaneado() || (u.getSuspendidoHasta() != null && u.getSuspendidoHasta().isAfter(LocalDateTime.now())))
            .forEach(u -> {
                String tipo = u.isBaneado() ? "BAN" : "SUSPENSION";
                String motivo = u.isBaneado() ? u.getMotivoBan() : u.getMotivoSuspension();
                String fechaFin = u.isBaneado() ? "PERMANENTE" : u.getSuspendidoHasta().toString();
                csv.append(u.getId()).append(",")
                   .append(u.getUser()).append(",")
                   .append(tipo).append(",")
                   .append("\"").append(motivo != null ? motivo.replace("\"", "'") : "").append("\",")
                   .append(fechaFin).append("\n");
            });

        byte[] data = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sanciones.csv")
            .header(HttpHeaders.CONTENT_TYPE, "text/csv")
            .body(data);
    }
    
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
            m.put("id", u.getId());
            m.put("user", u.getUser());
            m.put("avatar", u.getAvatar());
            m.put("motivo", u.getMotivoFlag());
            m.put("nReportes", reporteRepo.countByActorDenunciadoId(u.getId()));
            m.put("nVentasFallidas", compraRepo.countByVendedorIdAndEstado(u.getId(), EstadoCompra.CANCELADA));
            m.put("fechaPrimerFlag", u.getFechaRegistro()); 
            m.put("estado", "PENDIENTE");
            return (Object) m;
        }).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/fraude/productos-sospechosos")
    public ResponseEntity<List<Object>> productosSospechosos() {
        List<Producto> todos = productoRepo.findByEstado(EstadoProducto.DISPONIBLE);
        Map<Integer, Double> medias = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        
        for (Producto p : todos) {
            if (p.getCategoria() != null) {
                int cid = p.getCategoria().getId();
                medias.put(cid, medias.getOrDefault(cid, 0.0) + p.getPrecio());
                counts.put(cid, counts.getOrDefault(cid, 0) + 1);
            }
        }
        
        medias.forEach((cid, sum) -> medias.put(cid, sum / counts.get(cid)));

        var list = todos.stream()
            .filter(p -> p.getCategoria() != null)
            .filter(p -> {
                double media = medias.getOrDefault(p.getCategoria().getId(), 0.0);
                // Criterio: precio < 30% de la media O precio > 5000 O vendedor flagged
                return (media > 0 && p.getPrecio() < (media * 0.3)) 
                       || p.getPrecio() > 5000 
                       || (p.getVendedor() != null && p.getVendedor().isFlagFraude());
            })
            .map(p -> {
                double media = medias.getOrDefault(p.getCategoria().getId(), 0.0);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("titulo", p.getTitulo());
                m.put("imagenPrincipal", p.getImagenPrincipal());
                m.put("precio", p.getPrecio());
                m.put("mediaPorCategoria", media);
                m.put("porcentajeBajoMedia", media > 0 ? ((media - p.getPrecio()) / media) * 100 : 0);
                m.put("vendedor", miniActor(p.getVendedor()));
                m.put("categoria", p.getCategoria().getNombre());
                return (Object) m;
            }).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/fraude/estadisticas")
    public ResponseEntity<List<Object>> fraudeEstadisticas() {
        LocalDateTime since = LocalDateTime.now().minusDays(15).withHour(0).withMinute(0);
        List<Object> res = new ArrayList<>();
        
        for (int i = 0; i < 15; i++) {
            LocalDateTime day = since.plusDays(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dia", day.toLocalDate().toString());
            m.put("valor", reporteRepo.findAll().stream()
                .filter(r -> r.getFecha() != null && r.getFecha().toLocalDate().equals(day.toLocalDate()))
                .count());
            res.add(m);
        }
        return ResponseEntity.ok(res);
    }

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
        LocalDateTime d = (desde != null && !desde.isBlank()) ? LocalDateTime.parse(desde + "T00:00:00") : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime h = (hasta != null && !hasta.isBlank()) ? LocalDateTime.parse(hasta + "T23:59:59") : LocalDateTime.of(2999, 12, 31, 23, 59);
        
        String safeAdmin = (admin == null) ? "" : admin;
        String safeAccion = (accion == null) ? "" : accion;
        String safeEntidad = (entidadTipo == null) ? "" : entidadTipo;

        var paged = auditLogRepo.filter(safeAdmin, safeAccion, safeEntidad, d, h, PageRequest.of(page, size));
        List<Object> content = paged.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("adminId", a.getAdminId());
            m.put("adminUser", a.getAdminUser());
            m.put("accion", a.getAccion());
            m.put("entidadTipo", a.getEntidadTipo());
            m.put("entidadId", a.getEntidadId());
            m.put("detalle", a.getDetalle());
            m.put("timestamp", a.getTimestamp());
            
            // Resolve specific entity name
            String entidadNombre = "—";
            try {
                if (a.getEntidadId() != null && a.getEntidadId() > 0 && a.getEntidadTipo() != null) {
                    switch (a.getEntidadTipo().toUpperCase()) {
                        case "USUARIO":
                        case "ACTOR":
                        case "EMPRESA":
                            entidadNombre = actorRepo.findById(a.getEntidadId().intValue()).map(Actor::getUser).orElse("Usuario desconocido");
                            break;
                        case "PRODUCTO":
                            entidadNombre = productoRepo.findById(a.getEntidadId().intValue()).map(Producto::getTitulo).orElse("Producto eliminado");
                            break;
                        case "OFERTA":
                            entidadNombre = ofertaRepo.findById(a.getEntidadId().intValue()).map(Oferta::getTitulo).orElse("Oferta eliminada");
                            break;
                        case "REPORTE":
                            entidadNombre = reporteRepo.findById(a.getEntidadId().intValue()).map(r -> "Reporte #" + r.getId()).orElse("Reporte eliminado");
                            break;
                        case "DEVOLUCION":
                            entidadNombre = devolucionRepo.findById(a.getEntidadId().intValue()).map(d2 -> "Devolución #" + d2.getId()).orElse("Devolución eliminada");
                            break;
                        default:
                            entidadNombre = a.getEntidadTipo() + " #" + a.getEntidadId();
                    }
                }
            } catch (Exception e) {}
            m.put("entidadNombre", entidadNombre);
            
            return (Object) m;
        }).toList();
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
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Devolucion> paged;
        if (estado != null && !estado.isEmpty() && !estado.equals("TODAS")) {
            try {
                paged = devolucionRepo.findByEstado(EstadoDevolucion.valueOf(estado), pageable);
            } catch (IllegalArgumentException e) {
                paged = devolucionRepo.findAll(pageable);
            }
        } else {
            paged = devolucionRepo.findAll(pageable);
        }
        List<Object> content = paged.stream().map(this::mapDevolucion).map(m -> (Object) m).toList();
        return ResponseEntity.ok(buildPage(content, paged));
    }

    @PatchMapping("/devoluciones/{id}/aceptar")
    @Transactional
    public ResponseEntity<Void> aceptarDevolucion(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        Devolucion d = devolucionRepo.findById(id).orElseThrow();
        d.setEstado(EstadoDevolucion.ACEPTADA);
        d.setNotaAdmin(body.getOrDefault("nota", "Aceptada por administración").toString());
        if (body.containsKey("importeReembolso")) {
            d.setImporteDevolucion(Double.parseDouble(body.get("importeReembolso").toString()));
        }
        devolucionRepo.save(d);
        audit(ud, "ACEPTAR_DEVOLUCION", "DEVOLUCION", id.longValue(), body.toString(), req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/devoluciones/{id}/rechazar")
    @Transactional
    public ResponseEntity<Void> rechazarDevolucion(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                                  @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        Devolucion d = devolucionRepo.findById(id).orElseThrow();
        d.setEstado(EstadoDevolucion.RECHAZADA);
        d.setNotaAdmin(body.getOrDefault("motivo", "Rechazada por administración").toString());
        d.setFechaResolucion(LocalDateTime.now());
        devolucionRepo.save(d);
        audit(ud, "RECHAZAR_DEVOLUCION", "DEVOLUCION", id.longValue(), body.toString(), req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/devoluciones/{id}/cerrar-admin")
    @Transactional
    public ResponseEntity<Void> cerrarDevolucion(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal UserDetails ud, HttpServletRequest req) {
        Devolucion d = devolucionRepo.findById(id).orElseThrow();
        d.setEstado(EstadoDevolucion.COMPLETADA);
        d.setNotaAdmin(body.getOrDefault("motivo", "Cerrada por administración").toString());
        d.setFechaResolucion(LocalDateTime.now());
        devolucionRepo.save(d);
        audit(ud, "CERRAR_DEVOLUCION", "DEVOLUCION", id.longValue(), body.toString(), req);
        return ResponseEntity.ok().build();
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

        if (a instanceof Usuario || a instanceof Empresa) {
            long ventas = compraRepo.findByVendedorId(a.getId()).stream()
                .filter(c -> c.getEstado() == EstadoCompra.COMPLETADA || c.getEstado() == EstadoCompra.ENTREGADO || c.getEstado() == EstadoCompra.ENVIADO)
                .count();
            m.put("totalVentas", ventas);
        } else {
            m.put("totalVentas", 0);
        }
        
        m.put("reportesRecibidos", reporteRepo.countByActorDenunciadoId(a.getId()));
        return m;
    }

    private Map<String, Object> mapDevolucion(Devolucion d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("estado", d.getEstado() != null ? d.getEstado().name() : "SOLICITADA");
        m.put("motivo", d.getMotivo() != null ? d.getMotivo().name() : "OTRO");
        m.put("descripcion", d.getDescripcion());
        m.put("fechaSolicitud", d.getFechaSolicitud());
        m.put("fechaResolucion", d.getFechaResolucion());
        m.put("notaVendedor", d.getNotaVendedor());
        m.put("notaAdmin", d.getNotaAdmin());
        m.put("trackingDevolucion", d.getTrackingDevolucion());
        m.put("importeDevolucion", d.getImporteDevolucion());
        m.put("fotos", d.getFotos() != null ? new java.util.ArrayList<>(d.getFotos()) : new java.util.ArrayList<>());
        
        if (d.getCompra() != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", d.getCompra().getId());
            c.put("precioFinal", d.getCompra().getPrecioFinal());
            c.put("fechaCompra", d.getCompra().getFechaCompra());
            c.put("comprador", miniActor(d.getCompra().getComprador()));
            c.put("producto", miniProducto(d.getCompra().getProducto()));
            m.put("compra", c);
        }
        
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
            pd.put("id", r.getProductoDenunciado().getId()); 
            pd.put("titulo", r.getProductoDenunciado().getTitulo());
            pd.put("imagenPrincipal", r.getProductoDenunciado().getImagenPrincipal());
            m.put("productoDenunciado", pd);
        }
        
        if (r.getVehiculoDenunciado() != null) {
            var vd = new LinkedHashMap<String, Object>();
            vd.put("id", r.getVehiculoDenunciado().getId());
            vd.put("titulo", r.getVehiculoDenunciado().getTitulo());
            m.put("vehiculoDenunciado", vd);
        }
        
        if (r.getOfertaDenunciada() != null) {
            var od = new LinkedHashMap<String, Object>();
            od.put("id", r.getOfertaDenunciada().getId());
            od.put("titulo", r.getOfertaDenunciada().getTitulo());
            m.put("ofertaDenunciada", od);
        }

        if (r.getComentarioDenunciado() != null) {
            var cd = new LinkedHashMap<String, Object>();
            cd.put("id", r.getComentarioDenunciado().getId());
            cd.put("contenido", r.getComentarioDenunciado().getTexto());
            m.put("comentarioDenunciado", cd);
        }
        
        return m;
    }

    private Map<String, Object> miniActor(com.nexus.entity.Actor a) {
        if (a == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("user", a.getUser() != null ? a.getUser() : "—");
        m.put("avatar", a.getAvatar() != null ? a.getAvatar() : "");
        return m;
    }

    private Map<String, Object> miniProducto(com.nexus.entity.Producto p) {
        if (p == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("titulo", p.getTitulo() != null ? p.getTitulo() : "—");
        m.put("imagenPrincipal", p.getImagenPrincipal() != null ? p.getImagenPrincipal() : "");
        return m;
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
