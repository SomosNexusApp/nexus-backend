package com.nexus.service;

import com.nexus.entity.*;
import com.nexus.repository.AdminOfertaRepository;
import com.nexus.repository.OfertaRepository;
import com.nexus.repository.ActorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.repository.CategoriaRepository;
import java.time.LocalDateTime;

@Service
@Transactional
public class AdminOfertasService {

    private final AdminOfertaRepository adminRepo;
    private final OfertaRepository ofertaRepo;
    private final NotificacionService notificacionService;
    private final AuditLogService auditLogService;

    private final CategoriaRepository categoriaRepo;
    private final ActorRepository actorRepo;

    public AdminOfertasService(
            AdminOfertaRepository adminRepo,
            OfertaRepository ofertaRepo,
            NotificacionService notificacionService,
            AuditLogService auditLogService,
            CategoriaRepository categoriaRepo,
            ActorRepository actorRepo) {
        this.adminRepo = adminRepo;
        this.ofertaRepo = ofertaRepo;
        this.notificacionService = notificacionService;
        this.auditLogService = auditLogService;
        this.categoriaRepo = categoriaRepo;
        this.actorRepo = actorRepo;
    }

    @Transactional(readOnly = true)
    public Page<Oferta> buscar(String estadoStr, Pageable pageable) {
        EstadoOferta estado = (estadoStr != null && !estadoStr.isBlank())
                ? EstadoOferta.valueOf(estadoStr) : null;
        return adminRepo.buscarAdmin(estado, pageable);
    }

    public Oferta aprobar(Integer id) {
        Oferta o = findOrThrow(id);
        o.setEstado(EstadoOferta.ACTIVA);
        o.setEsActiva(true);
        notificacionService.notificarAccionAdmin(o.getActor().getId(), "Oferta aprobada",
                "Tu oferta \"" + o.getTitulo() + "\" ha sido aprobada y ya es visible.",
                "/ofertas/" + o.getId());
        auditLogService.registrar("OFERTA_APROBADA", id, "admin", null);
        return ofertaRepo.save(o);
    }

    public Oferta rechazar(Integer id, String motivo) {
        Oferta o = findOrThrow(id);
        o.setEstado(EstadoOferta.RECHAZADA);
        o.setEsActiva(false);
        notificacionService.notificarAccionAdmin(o.getActor().getId(), "Oferta rechazada",
                "Tu oferta \"" + o.getTitulo() + "\" ha sido rechazada. Motivo: " + motivo, null);
        auditLogService.registrar("OFERTA_RECHAZADA", id, "admin", "Motivo: " + motivo);
        return ofertaRepo.save(o);
    }

    public Oferta toggleDestacada(Integer id) {
        Oferta o = findOrThrow(id);
        if (!o.getDestacada()) {
            long count = adminRepo.countByDestacadaTrue();
            if (count >= 3) {
                throw new IllegalStateException("Ya hay 3 ofertas destacadas. Quita una antes de añadir otra.");
            }
        }
        o.setDestacada(!o.getDestacada());
        auditLogService.registrar(
                o.getDestacada() ? "OFERTA_DESTACADA" : "OFERTA_QUITADO_DESTACADO",
                id, "admin", null);
        return ofertaRepo.save(o);
    }

    public Oferta crearFlash(FlashOfertaRequest req, String adminUsername) {
        Oferta o = new Oferta();
        o.setTitulo(req.titulo());
        o.setDescripcion(req.descripcion());
        o.setPrecioOferta(req.precioEspecial());
        o.setPrecioOriginal(req.precioOriginal());
        o.setImagenPrincipal(req.imagenPrincipal());
        o.setTienda(req.tienda());
        o.setUrlOferta(req.urlOferta());
        
        if (req.categoriaId() != null) {
            categoriaRepo.findById(req.categoriaId()).ifPresent(o::setCategoria);
        }

        Actor admin = actorRepo.findByUsername(adminUsername).orElseThrow();
        o.setActor(admin);

        o.setEsFlash(true);
        o.setFlashFin(req.flashFin());
        o.setLimiteUnidades(req.limiteUnidades());
        o.setFechaPublicacion(LocalDateTime.now());
        o.setEstado(EstadoOferta.ACTIVA);
        o.setEsActiva(true);
        Oferta saved = ofertaRepo.save(o);
        auditLogService.registrar("OFERTA_FLASH_CREADA", saved.getId(), "admin",
                "Fin: " + req.flashFin());
        return saved;
    }

    public void eliminar(Integer id) {
        Oferta o = findOrThrow(id);
        o.setEstado(EstadoOferta.RECHAZADA);
        o.setEsActiva(false);
        ofertaRepo.delete(o); // O marcado como eliminado según política
        auditLogService.registrar("OFERTA_ELIMINADA", id, "admin", null);
    }

    private Oferta findOrThrow(Integer id) {
        return ofertaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada: " + id));
    }

    // ─── DTO record ──────────────────────────────────────────────────────────
    public record FlashOfertaRequest(
            String titulo,
            String descripcion,
            Double precioEspecial,
            Double precioOriginal,
            LocalDateTime flashInicio,
            LocalDateTime flashFin,
            Integer limiteUnidades,
            String imagenPrincipal,
            String tienda,
            String urlOferta,
            Integer categoriaId
    ) {}
}
