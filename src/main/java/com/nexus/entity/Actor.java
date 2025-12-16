package com.nexus.entity;


import java.time.LocalDateTime;
import java.util.List;

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
public abstract class Actor extends DomainEntity{

	@NotBlank
	@Column(name = "username", unique = true) 
	private String user;
	
	@NotBlank
	@Pattern(regexp = "^\\w[@]\\w[.]\\w$")
	@Column(unique = true) 
	private String email;
	
	@NotBlank
	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=])(?=\\\\S+$).{8,}$")
	private String password;
	
	private LocalDateTime fechaRegistro;
	
	@OneToMany(mappedBy = "actor", cascade = CascadeType.ALL)
    private List<Oferta> ofertasPublicadas;

	public Actor() {
		super();
	}

	public Actor(@NotBlank String user, @NotBlank @Pattern(regexp = "^\\w[@]\\w[.]\\w$") String email,
			@NotBlank @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=])(?=\\\\S+$).{8,}$") String password,
			@NotBlank LocalDateTime fechaRegistro) {
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

	
	@PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }
}
