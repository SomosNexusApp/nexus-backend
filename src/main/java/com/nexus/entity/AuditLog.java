package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_admin", columnList = "admin_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_entidad", columnList = "entidad_tipo")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "admin_user", nullable = false)
    private String adminUser;

    @Column(nullable = false)
    private String accion;

    @Column(name = "entidad_tipo")
    private String entidadTipo;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column
    private String ip;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }

    // ---- Getters/Setters ----------------------------------------

    public Long           getId()             { return id; }
    public Long           getAdminId()        { return adminId; }
    public void           setAdminId(Long v)  { this.adminId = v; }
    public String         getAdminUser()      { return adminUser; }
    public void           setAdminUser(String v) { this.adminUser = v; }
    public String         getAccion()         { return accion; }
    public void           setAccion(String v) { this.accion = v; }
    public String         getEntidadTipo()    { return entidadTipo; }
    public void           setEntidadTipo(String v) { this.entidadTipo = v; }
    public Long           getEntidadId()      { return entidadId; }
    public void           setEntidadId(Long v) { this.entidadId = v; }
    public String         getDetalle()        { return detalle; }
    public void           setDetalle(String v) { this.detalle = v; }
    public String         getIp()             { return ip; }
    public void           setIp(String v)     { this.ip = v; }
    public LocalDateTime  getTimestamp()      { return timestamp; }
    public void           setTimestamp(LocalDateTime v) { this.timestamp = v; }
}
