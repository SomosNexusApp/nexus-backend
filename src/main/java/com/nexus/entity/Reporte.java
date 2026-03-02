package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Reporte de contenido inapropiado.
 *
 * Setters requeridos por ReporteService:
 *   setFecha(LocalDateTime)         line 49
 *   setMotivo(MotivoReporte)        line 46  <- MotivoReporte no String
 *   setActorDenunciado(Actor)       line 56
 *   setVehiculoDenunciado(Vehiculo) line 70
 *   setMensajeDenunciado(Mensaje)   line 73
 *   setComentarioDenunciado(Comentario) line 77
 *   setResolucion(String)           line 92
 *   setResoltor(Actor)              line 93
 */
@Entity
@Table(name = "reporte", indexes = {
    @Index(name = "idx_reporte_estado",      columnList = "estado"),
    @Index(name = "idx_reporte_tipo",        columnList = "tipo"),
    @Index(name = "idx_reporte_reportador",  columnList = "reportador_id")
})
public class Reporte extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportador_id", nullable = false)
    private Actor reportador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoReporte tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoReporte motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReporte estado = EstadoReporte.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // ---- Objeto denunciado (solo uno es no-null segun tipo) ----------

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    @JoinColumn(name = "actor_denunciado_id")
    private Actor actorDenunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    @JoinColumn(name = "producto_denunciado_id")
    private Producto productoDenunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    @JoinColumn(name = "oferta_denunciada_id")
    private Oferta ofertaDenunciada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    @JoinColumn(name = "vehiculo_denunciado_id")
    private Vehiculo vehiculoDenunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    @JoinColumn(name = "mensaje_denunciado_id")
    private Mensaje mensajeDenunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    @JoinColumn(name = "comentario_denunciado_id")
    private Comentario comentarioDenunciado;

    // ---- Resolucion (admin) -----------------------------------------

    @Column(columnDefinition = "TEXT")
    private String resolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resoltor_id")
    private Actor resoltor;

    private LocalDateTime fechaResolucion;

    @PrePersist
    protected void onCreate() {
        if (fecha  == null) fecha  = LocalDateTime.now();
        if (estado == null) estado = EstadoReporte.PENDIENTE;
    }

    // ---- Getters / Setters ------------------------------------------

    public Actor          getReportador()                           { return reportador; }
    public void           setReportador(Actor a)                    { this.reportador = a; }
    public TipoReporte    getTipo()                                 { return tipo; }
    public void           setTipo(TipoReporte t)                    { this.tipo = t; }
    public MotivoReporte  getMotivo()                               { return motivo; }
    /** ReporteService line 46: recibe MotivoReporte, no String */
    public void           setMotivo(MotivoReporte m)                { this.motivo = m; }
    public String         getDescripcion()                          { return descripcion; }
    public void           setDescripcion(String d)                  { this.descripcion = d; }
    public EstadoReporte  getEstado()                               { return estado; }
    public void           setEstado(EstadoReporte e)                { this.estado = e; }
    public LocalDateTime  getFecha()                                { return fecha; }
    /** ReporteService line 49 */
    public void           setFecha(LocalDateTime f)                 { this.fecha = f; }

    /** ReporteService line 56 */
    public Actor          getActorDenunciado()                      { return actorDenunciado; }
    public void           setActorDenunciado(Actor a)               { this.actorDenunciado = a; }

    public Producto       getProductoDenunciado()                   { return productoDenunciado; }
    public void           setProductoDenunciado(Producto p)         { this.productoDenunciado = p; }

    public Oferta         getOfertaDenunciada()                     { return ofertaDenunciada; }
    public void           setOfertaDenunciada(Oferta o)             { this.ofertaDenunciada = o; }

    /** ReporteService line 70 */
    public Vehiculo       getVehiculoDenunciado()                   { return vehiculoDenunciado; }
    public void           setVehiculoDenunciado(Vehiculo v)         { this.vehiculoDenunciado = v; }

    /** ReporteService line 73 */
    public Mensaje        getMensajeDenunciado()                    { return mensajeDenunciado; }
    public void           setMensajeDenunciado(Mensaje m)           { this.mensajeDenunciado = m; }

    /** ReporteService line 77 */
    public Comentario     getComentarioDenunciado()                 { return comentarioDenunciado; }
    public void           setComentarioDenunciado(Comentario c)     { this.comentarioDenunciado = c; }

    /** ReporteService line 92 */
    public String         getResolucion()                           { return resolucion; }
    public void           setResolucion(String r)                   { this.resolucion = r; }

    /** ReporteService line 93 */
    public Actor          getResoltor()                             { return resoltor; }
    public void           setResoltor(Actor a)                      { this.resoltor = a; }

    public LocalDateTime  getFechaResolucion()                      { return fechaResolucion; }
    public void           setFechaResolucion(LocalDateTime f)       { this.fechaResolucion = f; }
}