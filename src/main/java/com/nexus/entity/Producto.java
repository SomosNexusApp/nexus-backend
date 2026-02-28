package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Producto de segunda mano.
 *
 * ALIAS necesarios (el campo real es "estado" y "vendedor",
 * pero los controllers/services usan los nombres alternativos):
 *
 *   getEstadoProducto()            alias de getEstado()
 *   setEstadoProducto(EstadoProducto) alias de setEstado()
 *   getPublicador()                alias de getVendedor()
 *   setPublicador(Usuario)         alias de setVendedor()
 *
 * Requeridos en:
 *   CompraController  lines 76, 82
 *   CompraService     lines 66, 77, 108
 *   DevolucionService lines 60, 139, 158
 *   EnvioService      lines 186, 213, 217, 225
 *   ProductoController lines 92, 94
 *   ProductoService   lines 41, 53, 59, 60
 */
@Entity
@Table(name = "producto", indexes = {
    @Index(name = "idx_producto_vendedor",  columnList = "vendedor_id"),
    @Index(name = "idx_producto_categoria", columnList = "categoria_id"),
    @Index(name = "idx_producto_estado",    columnList = "estado"),
    @Index(name = "idx_producto_precio",    columnList = "precio")
})
public class Producto extends DomainEntity {

    public Producto() {}

    public Producto(String titulo, String descripcion, Double precio,
                    TipoOferta tipoOferta, Actor vendedor, String imagenPrincipal) {
        this.titulo = titulo; this.descripcion = descripcion; this.precio = precio;
        this.tipoOferta = tipoOferta; this.vendedor = vendedor; this.imagenPrincipal = imagenPrincipal;
        this.estado = EstadoProducto.DISPONIBLE;
    }

    @Column(nullable = false) private String titulo;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @Column(nullable = false) private Double precio;

    @Enumerated(EnumType.STRING)
    private TipoOferta tipoOferta = TipoOferta.VENTA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProducto estado = EstadoProducto.DISPONIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Actor vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private String marca, modelo, talla, color;

    @Enumerated(EnumType.STRING)
    private CondicionProducto condicion = CondicionProducto.BUEN_ESTADO;

    @Column(columnDefinition = "TEXT")
    private String imagenPrincipal;

    @ElementCollection
    @CollectionTable(name = "producto_imagenes", joinColumns = @JoinColumn(name = "producto_id"))
    @Column(name = "url", columnDefinition = "TEXT")
    private List<String> galeriaImagenes = new ArrayList<>();

    private String ubicacion;
    private Boolean admiteEnvio = false;
    private Double precioEnvio = 0.0;

    @Column(nullable = false)
    private boolean precioNegociable = false;

    @Column(nullable = false) private Integer numeroVistas = 0;
    @Column(nullable = false) private Integer numeroFavoritos = 0;

    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaActualizacion;

    @PrePersist @PreUpdate
    protected void onSave() {
        if (fechaPublicacion == null) fechaPublicacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (estado == null) estado = EstadoProducto.DISPONIBLE;
        if (galeriaImagenes == null) galeriaImagenes = new ArrayList<>();
        if (admiteEnvio == null) admiteEnvio = false;
        if (numeroVistas == null) numeroVistas = 0;
        if (numeroFavoritos == null) numeroFavoritos = 0;
    }

    // ---- Getters / Setters canonicos -----------------------------------

    public String          getTitulo()                              { return titulo; }
    public void            setTitulo(String t)                      { this.titulo = t; }
    public String          getDescripcion()                         { return descripcion; }
    public void            setDescripcion(String d)                 { this.descripcion = d; }
    public Double          getPrecio()                              { return precio; }
    public void            setPrecio(Double p)                      { this.precio = p; }
    public TipoOferta      getTipoOferta()                          { return tipoOferta; }
    public void            setTipoOferta(TipoOferta t)              { this.tipoOferta = t; }

    public EstadoProducto  getEstado()                              { return estado; }
    public void            setEstado(EstadoProducto e)              { this.estado = e; }

    /** Alias requerido por CompraController, CompraService, DevolucionService, ProductoService, ProductoController */
    public EstadoProducto  getEstadoProducto()                      { return estado; }
    public void            setEstadoProducto(EstadoProducto e)      { this.estado = e; }

    public Actor           getVendedor()                            { return vendedor; }
    public void            setVendedor(Actor v)                     { this.vendedor = v; }

    /** Alias requerido por Compra.java, CompraController, DevolucionService, EnvioService, ProductoService */
    public Actor           getPublicador()                          { return vendedor; }
    public void            setPublicador(Actor v)                   { this.vendedor = v; }

    public Categoria       getCategoria()                           { return categoria; }
    public void            setCategoria(Categoria c)                { this.categoria = c; }
    public String          getMarca()                               { return marca; }
    public void            setMarca(String m)                       { this.marca = m; }
    public String          getModelo()                              { return modelo; }
    public void            setModelo(String m)                      { this.modelo = m; }
    public String          getTalla()                               { return talla; }
    public void            setTalla(String t)                       { this.talla = t; }
    public String          getColor()                               { return color; }
    public void            setColor(String c)                       { this.color = c; }
    public CondicionProducto getCondicion()                         { return condicion; }
    public void            setCondicion(CondicionProducto c)        { this.condicion = c; }
    public String          getImagenPrincipal()                     { return imagenPrincipal; }
    public void            setImagenPrincipal(String i)             { this.imagenPrincipal = i; }
    public List<String>    getGaleriaImagenes()                     { return galeriaImagenes; }
    public void            setGaleriaImagenes(List<String> g)       { this.galeriaImagenes = g; }
    public String          getUbicacion()                           { return ubicacion; }
    public void            setUbicacion(String u)                   { this.ubicacion = u; }
    public Boolean         getAdmiteEnvio()                         { return admiteEnvio; }
    public void            setAdmiteEnvio(boolean a)                { this.admiteEnvio = a; }
    public void            setAdmiteEnvio(Boolean a)                { this.admiteEnvio = a; }
    public Double          getPrecioEnvio()                         { return precioEnvio; }
    public void            setPrecioEnvio(Double p)                 { this.precioEnvio = p; }
    public boolean         isPrecioNegociable()                     { return precioNegociable; }
    public void            setPrecioNegociable(boolean p)           { this.precioNegociable = p; }
    public void            setPrecioNegociable(Boolean p)           { this.precioNegociable = Boolean.TRUE.equals(p); }
    public Integer         getNumeroVistas()                        { return numeroVistas; }
    public void            setNumeroVistas(Integer n)               { this.numeroVistas = n; }
    public Integer         getNumeroFavoritos()                     { return numeroFavoritos; }
    public void            setNumeroFavoritos(Integer n)            { this.numeroFavoritos = n; }
    public LocalDateTime   getFechaPublicacion()                    { return fechaPublicacion; }
    public void            setFechaPublicacion(LocalDateTime f)     { this.fechaPublicacion = f; }
    public LocalDateTime   getFechaActualizacion()                  { return fechaActualizacion; }

    public void addImagenGaleria(String url) {
        if (galeriaImagenes == null) galeriaImagenes = new ArrayList<>();
        galeriaImagenes.add(url);
    }
}
