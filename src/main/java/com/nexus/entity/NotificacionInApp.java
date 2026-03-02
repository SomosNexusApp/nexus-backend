package com.nexus.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
/**
 * Notificacion in-app.
 * NOMBRE: NotificacionInApp  (el repo extiende CrudRepository<NotificacionInApp,Integer>)
 * NotificacionService debe crear objetos de ESTE tipo, no de "Notificacion".
 */
@Entity
@Table(name = "notificacion_in_app", indexes = {
    @Index(name = "idx_notif_actor", columnList = "actor_id"),
    @Index(name = "idx_notif_leida", columnList = "leida")
})
public class NotificacionInApp extends DomainEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    private Actor actor;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TipoNotificacion tipo;
    @Column(nullable = false) private String titulo;
    @Column(columnDefinition = "TEXT") private String mensaje;
    private String url;
    @Column(nullable = false) private boolean leida = false;
    @Column(nullable = false) private LocalDateTime fecha;
    @PrePersist protected void onCreate() { if (fecha == null) fecha = LocalDateTime.now(); }

    public Actor            getActor()                       { return actor; }
    public void             setActor(Actor a)                { this.actor = a; }
    public TipoNotificacion getTipo()                        { return tipo; }
    public void             setTipo(TipoNotificacion t)      { this.tipo = t; }
    public String           getTitulo()                      { return titulo; }
    public void             setTitulo(String t)              { this.titulo = t; }
    public String           getMensaje()                     { return mensaje; }
    public void             setMensaje(String m)             { this.mensaje = m; }
    public String           getUrl()                         { return url; }
    public void             setUrl(String u)                 { this.url = u; }
    public boolean          isLeida()                        { return leida; }
    public void             setLeida(boolean l)              { this.leida = l; }
    public LocalDateTime    getFecha()                       { return fecha; }
    public void             setFecha(LocalDateTime f)        { this.fecha = f; }
}
