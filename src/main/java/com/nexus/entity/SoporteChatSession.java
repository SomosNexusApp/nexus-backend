package com.nexus.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

// -- SoporteChatSession: representa una sesion de chat con el bot/agente de soporte --
// contiene el historial de mensajes (via SoporteChatMessage), el estado de la sesion
// y la encuesta de satisfaccion al cerrar (antes era SoporteEncuesta, ahora esta aqui)
@Entity
@Table(name = "soporte_chat_session")
public class SoporteChatSession extends DomainEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String sessionToken; // token unico para identificar la sesion (UUID)

    /** usuario logueado, null si es anonimo */
    private Integer usuarioId;

    @Column(nullable = false)
    private boolean humanTakeover = false; // true cuando un agente humano toma el control

    /** veces que el usuario ha pedido hablar con una persona */
    @Column(nullable = false)
    private int insistenciaAgente = 0;

    @Column(nullable = false)
    private boolean escalacionMostrada = false; // true si ya se le mostro la opcion de escalar

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoporteSessionStatus status = SoporteSessionStatus.OPEN;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    // ---- encuesta de satisfaccion (antes era la entidad SoporteEncuesta) ----
    // movemos los campos aqui para no tener una tabla extra solo para 2 columnas
    @Column(name = "encuesta_valoracion")
    private Integer encuestaValoracion; // valoracion del 1 al 5, null si no ha respondido

    @Column(name = "encuesta_comentario", columnDefinition = "TEXT")
    private String encuestaComentario; // comentario opcional del usuario sobre el soporte recibido

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

    // getters/setters de la encuesta fusionada
    public Integer getEncuestaValoracion() { return encuestaValoracion; }
    public void setEncuestaValoracion(Integer v) { this.encuestaValoracion = v; }
    public String getEncuestaComentario() { return encuestaComentario; }
    public void setEncuestaComentario(String c) { this.encuestaComentario = c; }
}
