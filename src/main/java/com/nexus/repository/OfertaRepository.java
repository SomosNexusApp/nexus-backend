package com.nexus.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.entity.BadgeOferta;
import com.nexus.entity.Oferta;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Integer> {

  List<Oferta> findByEsActivaTrue();

  @Query("SELECT o FROM Oferta o WHERE " +
      "(o.categoria.nombre = :cat OR o.categoria.slug = :cat) AND o.esActiva = true")
  List<Oferta> findByCategoria(@Param("cat") String categoriaNameOrSlug);

  List<Oferta> findByTiendaContainingIgnoreCase(String tienda);

  List<Oferta> findByBadgeAndEsActivaTrue(BadgeOferta badge);

  @Query("SELECT o FROM Oferta o WHERE " +
      "LOWER(o.titulo) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
      "LOWER(o.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))")
  List<Oferta> buscarPorTexto(@Param("q") String texto);

  /**
   * Native query con CAST explícito a TEXT.
   *
   * CAUSA RAÍZ: Hibernate 6 + PostgreSQL — cuando un parámetro es NULL,
   * el driver JDBC no puede inferir su tipo SQL y PostgreSQL lo trata como
   * bytea por defecto. Cualquier función que reciba ese parámetro
   * (incluso LOWER en otro campo de la misma query) falla con
   * "no existe la función lower(bytea)".
   *
   * SOLUCIÓN: native query + CAST(:param AS TEXT) fuerza el tipo
   * en el lado de PostgreSQL, eliminando la ambigüedad del driver.
   *
   * ORDENACIÓN: el Pageable pasa el ORDER BY automáticamente.
   * OfertaService.sanitizarSort() garantiza que el campo es válido.
   *
   * COUNT QUERY: obligatoria en native + Pageable para que Spring
   * pueda calcular totalElements / totalPages.
   */
  @Query(value = """
      SELECT o.* FROM oferta o
      LEFT JOIN categoria c ON c.id = o.categoria_id
      WHERE
        (CAST(:categoria AS TEXT) IS NULL
          OR c.nombre = CAST(:categoria AS TEXT)
          OR c.slug   = CAST(:categoria AS TEXT))
        AND
        (CAST(:tienda AS TEXT) IS NULL
          OR LOWER(o.tienda) LIKE LOWER('%' || CAST(:tienda AS TEXT) || '%'))
        AND
        (CAST(:precioMin AS NUMERIC) IS NULL
          OR o.precio_oferta >= CAST(:precioMin AS NUMERIC))
        AND
        (CAST(:precioMax AS NUMERIC) IS NULL
          OR o.precio_oferta <= CAST(:precioMax AS NUMERIC))
        AND
        (CAST(:busqueda AS TEXT) IS NULL
          OR LOWER(o.titulo) LIKE LOWER('%' || CAST(:busqueda AS TEXT) || '%'))
        AND
        (:soloActivas = FALSE OR o.es_activa = TRUE)
        AND
        (CAST(:actorId AS NUMERIC) IS NULL
          OR o.actor_id = CAST(:actorId AS NUMERIC))
        AND
        (COALESCE(:excludedActorIds, NULL) IS NULL OR o.actor_id NOT IN (:excludedActorIds))
        AND
        (CAST(:minLat AS NUMERIC) IS NULL OR o.latitude >= CAST(:minLat AS NUMERIC))
        AND
        (CAST(:maxLat AS NUMERIC) IS NULL OR o.latitude <= CAST(:maxLat AS NUMERIC))
        AND
        (CAST(:minLng AS NUMERIC) IS NULL OR o.longitude >= CAST(:minLng AS NUMERIC))
        AND
        (CAST(:maxLng AS NUMERIC) IS NULL OR o.longitude <= CAST(:maxLng AS NUMERIC))
      """, countQuery = """
      SELECT COUNT(*) FROM oferta o
      LEFT JOIN categoria c ON c.id = o.categoria_id
      WHERE
        (CAST(:categoria AS TEXT) IS NULL
          OR c.nombre = CAST(:categoria AS TEXT)
          OR c.slug   = CAST(:categoria AS TEXT))
        AND
        (CAST(:tienda AS TEXT) IS NULL
          OR LOWER(o.tienda) LIKE LOWER('%' || CAST(:tienda AS TEXT) || '%'))
        AND
        (CAST(:precioMin AS NUMERIC) IS NULL
          OR o.precio_oferta >= CAST(:precioMin AS NUMERIC))
        AND
        (CAST(:precioMax AS NUMERIC) IS NULL
          OR o.precio_oferta <= CAST(:precioMax AS NUMERIC))
        AND
        (CAST(:busqueda AS TEXT) IS NULL
          OR LOWER(o.titulo) LIKE LOWER('%' || CAST(:busqueda AS TEXT) || '%'))
        AND
        (:soloActivas = FALSE OR o.es_activa = TRUE)
        AND
        (CAST(:actorId AS NUMERIC) IS NULL
          OR o.actor_id = CAST(:actorId AS NUMERIC))
        AND
        (COALESCE(:excludedActorIds, NULL) IS NULL OR o.actor_id NOT IN (:excludedActorIds))
        AND
        (CAST(:minLat AS NUMERIC) IS NULL OR o.latitude >= CAST(:minLat AS NUMERIC))
        AND
        (CAST(:maxLat AS NUMERIC) IS NULL OR o.latitude <= CAST(:maxLat AS NUMERIC))
        AND
        (CAST(:minLng AS NUMERIC) IS NULL OR o.longitude >= CAST(:minLng AS NUMERIC))
        AND
        (CAST(:maxLng AS NUMERIC) IS NULL OR o.longitude <= CAST(:maxLng AS NUMERIC))
      """, nativeQuery = true)
  Page<Oferta> buscarConFiltrosGeograficos(
      @Param("categoria") String categoria,
      @Param("tienda") String tienda,
      @Param("precioMin") Double precioMin,
      @Param("precioMax") Double precioMax,
      @Param("busqueda") String busqueda,
      @Param("soloActivas") boolean soloActivas,
      @Param("actorId") Integer actorId,
      @Param("excludedActorIds") List<Integer> excludedActorIds,
      @Param("minLat") Double minLat,
      @Param("maxLat") Double maxLat,
      @Param("minLng") Double minLng,
      @Param("maxLng") Double maxLng,
      Pageable pageable);

  @Query("SELECT o FROM Oferta o WHERE o.esActiva = true ORDER BY (o.sparkCount - o.dripCount) DESC")
  List<Oferta> findTopBySparkScore(Pageable pageable);

  @Query("SELECT o FROM Oferta o WHERE o.esActiva = true ORDER BY (o.sparkCount - o.dripCount) DESC")
  List<Oferta> findTop10ByOrderBySparkScoreDesc(Pageable pageable);

  @Query("SELECT o FROM Oferta o WHERE o.esActiva = true AND o.fechaPublicacion >= :desde " +
      "ORDER BY (o.sparkCount - o.dripCount) DESC")
  List<Oferta> findTrending(@Param("desde") LocalDateTime hace24h, Pageable pageable);

  @Query("SELECT o FROM Oferta o WHERE o.esActiva = true ORDER BY o.fechaPublicacion DESC")
  List<Oferta> findRecientes(Pageable pageable);

  @Query("SELECT o FROM Oferta o WHERE o.esActiva = true " +
      "AND o.fechaExpiracion BETWEEN :ahora AND :en24h ORDER BY o.fechaExpiracion ASC")
  List<Oferta> findProximasExpirar(@Param("ahora") LocalDateTime ahora,
      @Param("en24h") LocalDateTime en24h);

  @Query("SELECT o FROM Oferta o WHERE o.esActiva = true AND o.fechaExpiracion < :ahora")
  List<Oferta> findExpiradas(@Param("ahora") LocalDateTime ahora);

  @Query("SELECT o FROM Oferta o WHERE o.actor.id = :actorId ORDER BY o.fechaPublicacion DESC")
  List<Oferta> findByActorId(@Param("actorId") Integer actorId);

  @Query("SELECT o FROM Oferta o WHERE o.esActiva = true " +
      "AND (o.sparkCount - o.dripCount) >= 10 AND o.fechaPublicacion >= :desde " +
      "ORDER BY (o.sparkCount - o.dripCount) DESC")
  List<Oferta> findDestacadas(@Param("desde") LocalDateTime hace7dias, Pageable pageable);

  @Query("SELECT COUNT(o) FROM Oferta o WHERE o.esActiva = true")
  long countActivas();

  @Query("SELECT DISTINCT o.categoria.nombre FROM Oferta o " +
      "WHERE o.esActiva = true AND o.categoria IS NOT NULL ORDER BY o.categoria.nombre")
  List<String> findCategoriasDistintas();

  @Query("SELECT DISTINCT o.tienda FROM Oferta o " +
      "WHERE o.esActiva = true AND o.tienda IS NOT NULL ORDER BY o.tienda")
  List<String> findTiendasDistintas();
}