package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "bloqueo",
       uniqueConstraints = @UniqueConstraint(columnNames = {"bloqueador_id", "bloqueado_id"}))
public class Bloqueo extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloqueador_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
        "cuentaEliminada", "cuentaVerificada"})
    private Usuario bloqueador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloqueado_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
        "cuentaEliminada", "cuentaVerificada"})
    private Usuario bloqueado;

    private LocalDateTime fechaBloqueo;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    public Bloqueo() { super(); this.fechaBloqueo = LocalDateTime.now(); }

    public Usuario  getBloqueador()            { return bloqueador; }
    public void     setBloqueador(Usuario b)   { this.bloqueador = b; }
    public Usuario  getBloqueado()             { return bloqueado; }
    public void     setBloqueado(Usuario b)    { this.bloqueado = b; }
    public LocalDateTime getFechaBloqueo()     { return fechaBloqueo; }
    public String   getMotivo()                { return motivo; }
    public void     setMotivo(String m)        { this.motivo = m; }
}