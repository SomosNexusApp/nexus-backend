package com.nexus.repository;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminProductoRepository extends JpaRepository<Producto, Integer> {

    @Query(value = """
        SELECT p FROM Producto p
        JOIN FETCH p.vendedor v
        LEFT JOIN FETCH p.categoria c
        WHERE (:q IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:categoriaId IS NULL OR c.id = :categoriaId)
          AND (:estado IS NULL OR p.estado = :estado)
          AND (:vendedorId IS NULL OR v.id = :vendedorId)
          AND (:precioMin IS NULL OR p.precio >= :precioMin)
          AND (:precioMax IS NULL OR p.precio <= :precioMax)
          AND (:fechaDesde IS NULL OR p.fechaPublicacion >= :fechaDesde)
        """, countQuery = """
        SELECT COUNT(p) FROM Producto p
        LEFT JOIN p.categoria c
        WHERE (:q IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:categoriaId IS NULL OR c.id = :categoriaId)
          AND (:estado IS NULL OR p.estado = :estado)
          AND (:vendedorId IS NULL OR p.vendedor.id = :vendedorId)
          AND (:precioMin IS NULL OR p.precio >= :precioMin)
          AND (:precioMax IS NULL OR p.precio <= :precioMax)
          AND (:fechaDesde IS NULL OR p.fechaPublicacion >= :fechaDesde)
        """)
    Page<Producto> buscarAdmin(
            @Param("q") String q,
            @Param("categoriaId") Integer categoriaId,
            @Param("estado") EstadoProducto estado,
            @Param("vendedorId") Integer vendedorId,
            @Param("precioMin") Double precioMin,
            @Param("precioMax") Double precioMax,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.productoDenunciado.id = :productoId")
    long countReportesByProductoId(@Param("productoId") Integer productoId);

    List<Producto> findByPausadoHastaBeforeAndEstado(LocalDateTime fecha, EstadoProducto estado);
}
