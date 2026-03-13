package com.nexus.dto;

public class ProductoResumenDTO {
    private Integer id;
    private String titulo;
    private Double precio;
    private String imagenPrincipal;
    private String estado;

    // Constructores
    public ProductoResumenDTO() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public String getImagenPrincipal() { return imagenPrincipal; }
    public void setImagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}