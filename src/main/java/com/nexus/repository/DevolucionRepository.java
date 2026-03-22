package com.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Devolucion;
import java.util.Optional;

import java.util.List;

@Repository
public interface DevolucionRepository extends JpaRepository<Devolucion, Integer> {
    Optional<Devolucion> findByCompraId(Integer compraId);
    
    @Query("SELECT d FROM Devolucion d WHERE d.compra.comprador.id = :usuarioId")
    List<Devolucion> findByCompradorId(Integer usuarioId);
    
    @Query("SELECT d FROM Devolucion d WHERE d.compra.producto.vendedor.id = :usuarioId")
    List<Devolucion> findByVendedorId(Integer usuarioId);
}