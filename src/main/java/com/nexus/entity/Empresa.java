package com.nexus.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "empresa")
@PrimaryKeyJoinColumn(name = "actor_id")
public class Empresa extends Actor {

    @Column(unique = true)
    private String cif;

    private String nombreComercial;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String web;
    private String telefono;
    
    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String logo;

    @Column(nullable = false)
    private boolean verificada = false;

    public Empresa() {}

    public String  getCif()                    { return cif; }
    public void    setCif(String c)            { this.cif = c; }
    public String  getNombreComercial()        { return nombreComercial; }
    public void    setNombreComercial(String n){ this.nombreComercial = n; }
    public String  getDescripcion()            { return descripcion; }
    public void    setDescripcion(String d)    { this.descripcion = d; }
    public String  getWeb()                    { return web; }
    public void    setWeb(String w)            { this.web = w; }
    public String  getTelefono()               { return telefono; }
    public void    setTelefono(String t)       { this.telefono = t; }
    public String  getLogo()                   { return logo; }
    public void    setLogo(String l)           { this.logo = l; }
    public boolean isVerificada()              { return verificada; }
    public void    setVerificada(boolean v)    { this.verificada = v; }

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}
    
    

}