package com.nexus.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    
    /** Empresa vinculada al contrato (solo para contratos de tipo empresa). Puede ser null si es usuario personal. */
    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /**
     * Actor (Usuario o Empresa) que solicitó el patrocinio.
     * Para contratos de patrocinio iniciados por el usuario/empresa mismo.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
        "cuentaEliminada", "cuentaVerificada"})
    private Actor actor;

    /** Tipo del item patrocinado: 'PRODUCTO', 'OFERTA', 'VEHICULO'. */
    private String tipoItem;

    /** ID del item patrocinado (producto, oferta o vehiculo). */
    private Integer itemId;

    /** Título del item patrocinado (cacheado para mostrar sin join). */
    private String itemTitulo;

    /** URL de imagen del item patrocinado (cacheada). */
    @Column(columnDefinition = "TEXT")
    private String itemImagen;

    /** Días de patrocinio solicitados (null = indefinido). */
    private Integer diasPatrocinio;

    /** Producto a destacar si tipo PUBLICACION (opcional, legado). */
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

    public Actor getActor() { return actor; }
    public void setActor(Actor actor) { this.actor = actor; }

    public String getTipoItem() { return tipoItem; }
    public void setTipoItem(String tipoItem) { this.tipoItem = tipoItem; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public String getItemTitulo() { return itemTitulo; }
    public void setItemTitulo(String itemTitulo) { this.itemTitulo = itemTitulo; }

    public String getItemImagen() { return itemImagen; }
    public void setItemImagen(String itemImagen) { this.itemImagen = itemImagen; }

    public Integer getDiasPatrocinio() { return diasPatrocinio; }
    public void setDiasPatrocinio(Integer diasPatrocinio) { this.diasPatrocinio = diasPatrocinio; }

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