package com.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Devolucion;
import java.util.Optional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.nexus.entity.EstadoDevolucion;

@Repository
public interface DevolucionRepository extends JpaRepository<Devolucion, Integer> {
    Optional<Devolucion> findByCompraId(Integer compraId);
    Page<Devolucion> findByEstado(EstadoDevolucion estado, Pageable pageable);
    
    @Query("SELECT d FROM Devolucion d WHERE d.compra.comprador.id = :usuarioId")
    List<Devolucion> findByCompradorId(Integer usuarioId);
    
    @Query("SELECT d FROM Devolucion d WHERE d.compra.producto.vendedor.id = :usuarioId")
    List<Devolucion> findByVendedorId(Integer usuarioId);
}