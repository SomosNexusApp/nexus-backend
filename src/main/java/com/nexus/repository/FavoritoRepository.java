package com.nexus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Favorito;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Integer> {
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"producto", "oferta", "vehiculo"})
    @Query("SELECT f FROM Favorito f WHERE f.usuario.id = ?1 ORDER BY f.fechaGuardado DESC")
    List<Favorito> findByUsuarioId(Integer usuarioId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"producto", "oferta", "vehiculo"})
    @Query("SELECT f FROM Favorito f WHERE f.usuario.id = ?1 AND f.oferta.id = ?2")
    Optional<Favorito> findByUsuarioAndOferta(Integer usuarioId, Integer ofertaId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"producto", "oferta", "vehiculo"})
    @Query("SELECT f FROM Favorito f WHERE f.usuario.id = ?1 AND f.producto.id = ?2")
    Optional<Favorito> findByUsuarioAndProducto(Integer usuarioId, Integer productoId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"producto", "oferta", "vehiculo"})
    @Query("SELECT f FROM Favorito f WHERE f.usuario.id = ?1 AND f.vehiculo.id = ?2")
    Optional<Favorito> findByUsuarioAndVehiculo(Integer usuarioId, Integer vehiculoId);
}