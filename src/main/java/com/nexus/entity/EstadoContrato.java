package com.nexus.entity;

public enum EstadoContrato {
    DRAFT,
    /** Propuesta del admin pendiente de respuesta de la empresa */
    PROPUESTA_ADMIN,
    /** Empresa aceptó; esperando pago Stripe */
    PENDIENTE_PAGO,
    /** Contrato pagado y vigente (banner / publicación patrocinada) */
    ACTIVE,
    RECHAZADO,
    EXPIRED,
    CANCELLED
}
