package com.nexus.service;

import com.nexus.entity.*;
import com.nexus.repository.VehiculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AdminVehiculosService {

    private final VehiculoRepository vehiculoRepo;
    private final NotificacionService notificacionService;
    private final AuditLogService auditLogService;

    public AdminVehiculosService(
            VehiculoRepository vehiculoRepo,
            NotificacionService notificacionService,
            AuditLogService auditLogService) {
        this.vehiculoRepo = vehiculoRepo;
        this.notificacionService = notificacionService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<Vehiculo> buscar(String q, String tipo, String estado,
                                  Double precioMin, Double precioMax,
                                  Integer anioMin, Integer kmMax,
                                  Pageable pageable) {
        Specification<Vehiculo> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), like),
                        cb.like(cb.lower(root.get("marca")), like),
                        cb.like(cb.lower(root.get("modelo")), like)
                ));
            }
            if (tipo != null && !tipo.isBlank())
                preds.add(cb.equal(root.get("tipoVehiculo"), TipoVehiculo.valueOf(tipo)));
            if (estado != null && !estado.isBlank())
                preds.add(cb.equal(root.get("estadoVehiculo"), EstadoVehiculo.valueOf(estado)));
            if (precioMin != null)
                preds.add(cb.greaterThanOrEqualTo(root.get("precio"), precioMin));
            if (precioMax != null)
                preds.add(cb.lessThanOrEqualTo(root.get("precio"), precioMax));
            if (anioMin != null)
                preds.add(cb.greaterThanOrEqualTo(root.get("anio"), anioMin));
            if (kmMax != null)
                preds.add(cb.lessThanOrEqualTo(root.get("kilometros"), kmMax));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return vehiculoRepo.findAll(spec, pageable);
    }

    public Vehiculo pausar(Integer id, String motivo) {
        Vehiculo v = findOrThrow(id);
        // Usamos PAUSADO ya que SUSPENDIDO no existe en EstadoVehiculo
        v.setEstadoVehiculo(EstadoVehiculo.PAUSADO);
        notificacionService.notificarSistema(v.getPublicador().getId(),
                "Tu vehículo \"" + v.getTitulo() + "\" ha sido pausado por el administrador. Motivo: " + motivo);
        auditLogService.registrar("VEHICULO_PAUSADO", id, "admin", "Motivo: " + motivo);
        return vehiculoRepo.save(v);
    }

    public Vehiculo reactivar(Integer id) {
        Vehiculo v = findOrThrow(id);
        v.setEstadoVehiculo(EstadoVehiculo.DISPONIBLE);
        notificacionService.notificarSistema(v.getPublicador().getId(),
                "Tu vehículo \"" + v.getTitulo() + "\" ha sido reactivado.");
        auditLogService.registrar("VEHICULO_REACTIVADO", id, "admin", null);
        return vehiculoRepo.save(v);
    }

    public void eliminar(Integer id, String motivo) {
        Vehiculo v = findOrThrow(id);
        v.setEstadoVehiculo(EstadoVehiculo.ELIMINADO);
        vehiculoRepo.save(v);
        auditLogService.registrar("VEHICULO_ELIMINADO", id, "admin", "Motivo: " + motivo);
        notificacionService.notificarSistema(v.getPublicador().getId(),
                "Tu vehículo \"" + v.getTitulo() + "\" ha sido eliminado. Motivo: " + motivo);
    }

    private Vehiculo findOrThrow(Integer id) {
        return vehiculoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado: " + id));
    }
}
