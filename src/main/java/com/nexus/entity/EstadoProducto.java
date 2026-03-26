package com.nexus.entity;

public enum EstadoProducto {
    DISPONIBLE,        // En venta
    RESERVADO,         // Compra en proceso (pago confirmado, esperando envío)
    VENDIDO,           // Compra completada
    PAUSADO,           // Vendedor lo ocultó temporalmente
    ELIMINADO,         // Soft delete
    SUSPENDIDO_ADMIN   // Suspendido por el administrador
}