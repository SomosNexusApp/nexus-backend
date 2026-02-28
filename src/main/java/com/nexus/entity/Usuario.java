package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario")
@PrimaryKeyJoinColumn(name = "actor_id")
public class Usuario extends Actor {

    @Column(columnDefinition = "TEXT")
    private String avatar;

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

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "nombre",       column = @Column(name = "dir_nombre")),
        @AttributeOverride(name = "direccion",    column = @Column(name = "dir_calle")),
        @AttributeOverride(name = "ciudad",       column = @Column(name = "dir_ciudad")),
        @AttributeOverride(name = "codigoPostal", column = @Column(name = "dir_cp")),
        @AttributeOverride(name = "pais",         column = @Column(name = "dir_pais")),
        @AttributeOverride(name = "telefono",     column = @Column(name = "dir_telefono"))
    })
    private DireccionEnvio direccionPorDefecto;

    @ElementCollection
    @CollectionTable(name = "usuario_bloqueados",
                     joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "bloqueado_id")
    private List<Integer> blockedUserIds = new ArrayList<>();

    public Usuario() {}

    // ---- Getters / Setters -----------------------------------------------

    public String  getAvatar()                              { return avatar; }
    public void    setAvatar(String a)                      { this.avatar = a; }
    public String  getBiografia()                           { return biografia; }
    public void    setBiografia(String b)                   { this.biografia = b; }
    public String  getUbicacion()                           { return ubicacion; }
    public void    setUbicacion(String u)                   { this.ubicacion = u; }
    
    // Getters y Setters NUEVOS
    public TipoCuenta getTipoCuenta()                       { return tipoCuenta; }
    public void       setTipoCuenta(TipoCuenta t)           { this.tipoCuenta = t; }
    public boolean    isCuentaPrivada()                     { return cuentaPrivada; }
    public void       setCuentaPrivada(boolean b)           { this.cuentaPrivada = b; }
    public boolean    isTerminosAceptados()                 { return terminosAceptados; }
    public void       setTerminosAceptados(boolean b)       { this.terminosAceptados = b; }
    public boolean    isNewsletterSuscrito()                { return newsletterSuscrito; }
    public void       setNewsletterSuscrito(boolean b)      { this.newsletterSuscrito = b; }
    public String     getGoogleId()                         { return googleId; }
    public void       setGoogleId(String g)                 { this.googleId = g; }
    public String     getFacebookId()                       { return facebookId; }
    public void       setFacebookId(String f)               { this.facebookId = f; }
    public String     getVersionTerminosAceptados()         { return versionTerminosAceptados; }
    public void       setVersionTerminosAceptados(String v) { this.versionTerminosAceptados = v; }
    public LocalDateTime getFechaAceptacionTerminos()       { return fechaAceptacionTerminos; }
    public void       setFechaAceptacionTerminos(LocalDateTime d) { this.fechaAceptacionTerminos = d; }

    public double  getReputacion()                          { return reputacion; }
    public void    setReputacion(double r)                  { this.reputacion = r; }
    public int     getTotalVentas()                         { return totalVentas; }
    public void    setTotalVentas(int t)                    { this.totalVentas = t; }
    public boolean isEsVerificado()                         { return esVerificado; }
    public void    setEsVerificado(boolean v)               { this.esVerificado = v; }
    public boolean isPerfilPublico()                        { return perfilPublico; }
    public void    setPerfilPublico(boolean b)              { this.perfilPublico = b; }
    public boolean isMostrarTelefono()                      { return mostrarTelefono; }
    public void    setMostrarTelefono(boolean b)            { this.mostrarTelefono = b; }
    public boolean isMostrarUbicacion()                     { return mostrarUbicacion; }
    public void    setMostrarUbicacion(boolean b)           { this.mostrarUbicacion = b; }
    public DireccionEnvio getDireccionPorDefecto()          { return direccionPorDefecto; }
    public void    setDireccionPorDefecto(DireccionEnvio d) { this.direccionPorDefecto = d; }
    public List<Integer> getBlockedUserIds()                { return blockedUserIds; }
    public void    setBlockedUserIds(List<Integer> l)       { this.blockedUserIds = l; }
}