package com.nexus.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Actor extends DomainEntity {

    @NotBlank
    @Column(name = "username", unique = true) 
    private String user;
    
    @NotBlank
    @Pattern(regexp = "^\\w[@]\\w[.]\\w$")
    @Column(unique = true) 
    private String email;
    
    @NotBlank
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=])(?=\\\\S+$).{8,}$")
    @JsonIgnore 
    private String password;
    
    private LocalDateTime fechaRegistro;
    
    // --- NUEVO CAMPO PARA CÓDIGO VERIFICACIÓN ---
    @JsonIgnore // No queremos que esto salga en el JSON
    @Column(name = "codigo_verificacion")
    private String codigoVerificacion;
    
    @OneToMany(mappedBy = "actor", cascade = CascadeType.ALL)
    private List<Oferta> ofertasPublicadas;

    public Actor() {
        super();
    }
    
    // Constructor completo
    public Actor(String user, String email, String password, LocalDateTime fechaRegistro) {
        super();
        this.user = user;
        this.email = email;
        this.password = password;
        this.fechaRegistro = fechaRegistro;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public void setCodigoVerificacion(String codigoVerificacion) {
        this.codigoVerificacion = codigoVerificacion;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }
    
    public List<Oferta> getOfertasPublicadas() {
        return ofertasPublicadas;
    }

    public void setOfertasPublicadas(List<Oferta> ofertasPublicadas) {
        this.ofertasPublicadas = ofertasPublicadas;
    }
}