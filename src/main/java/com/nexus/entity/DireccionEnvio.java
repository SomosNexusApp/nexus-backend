package com.nexus.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class DireccionEnvio {

    private String nombre;
    private String apellidos;
    private String direccion;
    private String pisoPuerta;
    private String ciudad;
    private String codigoPostal;
    private String pais;
    private String telefono;

    public DireccionEnvio() {}

    public String getNombre()                 { return nombre; }
    public void   setNombre(String n)         { this.nombre = n; }
    public String getApellidos()              { return apellidos; }
    public void   setApellidos(String a)      { this.apellidos = a; }
    public String getDireccion()              { return direccion; }
    public void   setDireccion(String d)      { this.direccion = d; }
    public String getPisoPuerta()             { return pisoPuerta; }
    public void   setPisoPuerta(String pp)    { this.pisoPuerta = pp; }
    public String getCiudad()                 { return ciudad; }
    public void   setCiudad(String c)         { this.ciudad = c; }
    public String getCodigoPostal()           { return codigoPostal; }
    public void   setCodigoPostal(String cp)  { this.codigoPostal = cp; }
    public String getPais()                   { return pais; }
    public void   setPais(String p)           { this.pais = p; }
    public String getTelefono()               { return telefono; }
    public void   setTelefono(String t)       { this.telefono = t; }
}