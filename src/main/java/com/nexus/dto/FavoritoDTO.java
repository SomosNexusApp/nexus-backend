package com.nexus.dto;

import java.time.LocalDateTime;

public class FavoritoDTO {
    private Integer id;
    private LocalDateTime fechaGuardado;
    private String nota;
    private ProductoResumenDTO producto;
    private VehiculoResumenDTO vehiculo;
    private OfertaResumenDTO oferta; 

    public FavoritoDTO() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getFechaGuardado() { return fechaGuardado; }
    public void setFechaGuardado(LocalDateTime fechaGuardado) { this.fechaGuardado = fechaGuardado; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public ProductoResumenDTO getProducto() { return producto; }
    public void setProducto(ProductoResumenDTO producto) { this.producto = producto; }

    public VehiculoResumenDTO getVehiculo() { return vehiculo; }
    public void setVehiculo(VehiculoResumenDTO vehiculo) { this.vehiculo = vehiculo; }
    
    public OfertaResumenDTO getOferta() { return oferta; }
    public void setOferta(OfertaResumenDTO oferta) { this.oferta = oferta; }
}