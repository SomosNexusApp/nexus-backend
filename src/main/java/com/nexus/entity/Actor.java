package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "actor")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public abstract class Actor extends DomainEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String user;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // ---- NUEVOS CAMPOS -------------------------------------------------
    @Column
    private String nombre;

    @Column
    private String apellidos;

    @Column(unique = true)
    private String telefono; // Movido desde Usuario.java

    @Column(columnDefinition = "TEXT")
    private String avatar; // Movido desde Usuario y Empresa

    // ---- 2FA -----------------------------------------------------------
    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    private String twoFactorMethod; // "TOTP" o "EMAIL"
    private String twoFactorSecret; // Secret TOTP (encriptado)

    // ---- Sesiones -------------------------------------------------------
    @Column(nullable = false)
    private int jwtVersion = 0;

    // ---- Estado de la cuenta --------------------------------------------
    @Column(nullable = false)
    private boolean cuentaEliminada = false;

    @Column(nullable = false)
    private boolean cuentaVerificada = false;

    private LocalDateTime fechaRegistro;

    @Column(columnDefinition = "TEXT")
    private String googleAvatarUrl;

    @Column
    private String googleId;

    @Column
    private String facebookId;

    @Column
    private String avatarSource; // "GOOGLE", "INITIALS", "CUSTOM"
    
    @Column(columnDefinition = "TEXT")
    private String customAvatarUrl;

    /** Ban permanente */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean baneado = false;

    @Column(columnDefinition = "TEXT")
    private String motivoBan;

    /** Suspensión temporal — null = no suspendido */
    @Column(name = "suspendido_hasta")
    private LocalDateTime suspendidoHasta;

    @Column(name = "motivo_suspension", columnDefinition = "TEXT")
    private String motivoSuspension;

    /** Fraude flag */
    @Column(name = "flag_fraude", nullable = false, columnDefinition = "boolean default false")
    private boolean flagFraude = false;

    @Column(name = "motivo_flag", columnDefinition = "TEXT")
    private String motivoFlag;

    // ---- Notificaciones -------------------------------------------------
    @Embedded
    private ActorNotificacionConfig notificacionConfig = new ActorNotificacionConfig();

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @PrePersist
    protected void onActorCreate() {
        if (fechaRegistro == null)
            fechaRegistro = LocalDateTime.now();
        if (notificacionConfig == null)
            notificacionConfig = new ActorNotificacionConfig();
    }

    // ---- Getters / Setters -----------------------------------------------

    public String getUser() {
        return user;
    }

    public void setUser(String u) {
        this.user = u;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String e) {
        this.email = e;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String p) {
        this.password = p;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String n) {
        this.nombre = n;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String a) {
        this.apellidos = a;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String t) {
        this.telefono = t;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean b) {
        this.twoFactorEnabled = b;
    }

    public String getTwoFactorMethod() {
        return twoFactorMethod;
    }

    public void setTwoFactorMethod(String m) {
        this.twoFactorMethod = m;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String s) {
        this.twoFactorSecret = s;
    }

    public int getJwtVersion() {
        return jwtVersion;
    }

    public void setJwtVersion(int v) {
        this.jwtVersion = v;
    }

    public boolean isCuentaEliminada() {
        return cuentaEliminada;
    }

    public void setCuentaEliminada(boolean b) {
        this.cuentaEliminada = b;
    }

    public boolean isCuentaVerificada() {
        return cuentaVerificada;
    }

    public void setCuentaVerificada(boolean b) {
        this.cuentaVerificada = b;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime f) {
        this.fechaRegistro = f;
    }

    public String getGoogleAvatarUrl() {
        return googleAvatarUrl;
    }

    public void setGoogleAvatarUrl(String g) {
        this.googleAvatarUrl = g;
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

    public String getAvatarSource() {
        return avatarSource;
    }

    public void setAvatarSource(String s) {
        this.avatarSource = s;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getCustomAvatarUrl() {
        return customAvatarUrl;
    }

    public void setCustomAvatarUrl(String c) {
        this.customAvatarUrl = c;
    }

    public ActorNotificacionConfig getNotificacionConfig() {
        return notificacionConfig;
    }

    public void setNotificacionConfig(ActorNotificacionConfig c) {
        this.notificacionConfig = c;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isBaneado()                           { return baneado; }
    public void    setBaneado(boolean b)                 { this.baneado = b; }
    public String  getMotivoBan()                        { return motivoBan; }
    public void    setMotivoBan(String m)                { this.motivoBan = m; }
    public LocalDateTime getSuspendidoHasta()            { return suspendidoHasta; }
    public void    setSuspendidoHasta(LocalDateTime d)   { this.suspendidoHasta = d; }
    public String  getMotivoSuspension()                 { return motivoSuspension; }
    public void    setMotivoSuspension(String m)         { this.motivoSuspension = m; }
    public boolean isFlagFraude()                        { return flagFraude; }
    public void    setFlagFraude(boolean f)              { this.flagFraude = f; }
    public String  getMotivoFlag()                       { return motivoFlag; }
    public void    setMotivoFlag(String m)               { this.motivoFlag = m; }
}