package com.nexus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Contrato;
import com.nexus.entity.EstadoContrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {

    List<Contrato> findByEmpresa_IdOrderByFechaDesc(Integer empresaId);

    /** Contratos iniciados por el actor (patrocinios de usuarios/empresas) */
    List<Contrato> findByActor_IdOrderByFechaDesc(Integer actorId);

    List<Contrato> findByEstado(EstadoContrato estado);

    List<Contrato> findByEstadoIn(List<EstadoContrato> estados);

    @Query("SELECT c FROM Contrato c JOIN FETCH c.empresa WHERE c.estado = :estado")
    List<Contrato> findByEstadoWithEmpresa(@Param("estado") EstadoContrato estado);

    Optional<Contrato> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    @Query("SELECT c FROM Contrato c WHERE c.estado = 'ACTIVE' AND c.tipoContrato = 'PUBLICACION' AND c.productoId IS NOT NULL AND c.fechaFin > CURRENT_TIMESTAMP")
    List<Contrato> findActiveSponsoredProducts();
}
