package com.nexus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Contrato extends DomainEntity {
    
    @Enumerated(EnumType.STRING)
    private TipoContrato tipoContrato;
    
    @Enumerated(EnumType.STRING)
    private EstadoContrato estado = EstadoContrato.DRAFT;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fecha; 
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaInicio;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaFin;
    
    private Double monto;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /** Producto a destacar si tipo PUBLICACION (opcional). */
    private Integer productoId;

    @Column(columnDefinition = "TEXT")
    private String textoBanner;

    private String urlClick;

    private String stripeCheckoutSessionId;

    private String stripePaymentIntentId;

    public Contrato() {
        super();
        this.fecha = LocalDateTime.now();
    }

    public TipoContrato getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(TipoContrato tipoContrato) { this.tipoContrato = tipoContrato; }

    public EstadoContrato getEstado() { return estado; }
    public void setEstado(EstadoContrato estado) { this.estado = estado; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Integer getProductoId() { return productoId; }
    public void setProductoId(Integer productoId) { this.productoId = productoId; }

    public String getTextoBanner() { return textoBanner; }
    public void setTextoBanner(String textoBanner) { this.textoBanner = textoBanner; }

    public String getUrlClick() { return urlClick; }
    public void setUrlClick(String urlClick) { this.urlClick = urlClick; }

    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) { this.stripeCheckoutSessionId = stripeCheckoutSessionId; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
}