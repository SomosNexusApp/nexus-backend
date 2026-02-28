package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compra", indexes = {
    @Index(name = "idx_compra_comprador", columnList = "comprador_id"),
    @Index(name = "idx_compra_estado",    columnList = "estado")
})
public class Compra extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprador_id", nullable = false)
    private Usuario comprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompra estado = EstadoCompra.PENDIENTE;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(nullable = false)
    private Double precioFinal = 0.0;

    private Double precioEnvio = 0.0;

    @Enumerated(EnumType.STRING)
    private MetodoEntrega metodoEntrega;

    // Copia de la direccion en el momento de la compra
    private String dirNombre;
    private String dirCalle;
    private String dirCiudad;
    private String dirCodigoPostal;
    private String dirPais;
    private String dirTelefono;

    private LocalDateTime fechaCompra;
    private LocalDateTime fechaPago;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaCompletada;
    private LocalDateTime fechaCancelacion;
    
    @Enumerated(EnumType.STRING)
    private TipoEnvio tipoEnvio;
    
    private Double costoEnvio;     // 0.0=personal | 2.69=punto | 3.69=domicilio
    private Double comisionNexus;  // calculado automaticamente
    
    private String direccionCompleta;  // solo si DOMICILIO
    private String puntoRecogidaId;    // solo si PUNTO_RECOGIDA

    @PrePersist
    protected void onCreate() {
        if (fechaCompra == null) fechaCompra = LocalDateTime.now();
        if (estado      == null) estado      = EstadoCompra.PENDIENTE;
    }

    public Usuario   getComprador()                            { return comprador; }
    public void      setComprador(Usuario c)                   { this.comprador = c; }
    public Producto  getProducto()                             { return producto; }
    public void      setProducto(Producto p)                   { this.producto = p; }

    /** Acceso directo al vendedor = publicador del producto */
    @Transient
    public Actor getVendedor() {
        return (producto != null) ? producto.getPublicador() : null;
    }

    public EstadoCompra getEstado()                            { return estado; }
    public void      setEstado(EstadoCompra e)                 { this.estado = e; }
    public String    getStripePaymentIntentId()                { return stripePaymentIntentId; }
    public void      setStripePaymentIntentId(String s)        { this.stripePaymentIntentId = s; }
    public Double    getPrecioFinal()                          { return precioFinal; }
    public void      setPrecioFinal(Double p)                  { this.precioFinal = p; }
    public Double    getPrecioEnvio()                          { return precioEnvio; }
    public void      setPrecioEnvio(Double p)                  { this.precioEnvio = p; }
    public MetodoEntrega getMetodoEntrega()                    { return metodoEntrega; }
    public void      setMetodoEntrega(MetodoEntrega m)         { this.metodoEntrega = m; }
    public String    getDirNombre()                            { return dirNombre; }
    public void      setDirNombre(String v)                    { this.dirNombre = v; }
    public String    getDirCalle()                             { return dirCalle; }
    public void      setDirCalle(String v)                     { this.dirCalle = v; }
    public String    getDirCiudad()                            { return dirCiudad; }
    public void      setDirCiudad(String v)                    { this.dirCiudad = v; }
    public String    getDirCodigoPostal()                      { return dirCodigoPostal; }
    public void      setDirCodigoPostal(String v)              { this.dirCodigoPostal = v; }
    public String    getDirPais()                              { return dirPais; }
    public void      setDirPais(String v)                      { this.dirPais = v; }
    public String    getDirTelefono()                          { return dirTelefono; }
    public void      setDirTelefono(String v)                  { this.dirTelefono = v; }
    public LocalDateTime getFechaCompra()                      { return fechaCompra; }
    public void      setFechaCompra(LocalDateTime f)           { this.fechaCompra = f; }
    public LocalDateTime getFechaPago()                        { return fechaPago; }
    public void      setFechaPago(LocalDateTime f)             { this.fechaPago = f; }
    public LocalDateTime getFechaEnvio()                       { return fechaEnvio; }
    public void      setFechaEnvio(LocalDateTime f)            { this.fechaEnvio = f; }
    public LocalDateTime getFechaEntrega()                     { return fechaEntrega; }
    public void      setFechaEntrega(LocalDateTime f)          { this.fechaEntrega = f; }
    public LocalDateTime getFechaCompletada()                  { return fechaCompletada; }
    public void      setFechaCompletada(LocalDateTime f)       { this.fechaCompletada = f; }
    public LocalDateTime getFechaCancelacion()                 { return fechaCancelacion; }
    public void      setFechaCancelacion(LocalDateTime f)      { this.fechaCancelacion = f; }
    public TipoEnvio getTipoEnvio() { return tipoEnvio; }
    public void setTipoEnvio(TipoEnvio tipoEnvio) { this.tipoEnvio = tipoEnvio; }

    public Double getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(Double costoEnvio) { this.costoEnvio = costoEnvio; }

    public Double getComisionNexus() { return comisionNexus; }
    public void setComisionNexus(Double comisionNexus) { this.comisionNexus = comisionNexus; }

    public String getDireccionCompleta() { return direccionCompleta; }
    public void setDireccionCompleta(String direccionCompleta) { this.direccionCompleta = direccionCompleta; }

    public String getPuntoRecogidaId() { return puntoRecogidaId; }
    public void setPuntoRecogidaId(String puntoRecogidaId) { this.puntoRecogidaId = puntoRecogidaId; }
}