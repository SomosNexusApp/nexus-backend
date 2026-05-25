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
        WHERE (:admin = '' OR LOWER(a.adminUser) LIKE LOWER(CONCAT('%', :admin, '%')))
          AND (:accion = '' OR a.accion = :accion)
          AND (:entidadTipo = '' OR a.entidadTipo = :entidadTipo)
          AND (a.timestamp >= :desde)
          AND (a.timestamp <= :hasta)
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
