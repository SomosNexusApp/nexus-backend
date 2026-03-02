package com.nexus.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Entity
@Table(name = "devolucion", indexes = {
    @Index(name = "idx_devolucion_compra", columnList = "compra_id"),
    @Index(name = "idx_devolucion_estado", columnList = "estado")
})
public class Devolucion extends DomainEntity {
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "compra_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "comprador", "producto"})
    private Compra compra;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EstadoDevolucion estado = EstadoDevolucion.SOLICITADA;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MotivoDevolucion motivo;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @ElementCollection
    @CollectionTable(name = "devolucion_fotos", joinColumns = @JoinColumn(name = "devolucion_id"))
    @Column(name = "url", columnDefinition = "TEXT")
    private List<String> fotos = new ArrayList<>();
    @Column(name = "nota_vendedor",              columnDefinition = "TEXT") private String notaVendedor;
    @Column(name = "tracking_devolucion")         private String trackingDevolucion;
    @Column(name = "transportista_devolucion")    private String transportistaDevolucion;
    private Double importeDevolucion;
    private String stripeRefundId;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    @Column(columnDefinition = "TEXT") private String comentarioResolucion;
    @PrePersist protected void onCreate() {
        if (fechaSolicitud == null) fechaSolicitud = LocalDateTime.now();
        if (estado         == null) estado         = EstadoDevolucion.SOLICITADA;
        if (fotos          == null) fotos          = new ArrayList<>();
    }
    public Compra           getCompra()                               { return compra; }
    public void             setCompra(Compra c)                       { this.compra = c; }
    public EstadoDevolucion getEstado()                               { return estado; }
    public void             setEstado(EstadoDevolucion e)             { this.estado = e; }
    public MotivoDevolucion getMotivo()                               { return motivo; }
    public void             setMotivo(MotivoDevolucion m)             { this.motivo = m; }
    public String           getDescripcion()                          { return descripcion; }
    public void             setDescripcion(String d)                  { this.descripcion = d; }
    public List<String>     getFotos()                                { return fotos; }
    public void             setFotos(List<String> f)                  { this.fotos = f; }
    public void             addFoto(String url)                       { if (fotos==null)fotos=new ArrayList<>();fotos.add(url); }
    public String           getNotaVendedor()                         { return notaVendedor; }
    public void             setNotaVendedor(String n)                 { this.notaVendedor = n; }
    public String           getTrackingDevolucion()                   { return trackingDevolucion; }
    public void             setTrackingDevolucion(String t)           { this.trackingDevolucion = t; }
    public String           getTransportistaDevolucion()              { return transportistaDevolucion; }
    public void             setTransportistaDevolucion(String t)      { this.transportistaDevolucion = t; }
    public Double           getImporteDevolucion()                    { return importeDevolucion; }
    public void             setImporteDevolucion(Double i)            { this.importeDevolucion = i; }
    public String           getStripeRefundId()                       { return stripeRefundId; }
    public void             setStripeRefundId(String s)               { this.stripeRefundId = s; }
    public LocalDateTime    getFechaSolicitud()                       { return fechaSolicitud; }
    public void             setFechaSolicitud(LocalDateTime f)        { this.fechaSolicitud = f; }
    public LocalDateTime    getFechaResolucion()                      { return fechaResolucion; }
    public void             setFechaResolucion(LocalDateTime f)       { this.fechaResolucion = f; }
    public String           getComentarioResolucion()                 { return comentarioResolucion; }
    public void             setComentarioResolucion(String c)         { this.comentarioResolucion = c; }
}
