package com.nexus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Favorito;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Integer> {
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"producto", "oferta", "vehiculo"})
    @Query("SELECT f FROM Favorito f WHERE f.actor.id = :actorId ORDER BY f.fechaGuardado DESC")
    List<Favorito> findByActorId(@Param("actorId") Integer actorId);
    
    @Query("SELECT f FROM Favorito f WHERE f.actor.id = :actorId AND f.oferta.id = :ofertaId")
    Optional<Favorito> findByActorAndOferta(@Param("actorId") Integer actorId, @Param("ofertaId") Integer ofertaId);
    
    @Query("SELECT f FROM Favorito f WHERE f.actor.id = :actorId AND f.producto.id = :productoId")
    Optional<Favorito> findByActorAndProducto(@Param("actorId") Integer actorId, @Param("productoId") Integer productoId);

    @Query("SELECT f FROM Favorito f WHERE f.actor.id = :actorId AND f.vehiculo.id = :vehiculoId")
    Optional<Favorito> findByActorAndVehiculo(@Param("actorId") Integer actorId, @Param("vehiculoId") Integer vehiculoId);
}