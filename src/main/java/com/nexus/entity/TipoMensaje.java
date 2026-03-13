package com.nexus.entity;

public enum TipoMensaje {
    TEXTO, // Mensaje de texto normal
    IMAGEN, // Foto (URL de Cloudinary)
    VIDEO, // Vídeo (URL de Cloudinary)
    AUDIO, // ← NUEVO: Mensaje de voz (URL de Cloudinary, formato .webm/.ogg)
    SISTEMA, // Automático: "Pago confirmado", "Pedido enviado", etc.
    OFERTA_PRECIO, // Propuesta de precio → tarjeta interactiva en Angular
    GIF // GIFs alojados en una URL externa
}