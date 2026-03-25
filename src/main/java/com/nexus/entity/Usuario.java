package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "usuario")
@PrimaryKeyJoinColumn(name = "actor_id")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Usuario extends Actor {

    @Column(columnDefinition = "TEXT")
    private String biografia;

    private String ubicacion;

    // NUEVOS CAMPOS -----------------------------------------------------
    @Enumerated(EnumType.STRING)
    private TipoCuenta tipoCuenta = TipoCuenta.PERSONAL;

    @Column(nullable = false)
    private boolean cuentaPrivada = false;

    @Column(nullable = false)
    private boolean terminosAceptados = false;

    @Column(nullable = false)
    private boolean newsletterSuscrito = false;

    @Column
    private String googleId;

    @Column
    private String facebookId;

    @Column
    private String versionTerminosAceptados;

    @Column
    private LocalDateTime fechaAceptacionTerminos;


    // CAMPOS EXISTENTES REVISADOS ---------------------------------------
    @Column(nullable = false)
    private double reputacion = 0.0;

    @Column(nullable = false)
    private int totalVentas = 0;

    @Column(name = "es_verificado", nullable = false)
    private boolean esVerificado = false;

    @Column(name = "perfil_publico", nullable = false)
    private boolean perfilPublico = true;

    @Column(name = "mostrar_telefono", nullable = false)
    private boolean mostrarTelefono = false;

    @Column(name = "mostrar_ubicacion", nullable = false)
    private boolean mostrarUbicacion = true;

    @Column(name = "permitir_mensajes_desconocidos", nullable = false)
    private boolean permitirMensajesDesconocidos = true;

    // PREFERENCIAS NOTIFICACIONES ---------------------------------------
    @Column(name = "notif_nuevos_mensajes", nullable = false)
    private boolean notifNuevosMensajes = true;

    @Column(name = "notif_nueva_compra", nullable = false)
    private boolean notifNuevaCompra = true;

    @Column(name = "notif_valoracion", nullable = false)
    private boolean notifValoracion = true;

    @Column(name = "notif_ofertas", nullable = false)
    private boolean notifOfertas = true;

    @Column(name = "notif_envios", nullable = false)
    private boolean notifEnvios = true;

    @Column(name = "notif_novedades", nullable = false)
    private boolean notifNovedades = true;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nombre", column = @Column(name = "dir_nombre")),
            @AttributeOverride(name = "direccion", column = @Column(name = "dir_calle")),
            @AttributeOverride(name = "ciudad", column = @Column(name = "dir_ciudad")),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "dir_cp")),
            @AttributeOverride(name = "pais", column = @Column(name = "dir_pais")),
            @AttributeOverride(name = "telefono", column = @Column(name = "dir_telefono"))
    })
    private DireccionEnvio direccionPorDefecto;

    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "usuario_bloqueados", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "bloqueado_id")
    private List<Integer> blockedUserIds = new ArrayList<>();

    // ── CAMPOS ADMIN / MODERACIÓN ──────────────────────────────────────────

    /** Ban permanente */
    @Column(nullable = false)
    private boolean baneado = false;

    @Column(columnDefinition = "TEXT")
    private String motivoBan;

    /** Suspensión temporal — null = no suspendido */
    @Column(name = "suspendido_hasta")
    private java.time.LocalDateTime suspendidoHasta;

    @Column(name = "motivo_suspension", columnDefinition = "TEXT")
    private String motivoSuspension;

    /** Fraude flag */
    @Column(name = "flag_fraude", nullable = false)
    private boolean flagFraude = false;

    @Column(name = "motivo_flag", columnDefinition = "TEXT")
    private String motivoFlag;

    public Usuario() {
    }

    // ---- Getters / Setters ADMIN -------------------------------------------

    public boolean isBaneado()                           { return baneado; }
    public void    setBaneado(boolean b)                 { this.baneado = b; }
    public String  getMotivoBan()                        { return motivoBan; }
    public void    setMotivoBan(String m)                { this.motivoBan = m; }
    public java.time.LocalDateTime getSuspendidoHasta()  { return suspendidoHasta; }
    public void    setSuspendidoHasta(java.time.LocalDateTime d) { this.suspendidoHasta = d; }
    public String  getMotivoSuspension()                 { return motivoSuspension; }
    public void    setMotivoSuspension(String m)         { this.motivoSuspension = m; }
    public boolean isFlagFraude()                        { return flagFraude; }
    public void    setFlagFraude(boolean f)              { this.flagFraude = f; }
    public String  getMotivoFlag()                       { return motivoFlag; }
    public void    setMotivoFlag(String m)               { this.motivoFlag = m; }

    // ---- Getters / Setters -----------------------------------------------

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String b) {
        this.biografia = b;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String u) {
        this.ubicacion = u;
    }

    // Getters y Setters NUEVOS
    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(TipoCuenta t) {
        this.tipoCuenta = t;
    }

    public boolean isCuentaPrivada() {
        return cuentaPrivada;
    }

    public void setCuentaPrivada(boolean b) {
        this.cuentaPrivada = b;
    }

    public boolean isTerminosAceptados() {
        return terminosAceptados;
    }

    public void setTerminosAceptados(boolean b) {
        this.terminosAceptados = b;
    }

    public boolean isNewsletterSuscrito() {
        return newsletterSuscrito;
    }

    public void setNewsletterSuscrito(boolean b) {
        this.newsletterSuscrito = b;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String g) {
        this.googleId = g;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String f) {
        this.facebookId = f;
    }

    public String getVersionTerminosAceptados() {
        return versionTerminosAceptados;
    }

    public void setVersionTerminosAceptados(String v) {
        this.versionTerminosAceptados = v;
    }

    public LocalDateTime getFechaAceptacionTerminos() {
        return fechaAceptacionTerminos;
    }

    public void setFechaAceptacionTerminos(LocalDateTime d) {
        this.fechaAceptacionTerminos = d;
    }


    public double getReputacion() {
        return reputacion;
    }

    public void setReputacion(double r) {
        this.reputacion = r;
    }

    public int getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(int t) {
        this.totalVentas = t;
    }

    public boolean isEsVerificado() {
        return esVerificado;
    }

    public void setEsVerificado(boolean v) {
        this.esVerificado = v;
    }

    public boolean isPerfilPublico() {
        return perfilPublico;
    }

    public void setPerfilPublico(boolean b) {
        this.perfilPublico = b;
    }

    public boolean isMostrarTelefono() {
        return mostrarTelefono;
    }

    public void setMostrarTelefono(boolean b) {
        this.mostrarTelefono = b;
    }

    public boolean isMostrarUbicacion() {
        return mostrarUbicacion;
    }

    public void setMostrarUbicacion(boolean b) {
        this.mostrarUbicacion = b;
    }

    public boolean isPermitirMensajesDesconocidos() {
        return permitirMensajesDesconocidos;
    }

    public void setPermitirMensajesDesconocidos(boolean permitirMensajesDesconocidos) {
        this.permitirMensajesDesconocidos = permitirMensajesDesconocidos;
    }

    public boolean isNotifNuevosMensajes() {
        return notifNuevosMensajes;
    }

    public void setNotifNuevosMensajes(boolean notifNuevosMensajes) {
        this.notifNuevosMensajes = notifNuevosMensajes;
    }

    public boolean isNotifNuevaCompra() {
        return notifNuevaCompra;
    }

    public void setNotifNuevaCompra(boolean notifNuevaCompra) {
        this.notifNuevaCompra = notifNuevaCompra;
    }

    public boolean isNotifValoracion() {
        return notifValoracion;
    }

    public void setNotifValoracion(boolean notifValoracion) {
        this.notifValoracion = notifValoracion;
    }

    public boolean isNotifOfertas() {
        return notifOfertas;
    }

    public void setNotifOfertas(boolean notifOfertas) {
        this.notifOfertas = notifOfertas;
    }

    public boolean isNotifEnvios() {
        return notifEnvios;
    }

    public void setNotifEnvios(boolean notifEnvios) {
        this.notifEnvios = notifEnvios;
    }

    public boolean isNotifNovedades() {
        return notifNovedades;
    }

    public void setNotifNovedades(boolean notifNovedades) {
        this.notifNovedades = notifNovedades;
    }

    public DireccionEnvio getDireccionPorDefecto() {
        return direccionPorDefecto;
    }

    public void setDireccionPorDefecto(DireccionEnvio d) {
        this.direccionPorDefecto = d;
    }

    public List<Integer> getBlockedUserIds() {
        return blockedUserIds;
    }

    public void setBlockedUserIds(List<Integer> l) {
        this.blockedUserIds = l;
    }
}