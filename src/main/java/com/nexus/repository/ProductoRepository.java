package com.nexus.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;

@Repository
public interface ProductoRepository
        extends JpaRepository<Producto, Integer>,
                JpaSpecificationExecutor<Producto> {

    Optional<Producto> findByTitulo(String titulo);

    List<Producto> findByVendedorIdOrderByFechaPublicacionDesc(Integer id);

    List<Producto> findByEstado(EstadoProducto estado);

    List<Producto> findByEstadoOrderByPatrocinadoDescFechaPublicacionDesc(EstadoProducto estado);

    List<Producto> findByEstadoIn(Collection<EstadoProducto> estados);
    long countByEstado(EstadoProducto estado);

    /**
     * Conservamos este método para retrocompatibilidad con código que lo llame directamente.
     * La búsqueda principal ahora pasa por ProductoSpecification.
     */
    @Query(
        value = """
            SELECT p FROM Producto p
            JOIN FETCH p.vendedor v
            LEFT JOIN FETCH p.categoria c
            WHERE p.estado = com.nexus.entity.EstadoProducto.DISPONIBLE
              AND (:busqueda  IS NULL OR LOWER(p.titulo)      LIKE LOWER(CONCAT('%', CAST(:busqueda  AS string), '%'))
                                     OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', CAST(:busqueda  AS string), '%')))
              AND (:categoria IS NULL OR c.nombre = :categoria OR c.slug = :categoria)
              AND (:precioMin IS NULL OR p.precio >= :precioMin)
              AND (:precioMax IS NULL OR p.precio <= :precioMax)
            """,
        countQuery = """
            SELECT COUNT(p) FROM Producto p
            LEFT JOIN p.categoria c
            WHERE p.estado = com.nexus.entity.EstadoProducto.DISPONIBLE
              AND (:busqueda  IS NULL OR LOWER(p.titulo)      LIKE LOWER(CONCAT('%', CAST(:busqueda  AS string), '%'))
                                     OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', CAST(:busqueda  AS string), '%')))
              AND (:categoria IS NULL OR c.nombre = :categoria OR c.slug = :categoria)
              AND (:precioMin IS NULL OR p.precio >= :precioMin)
              AND (:precioMax IS NULL OR p.precio <= :precioMax)
            """
    )
    Page<Producto> buscarConFiltros(
            @Param("categoria") String categoria,
            @Param("precioMin") Double precioMin,
            @Param("precioMax") Double precioMax,
            @Param("busqueda")  String busqueda,
            Pageable pageable);

    @Query("SELECT p FROM Producto p JOIN FETCH p.vendedor v LEFT JOIN FETCH p.categoria c")
    List<Producto> findAllWithDetails();

    @Query("SELECT DISTINCT p.categoria.nombre FROM Producto p " +
           "WHERE p.estado = com.nexus.entity.EstadoProducto.DISPONIBLE " +
           "AND p.categoria IS NOT NULL ORDER BY p.categoria.nombre")
    List<String> findCategoriasDistintas();

    long countByCategoriaId(Integer categoriaId);

    @Query("SELECT p FROM Producto p WHERE p.precio > 5000 OR p.vendedor.flagFraude = true " +
           "OR (SELECT COUNT(r) FROM Reporte r WHERE r.actorDenunciado = p.vendedor) > 2")
    List<Producto> findSospechosos();
}
