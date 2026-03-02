package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "chat_mensaje", indexes = {
    @Index(name = "idx_chat_producto",  columnList = "producto_id"),
    @Index(name = "idx_chat_remitente", columnList = "remitente_id"),
    @Index(name = "idx_chat_fecha",     columnList = "fechaEnvio")
})
public class ChatMensaje extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "vendedor", "categoria", "galeriaImagenes"})
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remitente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
        "cuentaEliminada", "cuentaVerificada"})
    private Usuario remitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
        "cuentaEliminada", "cuentaVerificada"})
    private Usuario receptor;

    @Column(columnDefinition = "TEXT") private String texto;
    @Column(columnDefinition = "TEXT") private String mediaUrl;
    @Column(columnDefinition = "TEXT") private String mediaThumbnail;
    private Integer audioDuracionSegundos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMensaje tipo = TipoMensaje.TEXTO;

    private LocalDateTime fechaEnvio;
    private Boolean leido = false;
    private Double  precioPropuesto;
    private String  estadoPropuesta;

    public ChatMensaje() { super(); this.fechaEnvio = LocalDateTime.now(); this.leido = false; }

    public Producto  getProducto()                       { return producto; }
    public void      setProducto(Producto p)             { this.producto = p; }
    public Usuario   getRemitente()                      { return remitente; }
    public void      setRemitente(Usuario r)             { this.remitente = r; }
    public Usuario   getReceptor()                       { return receptor; }
    public void      setReceptor(Usuario r)              { this.receptor = r; }
    public String    getTexto()                          { return texto; }
    public void      setTexto(String t)                  { this.texto = t; }
    public String    getMediaUrl()                       { return mediaUrl; }
    public void      setMediaUrl(String u)               { this.mediaUrl = u; }
    public String    getMediaThumbnail()                 { return mediaThumbnail; }
    public void      setMediaThumbnail(String t)         { this.mediaThumbnail = t; }
    public Integer   getAudioDuracionSegundos()          { return audioDuracionSegundos; }
    public void      setAudioDuracionSegundos(Integer d) { this.audioDuracionSegundos = d; }
    public TipoMensaje getTipo()                         { return tipo; }
    public void      setTipo(TipoMensaje t)              { this.tipo = t; }
    public LocalDateTime getFechaEnvio()                 { return fechaEnvio; }
    public void      setFechaEnvio(LocalDateTime f)      { this.fechaEnvio = f; }
    public Boolean   getLeido()                          { return leido; }
    public void      setLeido(Boolean l)                 { this.leido = l; }
    public Double    getPrecioPropuesto()                { return precioPropuesto; }
    public void      setPrecioPropuesto(Double p)        { this.precioPropuesto = p; }
    public String    getEstadoPropuesta()                { return estadoPropuesta; }
    public void      setEstadoPropuesta(String e)        { this.estadoPropuesta = e; }
}