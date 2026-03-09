package com.nexus.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Compra;
import com.nexus.entity.EstadoCompra;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Integer> {

    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByCompradorIdOrderByFechaCompraDesc(Integer compradorId);

    @Query("SELECT c FROM Compra c WHERE c.producto.vendedor.id = :vendedorId ORDER BY c.fechaCompra DESC")
    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByVendedorId(Integer vendedorId);

    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    Optional<Compra> findByStripePaymentIntentId(String paymentIntentId);

    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByEstado(EstadoCompra estado);

    @Query("SELECT c FROM Compra c WHERE c.comprador.id = ?1 AND c.estado = ?2")
    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    List<Compra> findByCompradorIdAndEstado(Integer compradorId, EstadoCompra estado);

    @Override
    @EntityGraph(attributePaths = { "comprador", "producto", "producto.vendedor", "producto.categoria" })
    Optional<Compra> findById(Integer id);
}