package com.nexus.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Configuración de caché en memoria optimizada para entornos con poca RAM (500 MB).
 *
 * Estrategia:
 *  - SimpleCacheManager con ConcurrentMapCache: sin dependencias extra (no Caffeine, no Redis).
 *  - Caché de categorías: TTL largo (10 min) — cambian muy poco.
 *  - Los tamaños son pequeños para no agotar el heap en Render Free.
 *
 * Para invalidar manualmente en desarrollo: GET /actuator/caches/{name}/clear (si actuator está activo)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Nombres de caché usados en la aplicación.
     * Añade aquí cualquier nuevo caché que crees en los Services.
     */
    public static final String CACHE_CATEGORIAS_RAIZ  = "categorias-raiz";
    public static final String CACHE_CATEGORIAS_TODAS = "categorias-todas";
    public static final String CACHE_CATEGORIAS_HIJAS = "categorias-hijas";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();

        manager.setCaches(Arrays.asList(
            // Categorías raíz — navbar principal. TTL ~10 min, máx 1 entrada (lista completa)
            new TtlConcurrentMapCache(CACHE_CATEGORIAS_RAIZ,  TimeUnit.MINUTES.toMillis(10), 1),
            // Todas las categorías activas — selects/dropdowns. TTL ~10 min, máx 1 entrada
            new TtlConcurrentMapCache(CACHE_CATEGORIAS_TODAS, TimeUnit.MINUTES.toMillis(10), 1),
            // Hijas por padre — una entrada por parent_id. TTL ~10 min, máx 30 entradas
            new TtlConcurrentMapCache(CACHE_CATEGORIAS_HIJAS, TimeUnit.MINUTES.toMillis(10), 30)
        ));

        return manager;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ConcurrentMapCache con TTL y tamaño máximo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extensión de ConcurrentMapCache que añade:
     *  - TTL por entrada (milisegundos desde la escritura).
     *  - Tamaño máximo para evitar crecimiento ilimitado en heap pequeño.
     *
     * Es una implementación simple y sin dependencias externas.
     * Si en el futuro se agrega Caffeine al pom.xml se puede sustituir fácilmente.
     */
    public static class TtlConcurrentMapCache extends ConcurrentMapCache {

        private final long ttlMs;
        private final int  maxSize;
        private final ConcurrentHashMap<Object, Long> timestamps = new ConcurrentHashMap<>();

        TtlConcurrentMapCache(String name, long ttlMs, int maxSize) {
            super(name, false);
            this.ttlMs   = ttlMs;
            this.maxSize = maxSize;
        }

        @Override
        public ValueWrapper get(Object key) {
            // Comprobar TTL antes de devolver el valor
            Long insertedAt = timestamps.get(key);
            if (insertedAt != null && System.currentTimeMillis() - insertedAt > ttlMs) {
                evict(key);
                return null;
            }
            return super.get(key);
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            Long insertedAt = timestamps.get(key);
            if (insertedAt != null && System.currentTimeMillis() - insertedAt > ttlMs) {
                evict(key);
                return null;
            }
            return super.get(key, type);
        }

        @Override
        public void put(Object key, Object value) {
            // Si se alcanza el tamaño máximo, limpiar entradas expiradas primero
            if (getNativeCache().size() >= maxSize) {
                evictExpired();
                // Si sigue lleno tras limpiar expiradas, vaciar todo (política LRU simple)
                if (getNativeCache().size() >= maxSize) {
                    clear();
                }
            }
            super.put(key, value);
            timestamps.put(key, System.currentTimeMillis());
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            // Respetar TTL también en putIfAbsent
            Long insertedAt = timestamps.get(key);
            if (insertedAt != null && System.currentTimeMillis() - insertedAt > ttlMs) {
                evict(key);
            }
            ValueWrapper existing = super.putIfAbsent(key, value);
            if (existing == null) {
                timestamps.put(key, System.currentTimeMillis());
            }
            return existing;
        }

        @Override
        public void evict(Object key) {
            super.evict(key);
            timestamps.remove(key);
        }

        @Override
        public void clear() {
            super.clear();
            timestamps.clear();
        }

        /** Elimina todas las entradas cuyo TTL haya expirado. */
        private void evictExpired() {
            long now = System.currentTimeMillis();
            timestamps.forEach((key, ts) -> {
                if (now - ts > ttlMs) {
                    super.evict(key);
                    timestamps.remove(key);
                }
            });
        }
    }
}
