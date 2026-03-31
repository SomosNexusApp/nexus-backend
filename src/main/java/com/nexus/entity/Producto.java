package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "producto", indexes = {
    @Index(name = "idx_producto_vendedor",  columnList = "vendedor_id"),
    @Index(name = "idx_producto_estado",    columnList = "estado"),
    @Index(name = "idx_producto_categoria", columnList = "categoria_id")
})
public class Producto extends DomainEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Double precio = 0.0;

    @Enumerated(EnumType.STRING)
    private TipoOferta tipoOferta = TipoOferta.VENTA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProducto estado = EstadoProducto.DISPONIBLE;

    @Enumerated(EnumType.STRING)
    private CondicionProducto condicion;

    // ── Asociaciones EAGER + @JsonIgnoreProperties para evitar LazyInitializationException ──

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "hijos", "parent"})
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendedor_id", nullable = false)
    @JsonIgnoreProperties({
        "hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion",
        "notificacionConfig", "cuentaEliminada", "cuentaVerificada"
    })
    private Actor vendedor;

    // ── Campos de producto de segunda mano ──────────────────────────────────

    private String  marca;
    private String  modelo;
    private Double  peso;
    private Boolean admiteEnvio    = false;
    private Double  precioEnvio    = 0.0;
    private Boolean precioNegociable = false;
    private String  ubicacion;
    private Double  latitude;
    private Double  longitude;

    @Column(name = "numero_vistas", nullable = false)
    private int numeroVistas = 0;

    @Column(name = "numero_favoritos", nullable = false)
    private int numeroFavoritos = 0;

    // ── Imágenes ─────────────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String imagenPrincipal;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "producto_imagenes", joinColumns = @JoinColumn(name = "producto_id"))
    @Column(name = "url", columnDefinition = "TEXT")
    private List<String> galeriaImagenes = new ArrayList<>();

    // ── Fechas ────────────────────────────────────────────────────────────────

    private LocalDateTime fechaPublicacion;

    /** Fin de vigencia del anuncio (estilo Wallapop). Si pasa, pasa a EXPIRADO. */
    private LocalDateTime fechaCaducidad;

    /** Último hito de aviso enviado (días restantes: 30, 14, 7 o 1). Null = ninguno. */
    private Integer ultimoAvisoCaducidadDias;

    // ── Campos admin ─────────────────────────────────────────────────────────

    /** Fecha hasta la que el producto está pausado por el admin. Null = no pausado por admin. */
    private LocalDateTime pausadoHasta;

    /** Motivo de la pausa admin (visible al vendedor). */
    @Column(columnDefinition = "TEXT")
    private String motivoPausa;

    /** True = aparece primero en búsquedas y home. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean destacado = false;

    /** Contrato de publicidad pagado: prioridad en listados y etiqueta Patrocinado. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean patrocinado = false;

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════

    public Producto() {}

    /**
     * Constructor usado en PopulateDB:
     * new Producto(titulo, descripcion, precio, tipoOferta, vendedor, imagenPrincipal)
     */
    public Producto(String titulo, String descripcion, Double precio,
                    TipoOferta tipoOferta, Actor vendedor, String imagenPrincipal) {
        this.titulo          = titulo;
        this.descripcion     = descripcion;
        this.precio          = precio;
        this.tipoOferta      = tipoOferta;
        this.vendedor        = vendedor;
        this.imagenPrincipal = imagenPrincipal;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    @PrePersist
    protected void onCreate() {
        if (fechaPublicacion == null) fechaPublicacion = LocalDateTime.now();
        if (fechaCaducidad == null) fechaCaducidad = fechaPublicacion.plusDays(180);
        if (estado           == null) estado           = EstadoProducto.DISPONIBLE;
        if (tipoOferta       == null) tipoOferta       = TipoOferta.VENTA;
        if (galeriaImagenes  == null) galeriaImagenes  = new ArrayList<>();
        if (admiteEnvio      == null) admiteEnvio      = false;
        if (precioEnvio      == null) precioEnvio      = 0.0;
        if (precioNegociable == null) precioNegociable = false;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GETTERS / SETTERS
    // ═══════════════════════════════════════════════════════════════════════

    public String           getTitulo()                         { return titulo; }
    public void             setTitulo(String t)                 { this.titulo = t; }

    public String           getDescripcion()                    { return descripcion; }
    public void             setDescripcion(String d)            { this.descripcion = d; }

    public Double           getPrecio()                         { return precio; }
    public void             setPrecio(Double p)                 { this.precio = p; }

    public TipoOferta       getTipoOferta()                     { return tipoOferta; }
    public void             setTipoOferta(TipoOferta t)         { this.tipoOferta = t; }

    public EstadoProducto   getEstado()                         { return estado; }
    public void             setEstado(EstadoProducto e)         { this.estado = e; }

    /** Alias para compatibilidad con código existente */
    public EstadoProducto   getEstadoProducto()                 { return estado; }
    public void             setEstadoProducto(EstadoProducto e) { this.estado = e; }

    public CondicionProducto getCondicion()                     { return condicion; }
    public void             setCondicion(CondicionProducto c)   { this.condicion = c; }

    public Categoria        getCategoria()                      { return categoria; }
    public void             setCategoria(Categoria c)           { this.categoria = c; }

    /**
     * Campo principal del vendedor.
     * getPublicador() es alias necesario para Compra, DevolucionService y EnvioService.
     */
    public Actor            getVendedor()                       { return vendedor; }
    public void             setVendedor(Actor v)                { this.vendedor = v; }

    /** Alias getPublicador() — usado en Compra, DevolucionService, EnvioService */
    public Actor            getPublicador()                     { return vendedor; }
    public void             setPublicador(Actor v)              { this.vendedor = v; }

    public String           getMarca()                          { return marca; }
    public void             setMarca(String m)                  { this.marca = m; }

    public String           getModelo()                         { return modelo; }
    public void             setModelo(String m)                 { this.modelo = m; }

    public Double           getPeso()                           { return peso; }
    public void             setPeso(Double p)                   { this.peso = p; }

    public Boolean          getAdmiteEnvio()                    { return admiteEnvio; }
    public void             setAdmiteEnvio(Boolean a)           { this.admiteEnvio = a; }
    /** Sobrecarga primitiva para PopulateDB */
    public void             setAdmiteEnvio(boolean a)           { this.admiteEnvio = a; }

    public Double           getPrecioEnvio()                    { return precioEnvio; }
    public void             setPrecioEnvio(Double p)            { this.precioEnvio = p; }

    public Boolean          getPrecioNegociable()               { return precioNegociable; }
    public void             setPrecioNegociable(Boolean p)      { this.precioNegociable = p; }
    /** Sobrecarga primitiva para PopulateDB */
    public void             setPrecioNegociable(boolean p)      { this.precioNegociable = p; }

    public String           getUbicacion()                      { return ubicacion; }
    public void             setUbicacion(String u)              { this.ubicacion = u; }

    public Double           getLatitude()                       { return latitude; }
    public void             setLatitude(Double l)               { this.latitude = l; }

    public Double           getLongitude()                      { return longitude; }
    public void             setLongitude(Double l)              { this.longitude = l; }

    public String           getImagenPrincipal()                { return imagenPrincipal; }
    public void             setImagenPrincipal(String i)        { this.imagenPrincipal = i; }

    public List<String>     getGaleriaImagenes()                { return galeriaImagenes; }
    public void             setGaleriaImagenes(List<String> l)  { this.galeriaImagenes = l; }

    public LocalDateTime    getFechaPublicacion()               { return fechaPublicacion; }
    public void             setFechaPublicacion(LocalDateTime f){ this.fechaPublicacion = f; }

    public LocalDateTime    getFechaCaducidad()                 { return fechaCaducidad; }
    public void             setFechaCaducidad(LocalDateTime f)  { this.fechaCaducidad = f; }

    public Integer          getUltimoAvisoCaducidadDias()       { return ultimoAvisoCaducidadDias; }
    public void             setUltimoAvisoCaducidadDias(Integer u) { this.ultimoAvisoCaducidadDias = u; }

    public LocalDateTime    getPausadoHasta()                   { return pausadoHasta; }
    public void             setPausadoHasta(LocalDateTime p)    { this.pausadoHasta = p; }

    public String           getMotivoPausa()                    { return motivoPausa; }
    public void             setMotivoPausa(String m)            { this.motivoPausa = m; }

    public Boolean          getDestacado()                      { return destacado != null && destacado; }
    public void             setDestacado(Boolean d)             { this.destacado = d != null ? d : false; }

    public Boolean          getPatrocinado()                    { return patrocinado != null && patrocinado; }
    public void             setPatrocinado(Boolean p)          { this.patrocinado = p != null ? p : false; }

    public void addImagenGaleria(String url) {
        if (galeriaImagenes == null) galeriaImagenes = new ArrayList<>();
        galeriaImagenes.add(url);
    }
}