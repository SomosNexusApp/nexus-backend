package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;



@Entity
@Table(name = "favorito")
public class Favorito extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Actor actor;

    @ManyToOne(fetch = FetchType.EAGER) // <-- CAMBIADO A EAGER
    @JoinColumn(name = "oferta_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({
        "hibernateLazyInitializer", "handler", "votos", "propietario", "categoria", "valoraciones", "vendedor"
    })
    private Oferta oferta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({
        "hibernateLazyInitializer", "handler", "votos", "propietario", "categoria", "valoraciones", "vendedor", "galeriaImagenes"
    })
    private Producto producto;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehiculo_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({
        "hibernateLazyInitializer", "handler", "publicador", "categoria", "galeriaImagenes"
    })
    private Vehiculo vehiculo;

    private LocalDateTime fechaGuardado;
    
    @Column(columnDefinition = "TEXT")
    private String nota;

    public Favorito() {
        super();
        this.fechaGuardado = LocalDateTime.now();
    }

    // Getters y Setters
    public Actor getActor() { return actor; }
    public void setActor(Actor actor) { this.actor = actor; }

    public Oferta getOferta() { return oferta; }
    public void setOferta(Oferta oferta) { this.oferta = oferta; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }

    public LocalDateTime getFechaGuardado() { return fechaGuardado; }
    public void setFechaGuardado(LocalDateTime fechaGuardado) { this.fechaGuardado = fechaGuardado; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }


}