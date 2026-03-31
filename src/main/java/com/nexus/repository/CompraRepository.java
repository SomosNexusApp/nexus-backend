package com.nexus.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Compra;
import com.nexus.entity.EstadoCompra;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Integer> {

    @Override
    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    org.springframework.data.domain.Page<Compra> findAll(org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByCompradorIdOrderByFechaCompraDesc(Integer compradorId);

    @Query("SELECT c FROM Compra c WHERE c.producto.vendedor.id = :vendedorId ORDER BY c.fechaCompra DESC")
    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByVendedorId(Integer vendedorId);

    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    Optional<Compra> findByStripePaymentIntentId(String paymentIntentId);

    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    org.springframework.data.domain.Page<Compra> findAllByEstado(EstadoCompra estado, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByEstado(EstadoCompra estado);

    @Query("SELECT c FROM Compra c WHERE c.comprador.id = ?1 AND c.estado = ?2")
    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByCompradorIdAndEstado(Integer compradorId, EstadoCompra estado);

    @Override
    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    Optional<Compra> findById(Integer id);

    @Query("SELECT SUM(c.precioFinal) FROM Compra c WHERE c.estado IN (com.nexus.entity.EstadoCompra.PAGADO, com.nexus.entity.EstadoCompra.ENVIADO, com.nexus.entity.EstadoCompra.ENTREGADO, com.nexus.entity.EstadoCompra.COMPLETADA)")
    Double getTotalRevenue();

    @Query("SELECT SUM(c.comisionNexus) FROM Compra c WHERE c.estado IN (com.nexus.entity.EstadoCompra.PAGADO, com.nexus.entity.EstadoCompra.ENVIADO, com.nexus.entity.EstadoCompra.ENTREGADO, com.nexus.entity.EstadoCompra.COMPLETADA)")
    Double getSumComisionesTotal();

    @Query("SELECT SUM(c.comisionNexus) FROM Compra c WHERE c.estado IN (com.nexus.entity.EstadoCompra.PAGADO, com.nexus.entity.EstadoCompra.ENVIADO, com.nexus.entity.EstadoCompra.ENTREGADO, com.nexus.entity.EstadoCompra.COMPLETADA) AND c.fechaCompra BETWEEN :start AND :end")
    Double getSumComisiones(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT CAST(c.fechaCompra AS LocalDate) as dia, SUM(c.comisionNexus) as valor " +
           "FROM Compra c WHERE c.estado IN (com.nexus.entity.EstadoCompra.PAGADO, com.nexus.entity.EstadoCompra.ENVIADO, com.nexus.entity.EstadoCompra.ENTREGADO, com.nexus.entity.EstadoCompra.COMPLETADA) AND c.fechaCompra >= :since " +
           "GROUP BY CAST(c.fechaCompra AS LocalDate) ORDER BY CAST(c.fechaCompra AS LocalDate) ASC")
    List<Map<String, Object>> getComisionesPorDia(@Param("since") LocalDateTime since);
}