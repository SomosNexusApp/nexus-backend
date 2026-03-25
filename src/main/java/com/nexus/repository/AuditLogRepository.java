package com.nexus.repository;

import com.nexus.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:admin IS NULL OR LOWER(a.adminUser) LIKE LOWER(CONCAT('%', :admin, '%')))
          AND (:accion IS NULL OR a.accion = :accion)
          AND (:entidadTipo IS NULL OR a.entidadTipo = :entidadTipo)
          AND (:desde IS NULL OR a.timestamp >= :desde)
          AND (:hasta IS NULL OR a.timestamp <= :hasta)
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> filter(
        @Param("admin") String admin,
        @Param("accion") String accion,
        @Param("entidadTipo") String entidadTipo,
        @Param("desde") java.time.LocalDateTime desde,
        @Param("hasta") java.time.LocalDateTime hasta,
        Pageable pageable
    );
}
