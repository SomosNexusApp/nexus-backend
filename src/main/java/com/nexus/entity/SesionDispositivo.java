package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SesionDispositivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer actorId;
    private String ip;
    private String dispositivo;
    private String ubicacion;
    private LocalDateTime fechaLogin;
    private String tokenReference; // Opcional, para identificar el JWT

    public SesionDispositivo() {
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getActorId() {
        return actorId;
    }

    public void setActorId(Integer actorId) {
        this.actorId = actorId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public LocalDateTime getFechaLogin() {
        return fechaLogin;
    }

    public void setFechaLogin(LocalDateTime fechaLogin) {
        this.fechaLogin = fechaLogin;
    }

    public String getTokenReference() {
        return tokenReference;
    }

    public void setTokenReference(String tokenReference) {
        this.tokenReference = tokenReference;
    }
}