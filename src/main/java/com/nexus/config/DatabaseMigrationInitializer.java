package com.nexus.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Aplica migraciones SQL que Hibernate no puede ejecutar correctamente
 * (columnas NOT NULL en tablas con filas existentes), y siembra el admin por defecto.
 *
 * Cada ALTER usa IF NOT EXISTS para ser idempotente — seguro ejecutar N veces.
 */
@Component
public class DatabaseMigrationInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationInitializer.class);

    @Autowired
    private com.nexus.repository.AdminRepository adminRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void migrate() {
        addAdminColumns();
        seedAdminUser();
    }

    // ── 1. Columnas admin en tabla usuario ────────────────────────────────────

    private void addAdminColumns() {
        log.info("🔧 Verificando columnas admin en tabla 'usuario'...");
        run("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS baneado          boolean   NOT NULL DEFAULT false");
        run("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS motivo_ban       TEXT");
        run("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS suspendido_hasta TIMESTAMP");
        run("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS motivo_suspension TEXT");
        run("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS flag_fraude      boolean   NOT NULL DEFAULT false");
        run("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS motivo_flag      TEXT");
        log.info("✅ Columnas admin OK.");
    }

    // ── 2. Admin por defecto ──────────────────────────────────────────────────

    private void seedAdminUser() {
        try {
            if (adminRepository.findByEmail("admin@nexus.app").isPresent()) {
                log.info("ℹ️  Ya existe el admin 'admin@nexus.app' — se omite la siembra.");
                return;
            }

            com.nexus.entity.Admin admin = new com.nexus.entity.Admin();
            admin.setUser("nexusadmin");
            admin.setEmail("admin@nexus.app");
            admin.setPassword(new BCryptPasswordEncoder().encode("Admin1234!"));
            admin.setNombre("Nexus");
            admin.setApellidos("Admin");
            admin.setCuentaVerificada(true);
            admin.setNivelAcceso(1);

            adminRepository.save(admin);
            log.info("✅ Admin por defecto creado (vía JPA): email=admin@nexus.app  password=Admin1234!");

        } catch (Exception e) {
            log.warn("⚠️  No se pudo crear el admin por defecto: {}", e.getMessage());
        }
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private void run(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception e) {
            log.warn("  ⚠ {}: {}", sql.substring(0, Math.min(60, sql.length())), e.getMessage());
        }
    }
}
