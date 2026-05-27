package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Almacena tokens FCM por actor para enviar notificaciones push nativas.
 * Un actor puede tener varios tokens si usa la app en múltiples dispositivos.
 */
@Entity
@Table(name = "push_token", uniqueConstraints = {
    @UniqueConstraint(columnNames = "token")
})
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private Actor actor;

    /** Token FCM del dispositivo, único por dispositivo. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String token;

    /** Plataforma: "android" o "ios". */
    @Column(nullable = false, length = 10)
    private String plataforma;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /** Se pone a false cuando FCM reporta el token como inválido/expirado. */
    @Column(nullable = false)
    private boolean activo = true;

    // ---- Getters y Setters ---------------------------------------------------

    public Integer getId() { return id; }

    public Actor getActor() { return actor; }
    public void setActor(Actor actor) { this.actor = actor; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
