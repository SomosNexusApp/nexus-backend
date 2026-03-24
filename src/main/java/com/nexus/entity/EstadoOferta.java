package com.nexus.entity;

public enum EstadoOferta {
    ACTIVA,     // Visible y vigente
    PAUSADA,    // Ocultada temporalmente por el usuario
    AGOTADA,    // Se terminó el stock o la oferta ya no es válida
    ELIMINADA   // Borrado lógico
}
