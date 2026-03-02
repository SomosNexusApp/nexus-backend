package com.nexus.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Entity
@Table(name = "valoracion",
       uniqueConstraints = @UniqueConstraint(name = "uq_valoracion_compra", columnNames = "compra_id"))
public class Valoracion extends DomainEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comprador_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler","password", "twoFactorSecret", "jwtVersion", "notificacionConfig","cuentaEliminada", "cuentaVerificada"})
    private Usuario comprador;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vendedor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
    "password", "twoFactorSecret", "jwtVersion", "notificacionConfig",
    "cuentaEliminada", "cuentaVerificada"})
    private Actor vendedor;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "compra_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "comprador", "producto"})
    private Compra compra;
    /** int primitivo -> Stream.mapToInt(Valoracion::getPuntuacion) funciona */
    @Column(nullable = false) private int puntuacion;
    @Column(columnDefinition = "TEXT") private String comentario;
    @Column(columnDefinition = "TEXT") private String respuestaVendedor;
    private LocalDateTime fecha;
    private LocalDateTime fechaRespuesta;
    @PrePersist protected void onCreate() { if (fecha == null) fecha = LocalDateTime.now(); }
    public Usuario    getComprador()                       { return comprador; }
    public void       setComprador(Usuario c)              { this.comprador = c; }
    public Actor      getVendedor()                        { return vendedor; }
    public void       setVendedor(Actor v)                 { this.vendedor = v; }
    public Compra     getCompra()                          { return compra; }
    public void       setCompra(Compra c)                  { this.compra = c; }
    public int        getPuntuacion()                      { return puntuacion; }
    public void       setPuntuacion(int p)                 { this.puntuacion = p; }
    public String     getComentario()                      { return comentario; }
    public void       setComentario(String c)              { this.comentario = c; }
    public String     getRespuestaVendedor()               { return respuestaVendedor; }
    public void       setRespuestaVendedor(String r)       { this.respuestaVendedor = r; }
    public LocalDateTime getFecha()                        { return fecha; }
    public void       setFecha(LocalDateTime f)            { this.fecha = f; }
    public LocalDateTime getFechaRespuesta()               { return fechaRespuesta; }
    public void       setFechaRespuesta(LocalDateTime f)   { this.fechaRespuesta = f; }
}
