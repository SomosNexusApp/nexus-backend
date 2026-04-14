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
        fixNotificacionEnum();
        fixSoporteSchema();
        seedAdminUser();
        seedCategories();
    }

    private void fixSoporteSchema() {
        log.info("🔧 Verificando esquema de soporte...");
        // Añadir columna status con default para evitar error de NOT NULL con filas existentes
        run("ALTER TABLE soporte_chat_session ADD COLUMN IF NOT EXISTS status VARCHAR(255) NOT NULL DEFAULT 'OPEN'");
        log.info("✅ Esquema de soporte OK.");
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

    private void fixNotificacionEnum() {
        log.info("🔧 Verificando restricciones de enum en 'notificacion_in_app'...");
        run("ALTER TABLE notificacion_in_app DROP CONSTRAINT IF EXISTS notificacion_in_app_tipo_check");
        log.info("✅ Restricción de enum eliminada (para permitir nuevos tipos).");
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

    private void seedCategories() {
        log.info("🔧 Verificando categorías core...");

        // ── Categorías raíz ─────────────────────────────────────────────────
        seedCat(null, "Electrónica",   "electronica",   "cpu",        "#1565C0", 1);
        seedCat(null, "Moda",          "moda",          "shirt",      "#6A1B9A", 2);
        seedCat(null, "Hogar",         "hogar",         "home",       "#2E7D32", 3);
        seedCat(null, "Vehículos",     "vehiculos",     "car",        "#1976D2", 4);
        seedCat(null, "Informática",   "informatica",   "laptop",     "#00838F", 5);
        seedCat(null, "Videojuegos",   "videojuegos",   "gamepad",    "#7B1FA2", 6);
        seedCat(null, "Deportes",      "deportes",      "bicycle",    "#E65100", 7);
        seedCat(null, "Libros",        "libros",        "book",       "#4E342E", 8);
        seedCat(null, "Juguetes",      "juguetes",      "toy-brick",  "#F57F17", 9);
        seedCat(null, "Inmuebles",     "inmuebles",     "building",   "#37474F", 10);
        seedCat(null, "Viajes",        "viajes",        "plane",      "#F44336", 12);
        seedCat(null, "Otros",         "otros",         "archive",    "#78909C", 13);

        // ── Subcategorías Electrónica ────────────────────────────────────────
        seedCatHija("electronica", "Móviles",        "moviles",           "smartphone",   "#1565C0", 1);
        seedCatHija("electronica", "Audio",          "audio",             "headphones",   "#1565C0", 2);
        seedCatHija("electronica", "TV y Vídeo",     "tv-video",          "tv",           "#1565C0", 3);
        seedCatHija("electronica", "Cámaras",        "camaras",           "camera",       "#1565C0", 4);

        // ── Subcategorías Informática ────────────────────────────────────────
        seedCatHija("informatica", "PCs y Portátiles", "pcs",             "laptop",       "#00838F", 1);
        seedCatHija("informatica", "Software",        "software",         "code",         "#00838F", 2);
        seedCatHija("informatica", "Componentes",     "componentes-pc",   "memory",       "#00838F", 3);

        // ── Subcategorías Vehículos ──────────────────────────────────────────
        seedCatHija("vehiculos",   "Coches",          "coches",           "car",          "#1976D2", 1);
        seedCatHija("vehiculos",   "Motos",           "motos",            "bike",         "#1976D2", 2);

        // ── Subcategorías Moda ───────────────────────────────────────────────
        seedCatHija("moda",        "Moda Hombre",     "moda-hombre",      "user",         "#6A1B9A", 1);
        seedCatHija("moda",        "Moda Mujer",      "moda-mujer",       "user",         "#6A1B9A", 2);
        seedCatHija("moda",        "Zapatillas",      "zapatillas",       "footsteps",    "#6A1B9A", 3);

        // ── Subcategorías Videojuegos ────────────────────────────────────────
        seedCatHija("videojuegos", "Consolas",        "consolas",         "gamepad",      "#7B1FA2", 1);

        // ── Subcategorías Hogar ──────────────────────────────────────────────
        seedCatHija("hogar",       "Muebles",         "muebles",          "chair",        "#2E7D32", 1);
        seedCatHija("hogar",       "Electrodomésticos","electrodomesticos","kitchen",     "#2E7D32", 2);

        // ── Subcategorías Viajes ─────────────────────────────────────────────
        seedCatHija("viajes",      "Vuelos",          "vuelos",           "plane-takeoff","#F44336", 1);
        seedCatHija("viajes",      "Hoteles",         "hoteles",          "bed",          "#F44336", 2);

        log.info("✅ Categorías core verificadas/insertadas.");
    }

    /**
     * Inserta una categoría raíz si no existe todavía (idempotente por slug).
     */
    private void seedCat(String parentSlug, String nombre, String slug,
                         String icono, String color, int orden) {
        if (parentSlug == null) {
            run("INSERT INTO categoria (nombre, slug, icono, color, orden, activa) " +
                "SELECT '" + nombre + "', '" + slug + "', '" + icono + "', '" + color + "', " + orden + ", true " +
                "WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE slug = '" + slug + "')");
        }
    }

    /**
     * Inserta una subcategoría hija (referenciando parent por slug) si no existe.
     * Usa una subquery para obtener el parent_id dinámicamente.
     */
    private void seedCatHija(String parentSlug, String nombre, String slug,
                              String icono, String color, int orden) {
        run("INSERT INTO categoria (nombre, slug, icono, color, orden, activa, parent_id) " +
            "SELECT '" + nombre + "', '" + slug + "', '" + icono + "', '" + color + "', " + orden + ", true, " +
            "(SELECT id FROM categoria WHERE slug = '" + parentSlug + "' LIMIT 1) " +
            "WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE slug = '" + slug + "') " +
            "AND EXISTS (SELECT 1 FROM categoria WHERE slug = '" + parentSlug + "')");
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
