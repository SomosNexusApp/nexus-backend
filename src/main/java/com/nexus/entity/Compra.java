package com.nexus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
public class Compra extends DomainEntity {

    @Min(0)
    private double precioFinal;

    private LocalDateTime fechaCompra;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EstadoCompra estadoCompra;

    // Relación: Usuario realiza Compra (El comprador)
    @ManyToOne
    @JoinColumn(name = "comprador_id")
    private Usuario comprador;

    // Relación: Compra pertenece a Producto
    // Usamos ManyToOne porque técnicamente un producto podría tener varios intentos de compra (historial),
    // aunque solo una se concrete como VENDIDO.
    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    public Compra() {
        super();
    }
    
    public Compra(double precioFinal, Usuario comprador, Producto producto) {
        super();
        this.precioFinal = precioFinal;
        this.comprador = comprador;
        this.producto = producto;
        this.fechaCompra = LocalDateTime.now();
        this.estadoCompra = EstadoCompra.PENDIENTE;
    }

    // Getters y Setters

    public double getPrecioFinal() {
        return precioFinal;
    }

    public void setPrecioFinal(double precioFinal) {
        this.precioFinal = precioFinal;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDateTime fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public EstadoCompra getEstadoCompra() {
        return estadoCompra;
    }

    public void setEstadoCompra(EstadoCompra estadoCompra) {
        this.estadoCompra = estadoCompra;
    }

    public Usuario getComprador() {
        return comprador;
    }

    public void setComprador(Usuario comprador) {
        this.comprador = comprador;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}