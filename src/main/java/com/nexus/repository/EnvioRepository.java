package com.nexus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Envio;
import com.nexus.entity.EstadoEnvio;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Integer> {

    Optional<Envio> findByCompraId(Integer compraId);

    List<Envio> findByEstado(EstadoEnvio estado);

    // Envíos pendientes de confirmación por el comprador (para la tarea automática de 7 días)
    @Query("SELECT e FROM Envio e WHERE e.estado = 'ENVIADO' AND " +
           "e.fechaEnvio < CURRENT_TIMESTAMP - 7 DAY")
    List<Envio> findPendientesAutoConfirmacion();

    // Todos los envíos donde el comprador es el usuario
    @Query("SELECT e FROM Envio e WHERE e.compra.comprador.id = ?1 ORDER BY e.fechaCreacion DESC")
    List<Envio> findByCompradorId(Integer compradorId);

    // Todos los envíos donde el vendedor es el usuario
    @Query("SELECT e FROM Envio e WHERE e.compra.producto.vendedor.id = :vendedorId ORDER BY e.fechaCreacion DESC")
    List<Envio> findByVendedorId(Integer vendedorId);
}