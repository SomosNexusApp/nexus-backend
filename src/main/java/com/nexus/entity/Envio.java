package com.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Gestiona el envío físico de un producto tras confirmar el pago.
 *
 * Flujo completo:
 * PAGADO → vendedor crea envío → PENDIENTE_ENVIO
 * Vendedor añade tracking → ENVIADO
 * Comprador confirma recepción → ENTREGADO → fondos liberados en Stripe
 *
 * Si el comprador no confirma en 7 días, se confirma automáticamente (tarea
 * programada).
 */
@Entity
@Table(name = "envio")
public class Envio extends DomainEntity {

    @OneToOne
    @JoinColumn(name = "compra_id", nullable = false, unique = true)
    private Compra compra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE_ENVIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoEntrega metodoEntrega;

    // Dirección de entrega (solo para ENVIO_PAQUETERIA)
    private String nombreDestinatario;
    private String direccion;
    private String ciudad;
    private String codigoPostal;
    private String pais;
    private String telefono;

    // Seguimiento del paquete
    @Enumerated(EnumType.STRING)
    @Column(name = "transportista_enum")
    private Transportista transportistaEnum; // CORREOS, SEUR, MRW

    private String transportista; // Nombre libre (legado / integraciones futuras)
    private String numeroSeguimiento;
    private String urlSeguimiento; // URL de seguimiento del transportista

    // Código de envío único (SHIP-XXXXXXXX)
    @Column(unique = true)
    private String codigoEnvio;

    // QR del código de envío (PNG base64)
    @Column(columnDefinition = "TEXT")
    private String qrBase64;

    // Peso del paquete en kg
    private Double pesoKg;

    // Precio del envío
    private Double precioEnvio = 0.0;

    // Timestamps
    private LocalDateTime fechaCreacion;
    /** Tras este instante, si sigue PENDIENTE_ENVIO, se puede reembolsar al comprador (scheduler). */
    private LocalDateTime fechaLimiteEnvio;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaEstimadaEntrega;
    private LocalDateTime fechaConfirmacionEntrega;

    // Identificador del PaymentIntent en Stripe para el release de fondos
    private String stripePaymentIntentId;

    // Valoración del vendedor después de la entrega (1-5 estrellas)
    private Integer valoracionVendedor;
    private String comentarioValoracion;

    public Envio() {
        super();
        this.fechaCreacion = LocalDateTime.now();
    }

    // ── Getters y Setters ──────────────────────────────────────────────────

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public EstadoEnvio getEstado() {
        return estado;
    }

    public void setEstado(EstadoEnvio estado) {
        this.estado = estado;
    }

    public MetodoEntrega getMetodoEntrega() {
        return metodoEntrega;
    }

    public void setMetodoEntrega(MetodoEntrega metodoEntrega) {
        this.metodoEntrega = metodoEntrega;
    }

    public String getNombreDestinatario() {
        return nombreDestinatario;
    }

    public void setNombreDestinatario(String nombreDestinatario) {
        this.nombreDestinatario = nombreDestinatario;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public String getNumeroSeguimiento() {
        return numeroSeguimiento;
    }

    public void setNumeroSeguimiento(String numeroSeguimiento) {
        this.numeroSeguimiento = numeroSeguimiento;
    }

    public String getUrlSeguimiento() {
        return urlSeguimiento;
    }

    public void setUrlSeguimiento(String urlSeguimiento) {
        this.urlSeguimiento = urlSeguimiento;
    }

    public Double getPrecioEnvio() {
        return precioEnvio;
    }

    public void setPrecioEnvio(Double precioEnvio) {
        this.precioEnvio = precioEnvio;
    }

    public Transportista getTransportistaEnum() {
        return transportistaEnum;
    }

    public void setTransportistaEnum(Transportista t) {
        this.transportistaEnum = t;
    }

    public String getCodigoEnvio() {
        return codigoEnvio;
    }

    public void setCodigoEnvio(String codigoEnvio) {
        this.codigoEnvio = codigoEnvio;
    }

    public String getQrBase64() {
        return qrBase64;
    }

    public void setQrBase64(String qrBase64) {
        this.qrBase64 = qrBase64;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaLimiteEnvio() {
        return fechaLimiteEnvio;
    }

    public void setFechaLimiteEnvio(LocalDateTime fechaLimiteEnvio) {
        this.fechaLimiteEnvio = fechaLimiteEnvio;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDateTime getFechaEstimadaEntrega() {
        return fechaEstimadaEntrega;
    }

    public void setFechaEstimadaEntrega(LocalDateTime fechaEstimadaEntrega) {
        this.fechaEstimadaEntrega = fechaEstimadaEntrega;
    }

    public LocalDateTime getFechaConfirmacionEntrega() {
        return fechaConfirmacionEntrega;
    }

    public void setFechaConfirmacionEntrega(LocalDateTime f) {
        this.fechaConfirmacionEntrega = f;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String id) {
        this.stripePaymentIntentId = id;
    }

    public Integer getValoracionVendedor() {
        return valoracionVendedor;
    }

    public void setValoracionVendedor(Integer valoracionVendedor) {
        this.valoracionVendedor = valoracionVendedor;
    }

    public String getComentarioValoracion() {
        return comentarioValoracion;
    }

    public void setComentarioValoracion(String comentarioValoracion) {
        this.comentarioValoracion = comentarioValoracion;
    }
}