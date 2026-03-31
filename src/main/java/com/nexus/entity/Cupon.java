package com.nexus.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupon")
public class Cupon extends DomainEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDescuento tipo;

    private BigDecimal valor;

    @Column(name = "valor_fijo")
    private BigDecimal valorFijo;

    @Column(name = "valor_porcentaje")
    private BigDecimal valorPorcentaje;

    @Column(name = "importe_minimo")
    private BigDecimal importeMinimo;

    @Column(name = "tope_maximo")
    private BigDecimal topeMaximo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlcanceCupon alcance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Actor usuario;

    @Column(name = "grupo_objetivo")
    private String grupoObjetivo;

    @Column(name = "limite_uso_total")
    private Integer limiteUsoTotal;

    @Column(name = "limite_uso_por_usuario")
    private Integer limiteUsoPorUsuario = 1;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "categorias_ids")
    private String categoriasIds;

    @Column(name = "descripcion_interna", nullable = false)
    private String descripcionInterna;

    private boolean activo = true;

    @Column(name = "total_usos")
    private int totalUsos = 0;

    @Column(name = "creado_por_admin_id")
    private Long creadoPorAdminId;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
    }

    // Getters and Setters
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public TipoDescuento getTipo() { return tipo; }
    public void setTipo(TipoDescuento tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public BigDecimal getValorFijo() { return valorFijo; }
    public void setValorFijo(BigDecimal valorFijo) { this.valorFijo = valorFijo; }

    public BigDecimal getValorPorcentaje() { return valorPorcentaje; }
    public void setValorPorcentaje(BigDecimal valorPorcentaje) { this.valorPorcentaje = valorPorcentaje; }

    public BigDecimal getImporteMinimo() { return importeMinimo; }
    public void setImporteMinimo(BigDecimal importeMinimo) { this.importeMinimo = importeMinimo; }

    public BigDecimal getTopeMaximo() { return topeMaximo; }
    public void setTopeMaximo(BigDecimal topeMaximo) { this.topeMaximo = topeMaximo; }

    public AlcanceCupon getAlcance() { return alcance; }
    public void setAlcance(AlcanceCupon alcance) { this.alcance = alcance; }

    public Actor getUsuario() { return usuario; }
    public void setUsuario(Actor usuario) { this.usuario = usuario; }

    public String getGrupoObjetivo() { return grupoObjetivo; }
    public void setGrupoObjetivo(String grupoObjetivo) { this.grupoObjetivo = grupoObjetivo; }

    public Integer getLimiteUsoTotal() { return limiteUsoTotal; }
    public void setLimiteUsoTotal(Integer limiteUsoTotal) { this.limiteUsoTotal = limiteUsoTotal; }

    public Integer getLimiteUsoPorUsuario() { return limiteUsoPorUsuario; }
    public void setLimiteUsoPorUsuario(Integer limiteUsoPorUsuario) { this.limiteUsoPorUsuario = limiteUsoPorUsuario; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getCategoriasIds() { return categoriasIds; }
    public void setCategoriasIds(String categoriasIds) { this.categoriasIds = categoriasIds; }

    public String getDescripcionInterna() { return descripcionInterna; }
    public void setDescripcionInterna(String descripcionInterna) { this.descripcionInterna = descripcionInterna; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public int getTotalUsos() { return totalUsos; }
    public void setTotalUsos(int totalUsos) { this.totalUsos = totalUsos; }

    public Long getCreadoPorAdminId() { return creadoPorAdminId; }
    public void setCreadoPorAdminId(Long creadoPorAdminId) { this.creadoPorAdminId = creadoPorAdminId; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
