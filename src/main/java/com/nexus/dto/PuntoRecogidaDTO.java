package com.nexus.dto;

public class PuntoRecogidaDTO {
    private String nombre;
    private String direccion;
    private String ciudad;
    private String horario;
    private String transportista;

    public PuntoRecogidaDTO() {}

    public PuntoRecogidaDTO(String nombre, String direccion, String ciudad, String horario, String transportista) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.horario = horario;
        this.transportista = transportista;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
    public String getTransportista() { return transportista; }
    public void setTransportista(String transportista) { this.transportista = transportista; }
}
