package com.nexus.entity;
/**
 * Estados reales usados en EnvioService y DevolucionService:
 *   PENDIENTE_ENVIO  lines 26(Envio.java), 36, 66
 *   ENVIADO          lines 70, 100
 *   EN_TRANSITO      line 100
 *   ENTREGADO        lines 37(Devolucion), 113, 137
 */
public enum EstadoEnvio {
    PENDIENTE_ENVIO,
    PREPARANDO,
    ENVIADO,
    EN_TRANSITO,
    EN_REPARTO,
    ENTREGADO,
    INCIDENCIA,
    DEVUELTO,
    DEVOLUCION_SOLICITADA,
    CANCELADO
}