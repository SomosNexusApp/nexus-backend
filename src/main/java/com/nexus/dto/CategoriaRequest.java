package com.nexus.dto;

public record CategoriaRequest(
        String nombre,
        String slug,
        String icono,
        String color,
        Boolean activa,
        Integer padreId
) {}
