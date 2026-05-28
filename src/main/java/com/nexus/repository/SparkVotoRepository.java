package com.nexus.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.nexus.entity.SparkVoto;

@Repository
public interface SparkVotoRepository extends JpaRepository<SparkVoto, Integer> {

    Optional<SparkVoto> findByActorIdAndOfertaId(Integer actorId, Integer ofertaId);
    Optional<SparkVoto> findByActorIdAndProductoId(Integer actorId, Integer productoId);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM SparkVoto v LEFT JOIN FETCH v.oferta LEFT JOIN FETCH v.producto WHERE v.actor.id = :actorId ORDER BY v.fechaVoto DESC")
    java.util.List<SparkVoto> findByActorId(@org.springframework.data.repository.query.Param("actorId") Integer actorId);

    @Query("SELECT COALESCE(SUM(v.valor), 0) FROM SparkVoto v WHERE v.oferta.id = ?1")
    int sumarVotosPorOferta(Integer ofertaId);

    @Query("SELECT COALESCE(SUM(v.valor), 0) FROM SparkVoto v WHERE v.producto.id = ?1")
    int sumarVotosPorProducto(Integer productoId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SparkVoto v WHERE v.actor.id = ?1 AND v.oferta.id = ?2")
    void deleteByActorAndOferta(Integer actorId, Integer ofertaId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SparkVoto v WHERE v.actor.id = ?1 AND v.producto.id = ?2")
    void deleteByActorAndProducto(Integer actorId, Integer productoId);
}