package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "spark_voto",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_spark_voto",
           columnNames = {"actor_id", "oferta_id", "producto_id"}))
public class SparkVoto extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    private Actor actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "actor", "galeriaImagenes"})
    private Oferta oferta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "vendedor", "galeriaImagenes"})
    private Producto producto;

    /** +1 = upvote (Spark), -1 = downvote (Drip) */
    @Column(nullable = false)
    private Integer valor;

    private LocalDateTime fechaVoto;

    /** Constructor por defecto (requerido por JPA) */
    public SparkVoto() {
        this.fechaVoto = LocalDateTime.now();
    }

    /**
     * Constructor para votar una Oferta.
     * Usado en PopulateDB y OfertaService:
     *   new SparkVoto(usuario, oferta, true)   -> Spark (+1)
     *   new SparkVoto(usuario, oferta, false)  -> Drip  (-1)
     *
     * Acepta Actor en lugar de Usuario para no forzar un cast en el servicio,
     * pero en la practica siempre sera un Usuario o Empresa.
     */
    public SparkVoto(Actor actor, Oferta oferta, boolean esSpark) {
        this();
        this.actor  = actor;
        this.oferta = oferta;
        this.valor  = esSpark ? 1 : -1;
    }

    /** Constructor para votar un Producto */
    public SparkVoto(Actor actor, Producto producto, boolean esSpark) {
        this();
        this.actor    = actor;
        this.producto = producto;
        this.valor    = esSpark ? 1 : -1;
    }

    public Actor    getActor()                        { return actor; }
    public void     setActor(Actor a)                 { this.actor = a; }
    public Oferta   getOferta()                       { return oferta; }
    public void     setOferta(Oferta o)               { this.oferta = o; }
    public Producto getProducto()                     { return producto; }
    public void     setProducto(Producto p)           { this.producto = p; }
    public Integer  getValor()                        { return valor; }
    public void     setValor(Integer v)               { this.valor = v; }
    public LocalDateTime getFechaVoto()               { return fechaVoto; }
    public void     setFechaVoto(LocalDateTime f)     { this.fechaVoto = f; }
}