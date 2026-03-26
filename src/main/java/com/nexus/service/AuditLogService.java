package com.nexus.service;

import com.nexus.entity.AuditLog;
import com.nexus.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada ligera sobre AuditLogRepository para que los servicios admin
 * puedan registrar acciones sin depender de AdminPanelController.
 */
@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository repo;

    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void registrar(String accion, Integer entidadId, String adminUser, String detalle) {
        AuditLog log = new AuditLog();
        log.setAccion(accion);
        log.setAdminId(0L);          // 0 = sistema/cron; overrideable si se pasa el id
        log.setAdminUser(adminUser != null ? adminUser : "system");
        log.setEntidadTipo(resolverTipo(accion));
        log.setEntidadId(entidadId != null ? entidadId.longValue() : null);
        log.setDetalle(detalle);
        repo.save(log);
    }

    private String resolverTipo(String accion) {
        if (accion.startsWith("PRODUCTO"))  return "PRODUCTO";
        if (accion.startsWith("OFERTA"))    return "OFERTA";
        if (accion.startsWith("VEHICULO"))  return "VEHICULO";
        if (accion.startsWith("CATEGORIA")) return "CATEGORIA";
        return "SISTEMA";
    }
}
