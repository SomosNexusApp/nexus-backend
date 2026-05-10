package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// entidad principal para los productos de segunda mano del marketplace
// tiene indices en vendedor, estado y categoria para que las busquedas vayan rapido
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
    private TipoOferta tipoOferta = TipoOferta.VENTA; // VENTA, DONACION o INTERCAMBIO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProducto estado = EstadoProducto.DISPONIBLE;

    @Enumerated(EnumType.STRING)
    private CondicionProducto condicion; // NUEVO, COMO_NUEVO, BUENO, ACEPTABLE, PARA_PIEZAS

    // usamos EAGER para que la categoria y el vendedor carguen siempre con el producto
    // y ponemos @JsonIgnoreProperties para evitar bucles infinitos en la serializacion JSON

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "hijos", "parent"})
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendedor_id", nullable = false)
    @JsonIgnoreProperties({
        "hibernateLazyInitializer", "handler",
        "password", "twoFactorSecret", "jwtVersion", // campos sensibles que no deben exponerse
        "notificacionConfig", "cuentaEliminada", "cuentaVerificada"
    })
    private Actor vendedor;

    // ── campos propios de productos de segunda mano ──────────────────────────────────

    private String  marca;
    private String  modelo;
    private Double  peso; // en kilos, se usa para calcular el precio de envio
    private Boolean admiteEnvio    = false;
    private Double  precioEnvio    = 0.0; // si el vendedor lo manda con envio, cuanto cobra
    private Boolean precioNegociable = false;
    private String  ubicacion;
    private Double  latitude;  // coordenadas para el mapa (pueden ser null si no se proporcionan)
    private Double  longitude;

    @Column(name = "numero_vistas", nullable = false)
    private int numeroVistas = 0;

    @Column(name = "numero_favoritos", nullable = false)
    private int numeroFavoritos = 0;

    // ── imagenes ─────────────────────────────────────────────────────────────
    // la imagen principal va como campo propio para mostrarla en las tarjetas
    // las demas van en una tabla separada (producto_imagenes)

    @Column(columnDefinition = "TEXT")
    private String imagenPrincipal;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "producto_imagenes", joinColumns = @JoinColumn(name = "producto_id"))
    @Column(name = "url", columnDefinition = "TEXT")
    private List<String> galeriaImagenes = new ArrayList<>();

    // ── fechas ────────────────────────────────────────────────────────────────

    private LocalDateTime fechaPublicacion;

    /** Fecha limite del anuncio. Si pasa, el estado cambia a EXPIRADO automaticamente. */
    private LocalDateTime fechaCaducidad;

    /** Ultimo hito de caducidad notificado (dias: 30, 14, 7 o 1). Null si no se ha avisado aun. */
    private Integer ultimoAvisoCaducidadDias;

    // ── campos de gestion admin ─────────────────────────────────────────────

    /** Si el admin pausa el producto, hasta cuando dura la pausa. Null = no pausado por admin. */
    private LocalDateTime pausadoHasta;

    /** Motivo de la pausa (lo ve el vendedor en su panel). */
    @Column(columnDefinition = "TEXT")
    private String motivoPausa;

    /** si esta a true, el producto sale el primero en busquedas y en la home. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean destacado = false;

    /** si tiene un contrato publicitario activo, aparece con la etiqueta 'Patrocinado'. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean patrocinado = false;

    /** Fecha hasta la que dura el patrocinio (null = indefinido). */
    private LocalDateTime patrocinioHasta;

    /** cuando se vendio, se usa para calcular cuando hay que ocultar el producto de la lista. */
    private LocalDateTime fechaVenta;

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

    // se ejecuta antes de insertar en la bbdd.
    // establece los valores por defecto para todos los campos que pueden venir null
    @PrePersist
    protected void onCreate() {
        if (fechaPublicacion == null) fechaPublicacion = LocalDateTime.now();
        if (fechaCaducidad == null) fechaCaducidad = fechaPublicacion.plusDays(180); // 6 meses de vida
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

    public LocalDateTime    getPatrocinioHasta()               { return patrocinioHasta; }
    public void             setPatrocinioHasta(LocalDateTime f){ this.patrocinioHasta = f; }

    public LocalDateTime    getFechaVenta()                     { return fechaVenta; }
    public void             setFechaVenta(LocalDateTime f)      { this.fechaVenta = f; }

    /** Calculado: días que faltan para que desaparezca de la app si está vendido. */
    public Long getDiasRestantesVendido() {
        if (estado != EstadoProducto.VENDIDO || fechaVenta == null) return null;
        long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDateTime.now(), fechaVenta.plusDays(14));
        return Math.max(0, days);
    }

    public void addImagenGaleria(String url) {
        if (galeriaImagenes == null) galeriaImagenes = new ArrayList<>();
        galeriaImagenes.add(url);
    }

    /** Calculado: días que faltan para que desaparezca de la app si está expirado. */
    public Long getDiasRestantesExpirado() {
        if (estado != EstadoProducto.EXPIRADO || fechaCaducidad == null) return null;
        long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDateTime.now(), fechaCaducidad.plusDays(14));
        return Math.max(0, days);
    }
}