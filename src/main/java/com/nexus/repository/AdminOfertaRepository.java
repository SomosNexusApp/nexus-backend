package com.nexus.repository;

import com.nexus.entity.EstadoOferta;
import com.nexus.entity.Oferta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminOfertaRepository extends JpaRepository<Oferta, Integer> {

    Page<Oferta> findByEstado(EstadoOferta estado, Pageable pageable);

    Page<Oferta> findAll(Pageable pageable);

    long countByDestacadaTrue();

    @Query("""
        SELECT o FROM Oferta o
        JOIN FETCH o.actor a
        LEFT JOIN FETCH o.categoria c
        WHERE (:estado IS NULL OR o.estado = :estado)
        """)
    Page<Oferta> buscarAdmin(@Param("estado") EstadoOferta estado, Pageable pageable);

    @Query("SELECT o FROM Oferta o WHERE o.esFlash = true ORDER BY o.fechaPublicacion DESC")
    List<Oferta> findOfertas();
}
