package com.nexus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "soporte_encuesta")
public class SoporteEncuesta extends DomainEntity {

    @Column(nullable = false)
    private Integer sessionId;

    @Column(nullable = false)
    private int valoracion; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comentario;

    public SoporteEncuesta() {}

    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }

    public int getValoracion() { return valoracion; }
    public void setValoracion(int valoracion) { this.valoracion = valoracion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
