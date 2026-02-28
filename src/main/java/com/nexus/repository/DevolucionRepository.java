package com.nexus.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.nexus.entity.Devolucion;
import com.nexus.entity.EstadoDevolucion;

public interface DevolucionRepository extends JpaRepository<Devolucion, Integer> {
    Optional<Devolucion> findByCompraId(Integer compraId);

    @Query("SELECT d FROM Devolucion d WHERE d.compra.comprador.id = ?1 ORDER BY d.fechaSolicitud DESC")
    List<Devolucion> findByCompradorId(Integer compradorId);

    @Query("SELECT d FROM Devolucion d WHERE d.compra.producto.vendedor.id = :vendedorId ORDER BY d.fechaSolicitud DESC")
    List<Devolucion> findByVendedorId(Integer vendedorId);

    List<Devolucion> findByEstado(EstadoDevolucion estado);
}