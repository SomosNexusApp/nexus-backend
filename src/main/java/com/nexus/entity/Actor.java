package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "actor")
@Inheritance(strategy = InheritanceType.JOINED)
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

    // ---- 2FA -----------------------------------------------------------
    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    private String twoFactorMethod;  // "TOTP" o "EMAIL"
    private String twoFactorSecret;  // Secret TOTP (encriptado)

    // ---- Sesiones -------------------------------------------------------
    @Column(nullable = false)
    private int jwtVersion = 0;

    // ---- Estado de la cuenta --------------------------------------------
    @Column(nullable = false)
    private boolean cuentaEliminada = false;

    @Column(nullable = false)
    private boolean cuentaVerificada = false;

    private LocalDateTime fechaRegistro;

    // ---- Notificaciones -------------------------------------------------
    @Embedded
    private ActorNotificacionConfig notificacionConfig = new ActorNotificacionConfig();

    @PrePersist
    protected void onActorCreate() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
        if (notificacionConfig == null) notificacionConfig = new ActorNotificacionConfig();
    }

    // ---- Getters / Setters -----------------------------------------------

    public String  getUser()                                  { return user; }
    public void    setUser(String u)                          { this.user = u; }
    public String  getEmail()                                 { return email; }
    public void    setEmail(String e)                         { this.email = e; }
    @JsonIgnore
    public String getPassword() { return password; }
    public void    setPassword(String p)                      { this.password = p; }
    
    // Getters y Setters NUEVOS
    public String  getNombre()                                { return nombre; }
    public void    setNombre(String n)                        { this.nombre = n; }
    public String  getApellidos()                             { return apellidos; }
    public void    setApellidos(String a)                     { this.apellidos = a; }
    public String  getTelefono()                              { return telefono; }
    public void    setTelefono(String t)                      { this.telefono = t; }

    public boolean isTwoFactorEnabled()                       { return twoFactorEnabled; }
    public void    setTwoFactorEnabled(boolean b)             { this.twoFactorEnabled = b; }
    public String  getTwoFactorMethod()                       { return twoFactorMethod; }
    public void    setTwoFactorMethod(String m)               { this.twoFactorMethod = m; }
    public String  getTwoFactorSecret()                       { return twoFactorSecret; }
    public void    setTwoFactorSecret(String s)               { this.twoFactorSecret = s; }
    public int     getJwtVersion()                            { return jwtVersion; }
    public void    setJwtVersion(int v)                       { this.jwtVersion = v; }
    public boolean isCuentaEliminada()                        { return cuentaEliminada; }
    public void    setCuentaEliminada(boolean b)              { this.cuentaEliminada = b; }
    public boolean isCuentaVerificada()                       { return cuentaVerificada; }
    public void    setCuentaVerificada(boolean b)             { this.cuentaVerificada = b; }
    public LocalDateTime getFechaRegistro()                   { return fechaRegistro; }
    public void    setFechaRegistro(LocalDateTime f)          { this.fechaRegistro = f; }
    public ActorNotificacionConfig getNotificacionConfig()    { return notificacionConfig; }
    public void    setNotificacionConfig(ActorNotificacionConfig c) { this.notificacionConfig = c; }
}