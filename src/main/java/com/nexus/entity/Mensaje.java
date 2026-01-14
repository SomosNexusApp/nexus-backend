package com.nexus.entity;

import java.sql.Date;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;

@Entity
public class Mensaje extends DomainEntity{

	@NotNull
	private Date fechaCreacion;
	
	private boolean estaActivo;

	public Mensaje() {
		super();
	}

	public Mensaje(@NotNull Date fechaCreacion, boolean estaActivo) {
		super();
		this.fechaCreacion = fechaCreacion;
		this.estaActivo = estaActivo;
	}

	public Date getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(Date fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public boolean isEstaActivo() {
		return estaActivo;
	}

	public void setEstaActivo(boolean estaActivo) {
		this.estaActivo = estaActivo;
	}
	
	
	
}
