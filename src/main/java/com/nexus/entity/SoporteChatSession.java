package com.nexus.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "soporte_chat_session")
public class SoporteChatSession extends DomainEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String sessionToken;

    /** Usuario logueado, si aplica */
    private Integer usuarioId;

    @Column(nullable = false)
    private boolean humanTakeover = false;

    /** Veces que pidió hablar con persona / agente */
    @Column(nullable = false)
    private int insistenciaAgente = 0;

    @Column(nullable = false)
    private boolean escalacionMostrada = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoporteSessionStatus status = SoporteSessionStatus.OPEN;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    @jakarta.persistence.PreUpdate
    public void touch() {
        this.actualizadoEn = LocalDateTime.now();
    }

    public SoporteChatSession() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = this.creadoEn;
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public boolean isHumanTakeover() { return humanTakeover; }
    public void setHumanTakeover(boolean humanTakeover) { this.humanTakeover = humanTakeover; }

    public int getInsistenciaAgente() { return insistenciaAgente; }
    public void setInsistenciaAgente(int insistenciaAgente) { this.insistenciaAgente = insistenciaAgente; }

    public boolean isEscalacionMostrada() { return escalacionMostrada; }
    public void setEscalacionMostrada(boolean escalacionMostrada) { this.escalacionMostrada = escalacionMostrada; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    public SoporteSessionStatus getStatus() { return status; }
    public void setStatus(SoporteSessionStatus status) { this.status = status; }
}
