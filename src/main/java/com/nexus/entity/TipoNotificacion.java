package com.nexus.entity;
public enum TipoNotificacion {
    NUEVO_MENSAJE,
    NUEVA_COMPRA,
    COMPRA_CONFIRMADA,
    COMPRA_PAGADA_VENDEDOR,
    COMPRA_PAGADA_COMPRADOR,
    ENVIO_ACTUALIZADO,
    NUEVA_VALORACION,
    SPARK_EN_OFERTA,
    NUEVO_COMENTARIO,
    DEVOLUCION,
    DEVOLUCION_ACTUALIZACION,
    OFERTA_CHAT,
    ACCION_ADMIN,
    CADUCIDAD_ANUNCIO,
    ENVIO_PLAZO,
    REEMBOLSO_AUTOMATICO,
    FAVORITO_PRODUCTO,
    FAVORITO_OFERTA,
    GUIA_ENVIO_VENDEDOR,
    /** Propuesta de contrato publicitario del equipo Nexus */
    CONTRATO_PROPUESTA,
    /** Solicitud de patrocinio enviada por un usuario/empresa al admin */
    SOLICITUD_PATROCINIO,
    /** Admin aprueba o cancela un patrocinio */
    PATROCINIO_APROBADO,
    PATROCINIO_CANCELADO,
    SISTEMA
}
