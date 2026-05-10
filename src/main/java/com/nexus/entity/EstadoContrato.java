package com.nexus.entity;

public enum EstadoContrato {
    DRAFT,
    /** Propuesta del admin pendiente de respuesta de la empresa */
    PROPUESTA_ADMIN,
    /** Solicitud de patrocinio enviada por el usuario, pendiente de revisión del admin */
    SOLICITUD_USUARIO,
    /** Admin aprobó la solicitud del usuario; pendiente de pago Stripe */
    APROBADO_PENDIENTE_PAGO,
    /** Empresa aceptó; esperando pago Stripe (flujo empresa-admin) */
    PENDIENTE_PAGO,
    /** Contrato pagado y vigente (banner / publicación patrocinada) */
    ACTIVE,
    RECHAZADO,
    EXPIRED,
    CANCELLED
}
