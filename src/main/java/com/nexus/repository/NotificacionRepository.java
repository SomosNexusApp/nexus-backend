package com.nexus.repository;
import com.nexus.entity.NotificacionInApp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
/**
 * JpaRepository<NotificacionInApp, Integer>  <- tipo correcto
 * Tiene: findByActorIdOrderByFechaDesc (list + paginado),
 *        findByActorIdAndLeidaFalseOrderByFechaDesc,
 *        countByActorIdAndLeidaFalse
 */
@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionInApp, Integer> {
    List<NotificacionInApp> findByActorIdOrderByFechaDesc(Integer actorId);
    List<NotificacionInApp> findByActorIdAndLeidaFalseOrderByFechaDesc(Integer actorId);
    long countByActorIdAndLeidaFalse(Integer actorId);
    Page<NotificacionInApp> findByActorIdOrderByFechaDesc(Integer actorId, Pageable pageable);

    List<NotificacionInApp> findByActorIdAndDestacadaTrueAndLeidaFalseOrderByFechaDesc(Integer actorId);
}
