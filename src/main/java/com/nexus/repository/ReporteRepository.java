package com.nexus.repository;

import com.nexus.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Metodos requeridos por ReporteService:
 *   findByEstado(EstadoReporte)  line 33
 *   findByTipo(TipoReporte)      line 34
 */
@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    List<Reporte> findByEstado(EstadoReporte estado);

    List<Reporte> findByTipo(TipoReporte tipo);

    List<Reporte> findByReportadorId(Integer reportadorId);

    List<Reporte> findByEstadoOrderByFechaDesc(EstadoReporte estado);

    // ── Admin panel ───────────────────────────────────────────────────────────
    Page<Reporte> findByEstado(Reporte.EstadoReporte estado, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Reporte r WHERE CAST(r.estado AS string) = :estado")
    long countByEstado(@Param("estado") String estado);
}