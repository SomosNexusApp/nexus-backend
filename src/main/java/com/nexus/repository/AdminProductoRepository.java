package com.nexus.repository;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminProductoRepository extends JpaRepository<Producto, Integer>, JpaSpecificationExecutor<Producto> {

    // Búsqueda reemplazada por Specifications en AdminProductosService para evitar errores de tipo en PostgreSQL

    @Query("SELECT COUNT(r) FROM Reporte r WHERE r.productoDenunciado.id = :productoId")
    long countReportesByProductoId(@Param("productoId") Integer productoId);

    List<Producto> findByPausadoHastaBeforeAndEstado(LocalDateTime fecha, EstadoProducto estado);
}
