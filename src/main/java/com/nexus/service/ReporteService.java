package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.*;
import com.nexus.repository.*;

/**
 * Firmas exactas requeridas por ReporteController:
 *
 *   reportar(Integer reportadorId, TipoReporte tipo,
 *            MotivoReporte motivo, String descripcion, Integer objetoId)  line 32
 *
 *   resolver(Integer reporteId, EstadoReporte estado, String resolucion)  line 70
 *     <- SIN adminId (3 args, no 4)
 *
 *   getMisReportes(Integer actorId)   line 50
 *   getPendientes()                   line 58
 */
@Service
public class ReporteService {

    @Autowired private ReporteRepository    reporteRepository;
    @Autowired private ActorRepository      actorRepository;
    @Autowired private ProductoRepository   productoRepository;
    @Autowired private OfertaRepository     ofertaRepository;
    @Autowired private VehiculoRepository   vehiculoRepository;
    @Autowired private ComentarioRepository comentarioRepository;
    @Autowired private MensajeRepository    mensajeRepository;
    @Autowired private NotificacionService  notificacionService;

    public List<Reporte> findAll()             { return reporteRepository.findAll(); }
    public Optional<Reporte> findById(Integer id) { return reporteRepository.findById(id); }

    /** ReporteController line 33: findByEstado */
    public List<Reporte> findPendientes()      { return reporteRepository.findByEstado(EstadoReporte.PENDIENTE); }

    /** ReporteController line 58: getPendientes() */
    public List<Reporte> getPendientes()       { return findPendientes(); }

    /** ReporteController line 34: findByTipo */
    public List<Reporte> findByTipo(TipoReporte tipo) { return reporteRepository.findByTipo(tipo); }

    /** ReporteController line 50: getMisReportes(actorId) */
    public List<Reporte> getMisReportes(Integer actorId) {
        return reporteRepository.findByReportadorId(actorId);
    }

    /**
     * ReporteController line 32:
     *   reportar(Integer reportadorId, TipoReporte tipo,
     *            MotivoReporte motivo, String descripcion, Integer objetoId)
     *
     * motivo es MotivoReporte (enum), no String.
     */
    @Transactional
    public Reporte reportar(Integer reportadorId, TipoReporte tipo,
                             MotivoReporte motivo, String descripcion, Integer objetoId) {

        Actor reportador = actorRepository.findById(reportadorId)
            .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));

        Reporte r = new Reporte();
        r.setReportador(reportador);
        r.setTipo(tipo);
        r.setMotivo(motivo);
        r.setDescripcion(descripcion);
        r.setEstado(EstadoReporte.PENDIENTE);
        r.setFecha(LocalDateTime.now());

        // Switch exhaustivo -- todos los valores de TipoReporte cubiertos
        switch (tipo) {
            case USUARIO -> {
                actorRepository.findById(objetoId)
                    .ifPresent(r::setActorDenunciado);
            }
            case PRODUCTO -> {
                productoRepository.findById(objetoId)
                    .ifPresent(r::setProductoDenunciado);
            }
            case OFERTA -> {
                ofertaRepository.findById(objetoId)
                    .ifPresent(r::setOfertaDenunciada);
            }
            case VEHICULO -> {
                // Reporte.setVehiculoDenunciado(Vehiculo) existe en la entidad
                vehiculoRepository.findById(objetoId)
                    .ifPresent(r::setVehiculoDenunciado);
            }
            case MENSAJE -> {
                // Reporte.setMensajeDenunciado(Mensaje) existe en la entidad
                mensajeRepository.findById(objetoId)
                    .ifPresent(r::setMensajeDenunciado);
            }
            case COMENTARIO -> {
                // Reporte.setComentarioDenunciado(Comentario) existe en la entidad
                comentarioRepository.findById(objetoId)
                    .ifPresent(r::setComentarioDenunciado);
            }
        }

        return reporteRepository.save(r);
    }

    /**
     * Alias para compatibilidad con el metodo 'crear' anterior.
     */
    @Transactional
    public Reporte crear(Integer reportadorId, Integer objetoId,
                          TipoReporte tipo, String motivoStr, String descripcion) {
        MotivoReporte motivo;
        try { motivo = MotivoReporte.valueOf(motivoStr); }
        catch (Exception e) { motivo = MotivoReporte.OTRO; }
        return reportar(reportadorId, tipo, motivo, descripcion, objetoId);
    }

    /**
     * ReporteController line 70:
     *   resolver(Integer reporteId, EstadoReporte estado, String resolucion)
     *   3 argumentos -- SIN adminId.
     */
    @Transactional
    public Reporte resolver(Integer reporteId, EstadoReporte estado, String resolucion) {
        Reporte r = reporteRepository.findById(reporteId)
            .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        r.setEstado(estado);
        r.setResolucion(resolucion);
        r.setFechaResolucion(LocalDateTime.now());
        Reporte g = reporteRepository.save(r);
        notificarResolucionReporte(g);
        return g;
    }

    /**
     * Overload con adminId (compatibilidad).
     */
    @Transactional
    public Reporte resolver(Integer reporteId, Integer adminId,
                             EstadoReporte estado, String resolucion) {
        Reporte r = reporteRepository.findById(reporteId)
            .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        r.setEstado(estado);
        r.setResolucion(resolucion);
        r.setFechaResolucion(LocalDateTime.now());
        actorRepository.findById(adminId).ifPresent(r::setResoltor);
        Reporte g = reporteRepository.save(r);
        notificarResolucionReporte(g);
        return g;
    }

    /** Llamado también desde AdminPanelController al actualizar un reporte. */
    public void notificarResolucionReporte(Reporte r) {
        if (r.getReportador() == null) return;
        String detalle = r.getResolucion() != null ? r.getResolucion() : "";
        notificacionService.notificarAccionAdmin(r.getReportador().getId(),
                "Resolución de tu reporte",
                "Estado: " + (r.getEstado() != null ? r.getEstado().name() : "") + ". " + detalle,
                null);
    }

    @Transactional
    public void eliminar(Integer id) { reporteRepository.deleteById(id); }
}