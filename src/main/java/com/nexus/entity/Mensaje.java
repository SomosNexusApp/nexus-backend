package com.nexus.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Mensaje extends DomainEntity {

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String texto; // <-- Añadido según UML

    @NotNull
    private LocalDateTime fechaCreacion;
    
    private Boolean estaActivo;
    
    // RELACIÓN: Mensaje pertenece a 1 Producto
    @ManyToOne
    @JoinColumn(name = "producto_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "vendedor", "galeriaImagenes"})
    private Producto producto;

    // RELACIÓN: Mensaje lo escribe 1 Usuario
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig", "cuentaEliminada", "cuentaVerificada"})
    private Usuario usuario;
    
    public Mensaje() {
        super();
        this.estaActivo = true;
        this.fechaCreacion = LocalDateTime.now();
    }
    
    public Mensaje(String texto, Usuario usuario, Producto producto) {
        super();
        this.texto = texto;
        this.usuario = usuario;
        this.producto = producto;
        this.estaActivo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Boolean getEstaActivo() { return estaActivo; }
    public void setEstaActivo(Boolean estaActivo) { this.estaActivo = estaActivo; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}