package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "vehiculo", indexes = {
    @Index(name = "idx_vehiculo_publicador", columnList = "publicador_id"),
    @Index(name = "idx_vehiculo_categoria",  columnList = "categoria_id"),
    @Index(name = "idx_vehiculo_estado",     columnList = "estadoVehiculo"),
    @Index(name = "idx_vehiculo_tipo",       columnList = "tipoVehiculo")
})
public class Vehiculo extends DomainEntity {

    @Column(nullable = false) private String titulo;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @Column(nullable = false) private Double precio = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOferta tipoOferta = TipoOferta.VENTA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVehiculo tipoVehiculo = TipoVehiculo.COCHE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVehiculo estadoVehiculo = EstadoVehiculo.DISPONIBLE;

    @Enumerated(EnumType.STRING)
    private CondicionProducto condicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "hijos", "parent"})
    private Categoria categoria;

    private String  marca;
    private String  modelo;
    private Integer anio;
    private Integer kilometros;
    private String  combustible;
    private String  cambio;
    private Integer potencia;
    private Integer cilindrada;
    private String  color;
    private Integer numeroPuertas;
    private Integer plazas;
    private String  matricula;
    private Boolean itv;
    private LocalDateTime fechaITV;
    private Boolean garantia;
    private String  ubicacion;

    @Column(columnDefinition = "TEXT")
    private String imagenPrincipal;

    @ElementCollection
    @CollectionTable(name = "vehiculo_imagenes", joinColumns = @JoinColumn(name = "vehiculo_id"))
    @Column(name = "url", columnDefinition = "TEXT")
    private List<String> galeriaImagenes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicador_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
        "cuentaEliminada", "cuentaVerificada"})
    private Actor publicador;

    private LocalDateTime fechaPublicacion;

    @PrePersist
    protected void onCreate() {
        if (fechaPublicacion == null) fechaPublicacion = LocalDateTime.now();
        if (estadoVehiculo   == null) estadoVehiculo   = EstadoVehiculo.DISPONIBLE;
        if (tipoVehiculo     == null) tipoVehiculo     = TipoVehiculo.COCHE;
        if (tipoOferta       == null) tipoOferta       = TipoOferta.VENTA;
        if (galeriaImagenes  == null) galeriaImagenes  = new ArrayList<>();
    }

    public String       getTitulo()                           { return titulo; }
    public void         setTitulo(String t)                   { this.titulo = t; }
    public String       getDescripcion()                      { return descripcion; }
    public void         setDescripcion(String d)              { this.descripcion = d; }
    public Double       getPrecio()                           { return precio; }
    public void         setPrecio(Double p)                   { this.precio = p; }
    public TipoOferta   getTipoOferta()                       { return tipoOferta; }
    public void         setTipoOferta(TipoOferta t)           { this.tipoOferta = t; }
    public TipoVehiculo getTipoVehiculo()                     { return tipoVehiculo; }
    public void         setTipoVehiculo(TipoVehiculo t)       { this.tipoVehiculo = t; }
    public EstadoVehiculo getEstadoVehiculo()                 { return estadoVehiculo; }
    public void         setEstadoVehiculo(EstadoVehiculo e)   { this.estadoVehiculo = e; }
    public CondicionProducto getCondicion()                   { return condicion; }
    public void         setCondicion(CondicionProducto c)     { this.condicion = c; }
    public Categoria    getCategoria()                        { return categoria; }
    public void         setCategoria(Categoria c)             { this.categoria = c; }
    public String       getMarca()                            { return marca; }
    public void         setMarca(String m)                    { this.marca = m; }
    public String       getModelo()                           { return modelo; }
    public void         setModelo(String m)                   { this.modelo = m; }
    public Integer      getAnio()                             { return anio; }
    public void         setAnio(Integer a)                    { this.anio = a; }
    public Integer      getKilometros()                       { return kilometros; }
    public void         setKilometros(Integer k)              { this.kilometros = k; }
    public String       getCombustible()                      { return combustible; }
    public void         setCombustible(String c)              { this.combustible = c; }
    public String       getCambio()                           { return cambio; }
    public void         setCambio(String c)                   { this.cambio = c; }
    public Integer      getPotencia()                         { return potencia; }
    public void         setPotencia(Integer p)                { this.potencia = p; }
    public Integer      getCilindrada()                       { return cilindrada; }
    public void         setCilindrada(Integer c)              { this.cilindrada = c; }
    public String       getColor()                            { return color; }
    public void         setColor(String c)                    { this.color = c; }
    public Integer      getNumeroPuertas()                    { return numeroPuertas; }
    public void         setNumeroPuertas(Integer n)           { this.numeroPuertas = n; }
    public Integer      getPlazas()                           { return plazas; }
    public void         setPlazas(Integer p)                  { this.plazas = p; }
    public String       getMatricula()                        { return matricula; }
    public void         setMatricula(String m)                { this.matricula = m; }
    public Boolean      getItv()                              { return itv; }
    public void         setItv(Boolean i)                     { this.itv = i; }
    public LocalDateTime getFechaITV()                        { return fechaITV; }
    public void         setFechaITV(LocalDateTime f)          { this.fechaITV = f; }
    public Boolean      getGarantia()                         { return garantia; }
    public void         setGarantia(Boolean g)                { this.garantia = g; }
    public String       getUbicacion()                        { return ubicacion; }
    public void         setUbicacion(String u)                { this.ubicacion = u; }
    public String       getImagenPrincipal()                  { return imagenPrincipal; }
    public void         setImagenPrincipal(String i)          { this.imagenPrincipal = i; }
    public List<String> getGaleriaImagenes()                  { return galeriaImagenes; }
    public void         setGaleriaImagenes(List<String> l)    { this.galeriaImagenes = l; }
    public Actor        getPublicador()                       { return publicador; }
    public void         setPublicador(Actor p)                { this.publicador = p; }
    public LocalDateTime getFechaPublicacion()                { return fechaPublicacion; }
    public void         setFechaPublicacion(LocalDateTime f)  { this.fechaPublicacion = f; }

    public void addImagenGaleria(String url) {
        if (galeriaImagenes == null) galeriaImagenes = new ArrayList<>();
        galeriaImagenes.add(url);
    }
}