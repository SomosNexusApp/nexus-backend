package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devolucion")
public class Devolucion extends DomainEntity {

    @OneToOne
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoDevolucion motivo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "devolucion_fotos", joinColumns = @JoinColumn(name = "devolucion_id"))
    @Column(name = "foto_url")
    private List<String> fotos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EstadoDevolucion estado = EstadoDevolucion.SOLICITADA;

    private String notaVendedor;
    private String notaAdmin;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    
    private String trackingDevolucion;
    private Double importeDevolucion;
    private String direccionEnvio;

    public Devolucion() {
        super();
        this.fechaSolicitud = LocalDateTime.now();
    }

    // Getters y Setters
    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }

    public MotivoDevolucion getMotivo() { return motivo; }
    public void setMotivo(MotivoDevolucion motivo) { this.motivo = motivo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }

    public EstadoDevolucion getEstado() { return estado; }
    public void setEstado(EstadoDevolucion estado) { this.estado = estado; }

    public String getNotaVendedor() { return notaVendedor; }
    public void setNotaVendedor(String notaVendedor) { this.notaVendedor = notaVendedor; }

    public String getNotaAdmin() { return notaAdmin; }
    public void setNotaAdmin(String notaAdmin) { this.notaAdmin = notaAdmin; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }

    public String getTrackingDevolucion() { return trackingDevolucion; }
    public void setTrackingDevolucion(String t) { this.trackingDevolucion = t; }

    public Double getImporteDevolucion() { return importeDevolucion; }
    public void setImporteDevolucion(Double importeDevolucion) { this.importeDevolucion = importeDevolucion; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }
}
