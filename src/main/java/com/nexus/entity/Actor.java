package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Actor es la clase base de todos los usuarios del sistema.
// Usamos herencia tipo JOINED: cada subtipo (Usuario, Empresa, Admin) tiene su propia tabla
// pero comparten la tabla 'actor' para los campos comunes.
// Es una arquitectura limpia pero tiene sus complicaciones (ver UsuarioService.convertirAEmpresa)
@Entity
@Table(name = "actor")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" }) // evitamos excepciones de Hibernate al serializar
public abstract class Actor extends DomainEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String user; // el campo se llama 'user' en Java pero 'username' en la bbdd

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // siempre hasheada con bcrypt, nunca en texto plano

    // ---- CAMPOS GENERALES del actor -------------------------------------------------
    @Column
    private String nombre;

    @Column
    private String apellidos;

    @Column(unique = true)
    private String telefono; // Movido desde Usuario.java

    @Column(columnDefinition = "TEXT")
    private String avatar; // Movido desde Usuario y Empresa

    // ---- 2FA (autenticacion de dos factores) -----------------------------------
    @Column(nullable = false)
    private boolean twoFactorEnabled = false; // por defecto viene desactivado

    private String twoFactorMethod; // puede ser "TOTP" (app autenticadora) o "EMAIL" (codigo por correo)
    private String twoFactorSecret; // la clave secreta TOTP, que debe estar encriptada en la bbdd

    // ---- control de sesiones ----------------------------------------------------
    @Column(nullable = false)
    private int jwtVersion = 0; // cuando sube, invalida todos los tokens anteriores del usuario

    // ---- estado de la cuenta --------------------------------------------
    @Column(nullable = false)
    private boolean cuentaEliminada = false; // soft delete: no borramos de la bbdd, solo marcamos

    @Column(nullable = false)
    private boolean cuentaVerificada = false; // true cuando el usuario confirma su email

    private LocalDateTime fechaRegistro;

    @Column(columnDefinition = "TEXT")
    private String googleAvatarUrl; // url del avatar que viene de Google (puede ser larga)

    @Column
    private String googleId; // identificador unico de Google, para vincular cuentas OAuth

    @Column
    private String avatarSource; // de donde viene el avatar: "GOOGLE", "INITIALS" (iniciales) o "CUSTOM"

    @Column(columnDefinition = "TEXT")
    private String customAvatarUrl; // url del avatar subido por el usuario

    /** Ban permanente — el usuario no puede acceder a la plataforma */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean baneado = false;

    @Column(columnDefinition = "TEXT")
    private String motivoBan; // texto que ve el usuario cuando intenta entrar

    /** Suspension temporal — null significa que no está suspendido en este momento */
    @Column(name = "suspendido_hasta")
    private LocalDateTime suspendidoHasta;

    @Column(name = "motivo_suspension", columnDefinition = "TEXT")
    private String motivoSuspension;

    /** Marca de fraude para alertar al equipo de moderacion, no bloquea al usuario */
    @Column(name = "flag_fraude", nullable = false, columnDefinition = "boolean default false")
    private boolean flagFraude = false;

    @Column(name = "motivo_flag", columnDefinition = "TEXT")
    private String motivoFlag;

    // ---- config de notificaciones embebida en la misma tabla de actor ---
    @Embedded
    private ActorNotificacionConfig notificacionConfig = new ActorNotificacionConfig();

    // id del cliente en Stripe, para gestionar pagos y reembolsos
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    // ---- reset de contraseña (antes era una entidad separada PasswordResetToken) ----
    // almacenar el token aqui evita tener que hacer JOIN con otra tabla
    // el token es un UUID que se manda al email del usuario
    @Column(name = "reset_token", unique = true)
    private String resetToken;

    @Column(name = "reset_token_expira")
    private LocalDateTime resetTokenExpira; // cuando caduca el enlace de reset (15 min por defecto)

    // ---- ultima sesion de dispositivo (antes era SesionDispositivo) ----
    // guardar solo la ultima sesion evita tener una tabla de historial grande
    // si se necesita historial completo se puede ver en los logs del servidor
    @Column(name = "ultimo_ip")
    private String ultimoIp;

    @Column(name = "ultimo_dispositivo")
    private String ultimoDispositivo; // user-agent o descripcion del dispositivo

    @Column(name = "ultima_ubicacion")
    private String ultimaUbicacion; // ciudad/pais aproximado (de la IP)

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    // se ejecuta antes de insertar en la bbdd por primera vez
    // nos aseguramos de que ciertos campos tengan valor por defecto
    @PrePersist
    protected void onActorCreate() {
        if (fechaRegistro == null)
            fechaRegistro = LocalDateTime.now();
        if (notificacionConfig == null)
            notificacionConfig = new ActorNotificacionConfig();
    }

    // ---- Getters y Setters — nada especial aqui, solo encapsulamiento standard ----

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

    // @JsonIgnore evita que la contraseña hasheada aparezca en las respuestas JSON
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

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean onboardingCompletado = false;

    public boolean isOnboardingCompletado() {
        return onboardingCompletado;
    }

    public void setOnboardingCompletado(boolean b) {
        this.onboardingCompletado = b;
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

    // getters/setters del resetToken y sesion de dispositivo fusionados
    public String              getResetToken()              { return resetToken; }
    public void                setResetToken(String t)      { this.resetToken = t; }
    public LocalDateTime       getResetTokenExpira()        { return resetTokenExpira; }
    public void                setResetTokenExpira(LocalDateTime e) { this.resetTokenExpira = e; }
    public String              getUltimoIp()                { return ultimoIp; }
    public void                setUltimoIp(String ip)       { this.ultimoIp = ip; }
    public String              getUltimoDispositivo()       { return ultimoDispositivo; }
    public void                setUltimoDispositivo(String d) { this.ultimoDispositivo = d; }
    public String              getUltimaUbicacion()         { return ultimaUbicacion; }
    public void                setUltimaUbicacion(String u) { this.ultimaUbicacion = u; }
    public LocalDateTime       getUltimoLogin()             { return ultimoLogin; }
    public void                setUltimoLogin(LocalDateTime l) { this.ultimoLogin = l; }
}
