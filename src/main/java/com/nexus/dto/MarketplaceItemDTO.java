package com.nexus.dto;

import java.util.Date;
import java.util.List;


import com.nexus.entity.TipoVehiculo;

public class MarketplaceItemDTO {
    private Integer id;
    private String titulo;
    private String imagenPrincipal;
    private List<String> galeriaImagenes;
    private Double precio;
    private Double precioOriginal;
    private Double precioOferta;
    private String ubicacion;
    private Date fechaPublicacion;
    private String searchType; // "PRODUCTO", "OFERTA", "VEHICULO"
    
    // Campos extra opcionales
    private String tienda;
    private String badge;
    private Integer sparkCount;
    private String condicion;
    private String estado;
    private Double peso;
    private TipoVehiculo tipoVehiculo;
    private Integer kilometros;
    private Integer anio;

    // Constructor vacío
    public MarketplaceItemDTO() {}

    // Constructor completo
    public MarketplaceItemDTO(Integer id, String titulo, String imagenPrincipal, List<String> galeriaImagenes, 
                             Double precio, Double precioOriginal, Double precioOferta, String ubicacion, 
                             Date fechaPublicacion, String searchType, String tienda, String badge, 
                             Integer sparkCount, String condicion, String estado, Double peso, 
                             TipoVehiculo tipoVehiculo, Integer kilometros, Integer anio) {
        this.id = id;
        this.titulo = titulo;
        this.imagenPrincipal = imagenPrincipal;
        this.galeriaImagenes = galeriaImagenes;
        this.precio = precio;
        this.precioOriginal = precioOriginal;
        this.precioOferta = precioOferta;
        this.ubicacion = ubicacion;
        this.fechaPublicacion = fechaPublicacion;
        this.searchType = searchType;
        this.tienda = tienda;
        this.badge = badge;
        this.sparkCount = sparkCount;
        this.condicion = condicion;
        this.estado = estado;
        this.peso = peso;
        this.tipoVehiculo = tipoVehiculo;
        this.kilometros = kilometros;
        this.anio = anio;
    }

    // Builder manual
    public static class MarketplaceItemDTOBuilder {
        private Integer id;
        private String titulo;
        private String imagenPrincipal;
        private List<String> galeriaImagenes;
        private Double precio;
        private Double precioOriginal;
        private Double precioOferta;
        private String ubicacion;
        private Date fechaPublicacion;
        private String searchType;
        private String tienda;
        private String badge;
        private Integer sparkCount;
        private String condicion;
        private String estado;
        private Double peso;
        private TipoVehiculo tipoVehiculo;
        private Integer kilometros;
        private Integer anio;

        public MarketplaceItemDTOBuilder id(Integer id) { this.id = id; return this; }
        public MarketplaceItemDTOBuilder titulo(String titulo) { this.titulo = titulo; return this; }
        public MarketplaceItemDTOBuilder imagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; return this; }
        public MarketplaceItemDTOBuilder galeriaImagenes(List<String> galeriaImagenes) { this.galeriaImagenes = galeriaImagenes; return this; }
        public MarketplaceItemDTOBuilder precio(Double precio) { this.precio = precio; return this; }
        public MarketplaceItemDTOBuilder precioOriginal(Double precioOriginal) { this.precioOriginal = precioOriginal; return this; }
        public MarketplaceItemDTOBuilder precioOferta(Double precioOferta) { this.precioOferta = precioOferta; return this; }
        public MarketplaceItemDTOBuilder ubicacion(String ubicacion) { this.ubicacion = ubicacion; return this; }
        public MarketplaceItemDTOBuilder fechaPublicacion(Date fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; return this; }
        public MarketplaceItemDTOBuilder searchType(String searchType) { this.searchType = searchType; return this; }
        public MarketplaceItemDTOBuilder tienda(String tienda) { this.tienda = tienda; return this; }
        public MarketplaceItemDTOBuilder badge(String badge) { this.badge = badge; return this; }
        public MarketplaceItemDTOBuilder sparkCount(Integer sparkCount) { this.sparkCount = sparkCount; return this; }
        public MarketplaceItemDTOBuilder condicion(String condicion) { this.condicion = condicion; return this; }
        public MarketplaceItemDTOBuilder estado(String estado) { this.estado = estado; return this; }
        public MarketplaceItemDTOBuilder peso(Double peso) { this.peso = peso; return this; }
        public MarketplaceItemDTOBuilder tipoVehiculo(TipoVehiculo tipoVehiculo) { this.tipoVehiculo = tipoVehiculo; return this; }
        public MarketplaceItemDTOBuilder kilometros(Integer kilometros) { this.kilometros = kilometros; return this; }
        public MarketplaceItemDTOBuilder anio(Integer anio) { this.anio = anio; return this; }

        public MarketplaceItemDTO build() {
            return new MarketplaceItemDTO(id, titulo, imagenPrincipal, galeriaImagenes, precio, precioOriginal, 
                                          precioOferta, ubicacion, fechaPublicacion, searchType, tienda, badge, 
                                          sparkCount, condicion, estado, peso, tipoVehiculo, kilometros, anio);
        }
    }

    public static MarketplaceItemDTOBuilder builder() {
        return new MarketplaceItemDTOBuilder();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getImagenPrincipal() { return imagenPrincipal; }
    public void setImagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; }
    public List<String> getGaleriaImagenes() { return galeriaImagenes; }
    public void setGaleriaImagenes(List<String> galeriaImagenes) { this.galeriaImagenes = galeriaImagenes; }
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public Double getPrecioOriginal() { return precioOriginal; }
    public void setPrecioOriginal(Double precioOriginal) { this.precioOriginal = precioOriginal; }
    public Double getPrecioOferta() { return precioOferta; }
    public void setPrecioOferta(Double precioOferta) { this.precioOferta = precioOferta; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public Date getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(Date fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }
    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
    public String getTienda() { return tienda; }
    public void setTienda(String tienda) { this.tienda = tienda; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public Integer getSparkCount() { return sparkCount; }
    public void setSparkCount(Integer sparkCount) { this.sparkCount = sparkCount; }
    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }
    public TipoVehiculo getTipoVehiculo() { return tipoVehiculo; }
    public void setTipoVehiculo(TipoVehiculo tipoVehiculo) { this.tipoVehiculo = tipoVehiculo; return; }
    public Integer getKilometros() { return kilometros; }
    public void setKilometros(Integer kilometros) { this.kilometros = kilometros; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
}
