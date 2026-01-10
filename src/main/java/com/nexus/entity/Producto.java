package com.nexus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Producto extends DomainEntity {

    @NotBlank
    private String titulo;

    @NotBlank
    private String descripcion;

    @Min(0)
    private double precio;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoOferta tipoOferta;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoProducto estadoProducto;


    @ManyToOne
    @JoinColumn(name = "publicador_id")
    private Usuario publicador;

    public Producto() {
        super();
        this.estadoProducto = EstadoProducto.DISPONIBLE; 
    }

    public Producto(String titulo, String descripcion, double precio, TipoOferta tipoOferta, Usuario publicador) {
        super();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.tipoOferta = tipoOferta;
        this.publicador = publicador;
        this.estadoProducto = EstadoProducto.DISPONIBLE;
    }

    // Getters y Setters

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public TipoOferta getTipoOferta() {
        return tipoOferta;
    }

    public void setTipoOferta(TipoOferta tipoOferta) {
        this.tipoOferta = tipoOferta;
    }

    public EstadoProducto getEstadoProducto() {
        return estadoProducto;
    }

    public void setEstadoProducto(EstadoProducto estadoProducto) {
        this.estadoProducto = estadoProducto;
    }

    public Usuario getPublicador() {
        return publicador;
    }

    public void setPublicador(Usuario publicador) {
        this.publicador = publicador;
    }
}