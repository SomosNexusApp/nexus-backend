package com.nexus.service;

import com.nexus.entity.*;
import com.nexus.repository.AdminProductoRepository;
import com.nexus.repository.CategoriaRepository;
import com.nexus.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminProductosService {

    private final AdminProductoRepository adminRepo;
    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;
    private final NotificacionService notificacionService;
    private final AuditLogService auditLogService;

    public AdminProductosService(
            AdminProductoRepository adminRepo,
            ProductoRepository productoRepo,
            CategoriaRepository categoriaRepo,
            NotificacionService notificacionService,
            AuditLogService auditLogService) {
        this.adminRepo = adminRepo;
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
        this.notificacionService = notificacionService;
        this.auditLogService = auditLogService;
    }

    // ── Búsqueda ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Producto> buscar(String q, Integer categoriaId, String estadoStr,
                                  Integer vendedorId, Double precioMin, Double precioMax,
                                  String fechaDesde, Pageable pageable) {
        EstadoProducto estado = estadoStr != null && !estadoStr.isBlank()
                ? EstadoProducto.valueOf(estadoStr) : null;
        LocalDateTime desde = parseDateTime(fechaDesde);
        String qNorm = (q != null && !q.isBlank()) ? q : null;
        return adminRepo.buscarAdmin(qNorm, categoriaId, estado, vendedorId, precioMin, precioMax, desde, pageable);
    }

    // ── Editar ───────────────────────────────────────────────────────────────

    public Producto editar(Integer id, Map<String, Object> body) {
        Producto p = findOrThrow(id);
        if (body.containsKey("titulo"))       p.setTitulo((String) body.get("titulo"));
        if (body.containsKey("descripcion"))  p.setDescripcion((String) body.get("descripcion"));
        if (body.containsKey("precio"))       p.setPrecio(((Number) body.get("precio")).doubleValue());
        if (body.containsKey("estado"))       p.setEstado(EstadoProducto.valueOf((String) body.get("estado")));
        if (body.containsKey("categoriaId")) {
            Integer catId = ((Number) body.get("categoriaId")).intValue();
            categoriaRepo.findById(catId).ifPresent(p::setCategoria);
        }
        return productoRepo.save(p);
    }

    // ── Pausar ────────────────────────────────────────────────────────────────

    public Producto pausar(Integer id, String motivo, Integer duracionHoras) {
        Producto p = findOrThrow(id);
        p.setEstado(EstadoProducto.SUSPENDIDO_ADMIN);
        p.setMotivoPausa(motivo);
        p.setPausadoHasta(LocalDateTime.now().plusHours(duracionHoras));
        notificacionService.notificarSistema(p.getVendedor().getId(),
                "Tu producto \"" + p.getTitulo() + "\" ha sido pausado hasta "
                        + p.getPausadoHasta().toLocalDate() + ". Motivo: " + motivo);
        auditLogService.registrar("PRODUCTO_PAUSADO", id, "admin", "Motivo: " + motivo + " | Horas: " + duracionHoras);
        return productoRepo.save(p);
    }

    // ── Reactivar ─────────────────────────────────────────────────────────────

    public Producto reactivar(Integer id) {
        Producto p = findOrThrow(id);
        p.setEstado(EstadoProducto.DISPONIBLE);
        p.setPausadoHasta(null);
        p.setMotivoPausa(null);
        notificacionService.notificarSistema(p.getVendedor().getId(),
                "Tu producto \"" + p.getTitulo() + "\" ha sido reactivado.");
        auditLogService.registrar("PRODUCTO_REACTIVADO", id, "admin", null);
        return productoRepo.save(p);
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    public void eliminar(Integer id, String motivo) {
        Producto p = findOrThrow(id);
        p.setEstado(EstadoProducto.ELIMINADO);
        productoRepo.save(p);
        auditLogService.registrar("PRODUCTO_ELIMINADO", id, "admin", "Motivo: " + motivo);
        notificacionService.notificarSistema(p.getVendedor().getId(),
                "Tu producto \"" + p.getTitulo() + "\" ha sido eliminado. Motivo: " + motivo);
    }

    // ── Destacar toggle ───────────────────────────────────────────────────────

    public Producto toggleDestacado(Integer id) {
        Producto p = findOrThrow(id);
        p.setDestacado(!p.getDestacado());
        auditLogService.registrar(
                p.getDestacado() ? "PRODUCTO_DESTACADO" : "PRODUCTO_QUITADO_DESTACADO", id, "admin", null);
        return productoRepo.save(p);
    }

    // ── Reactivación automática (scheduler) ───────────────────────────────────

    public int reactivarVencidos() {
        List<Producto> vencidos = adminRepo.findByPausadoHastaBeforeAndEstado(
                LocalDateTime.now(), EstadoProducto.SUSPENDIDO_ADMIN);
        for (Producto p : vencidos) {
            p.setEstado(EstadoProducto.DISPONIBLE);
            p.setPausadoHasta(null);
            p.setMotivoPausa(null);
            notificacionService.notificarSistema(p.getVendedor().getId(),
                    "Tu producto \"" + p.getTitulo() + "\" ha sido reactivado automáticamente.");
            productoRepo.save(p);
        }
        return vencidos.size();
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

    private Producto findOrThrow(Integer id) {
        return productoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
    }
}
