package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
  "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
  "cuentaEliminada", "cuentaVerificada"})
    private Actor actor;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    public PasswordResetToken() {}

    public Integer getId()                       { return id; }
    public String getToken()                     { return token; }
    public void setToken(String token)           { this.token = token; }
    public Actor getActor()                      { return actor; }
    public void setActor(Actor actor)            { this.actor = actor; }
    public LocalDateTime getExpiraEn()           { return expiraEn; }
    public void setExpiraEn(LocalDateTime e)     { this.expiraEn = e; }
}