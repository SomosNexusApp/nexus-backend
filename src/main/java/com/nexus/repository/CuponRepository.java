package com.nexus.repository;

import com.nexus.entity.Cupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuponRepository extends JpaRepository<Cupon, Integer> {

    Optional<Cupon> findByCodigo(String codigo);
    Optional<Cupon> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigo(String codigo);

    @Query("SELECT c FROM Cupon c WHERE " +
           "(:activo IS NULL OR c.activo = :activo) AND " +
           "(:caducado IS NULL OR " +
           "  (:caducado = true AND c.fechaFin IS NOT NULL AND c.fechaFin < CURRENT_TIMESTAMP) OR " +
           "  (:caducado = false AND (c.fechaFin IS NULL OR c.fechaFin >= CURRENT_TIMESTAMP)))")
    Page<Cupon> findAdmin(Boolean activo, Boolean caducado, Pageable pageable);

    long countByActivoTrue();
}
