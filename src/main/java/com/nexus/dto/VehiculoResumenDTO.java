package com.nexus.dto;

public class VehiculoResumenDTO {
    private Integer id;
    private String titulo;
    private Double precio;
    private String imagenPrincipal;
    private String marca;
    private String modelo;
    private Integer kilometros;
    private String estado;

    public VehiculoResumenDTO() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public String getImagenPrincipal() { return imagenPrincipal; }
    public void setImagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getKilometros() { return kilometros; }
    public void setKilometros(Integer kilometros) { this.kilometros = kilometros; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
