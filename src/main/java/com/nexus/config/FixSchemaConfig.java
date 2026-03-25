package com.nexus.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Utilidad temporal para corregir la base de datos tras la migración de Usuario a Actor.
 * Elimina la restricción de llave foránea que apunta erróneamente a la tabla 'usuario'
 * en lugar de a la tabla 'actor'.
 */
@Component
public class FixSchemaConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fix() {
        try {
            // fksi8qts5lvy9tlnx6h6yvi8s73 es el nombre reportado por el error de Postgres
            jdbcTemplate.execute("ALTER TABLE compra DROP CONSTRAINT IF EXISTS fksi8qts5lvy9tlnx6h6yvi8s73");
            System.out.println("✅ NEXUS DB FIX: Restricción de llave foránea obsoleta (compra -> usuario) eliminada exitosamente.");
        } catch (Exception e) {
            // Ignoramos si falla (podría no existir o haberse borrado manualmente)
            System.out.println("ℹ️ NEXUS DB INFO: Intento de limpieza de FK finalizado: " + e.getMessage());
        }
    }
}
