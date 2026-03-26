package com.nexus.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupon_uso")
public class CuponUso extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupon_id", nullable = false)
    private Cupon cupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Actor usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(name = "fecha_uso", nullable = false)
    private LocalDateTime fechaUso;

    @Column(name = "importe_ahorro", nullable = false)
    private BigDecimal importeAhorro;

    @PrePersist
    protected void onCreate() {
        if (fechaUso == null) fechaUso = LocalDateTime.now();
    }

    // Getters and Setters
    public Cupon getCupon() { return cupon; }
    public void setCupon(Cupon cupon) { this.cupon = cupon; }

    public Actor getUsuario() { return usuario; }
    public void setUsuario(Actor usuario) { this.usuario = usuario; }

    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }

    public LocalDateTime getFechaUso() { return fechaUso; }
    public void setFechaUso(LocalDateTime fechaUso) { this.fechaUso = fechaUso; }

    public BigDecimal getImporteAhorro() { return importeAhorro; }
    public void setImporteAhorro(BigDecimal importeAhorro) { this.importeAhorro = importeAhorro; }
}
