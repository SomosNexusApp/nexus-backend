package com.nexus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
public class Comentario extends DomainEntity {

    @NotBlank
    private String texto;

    private LocalDateTime fecha;
    private Boolean esReportado;

    @ManyToOne
    @JoinColumn(name = "oferta_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "actor", "galeriaImagenes", "descripcion", "galeriaImagenes"})
    private Oferta oferta;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
        "cuentaEliminada", "cuentaVerificada"})
    private Actor actor;

    public Comentario() {
        super();
        this.fecha = LocalDateTime.now();
        this.esReportado = false;
    }

    public Comentario(String texto, Oferta oferta, Actor actor) {
        super();
        this.texto = texto;
        this.oferta = oferta;
        this.actor = actor;
        this.fecha = LocalDateTime.now();
        this.esReportado = false;
    }

    public String getTexto()                               { return texto; }
    public void setTexto(String texto)                     { this.texto = texto; }
    public LocalDateTime getFecha()                        { return fecha; }
    public void setFecha(LocalDateTime fecha)              { this.fecha = fecha; }
    public Boolean getEsReportado()                        { return esReportado; }
    public void setEsReportado(Boolean esReportado)        { this.esReportado = esReportado; }
    public Oferta getOferta()                              { return oferta; }
    public void setOferta(Oferta oferta)                   { this.oferta = oferta; }
    public Actor getActor()                                { return actor; }
    public void setActor(Actor actor)                      { this.actor = actor; }
}