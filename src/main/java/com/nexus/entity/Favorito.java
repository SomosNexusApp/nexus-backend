package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;



@Entity
@Table(name = "favorito")
public class Favorito extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "votos", "propietario", "categorias", "valoraciones"})
    private Oferta oferta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "votos", "propietario", "categorias", "valoraciones"})
    private Producto producto;

    private LocalDateTime fechaGuardado;
    
    @Column(columnDefinition = "TEXT")
    private String nota;

    public Favorito() {
        super();
        this.fechaGuardado = LocalDateTime.now();
    }

    // Getters y Setters
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Oferta getOferta() { return oferta; }
    public void setOferta(Oferta oferta) { this.oferta = oferta; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public LocalDateTime getFechaGuardado() { return fechaGuardado; }
    public void setFechaGuardado(LocalDateTime fechaGuardado) { this.fechaGuardado = fechaGuardado; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }


}