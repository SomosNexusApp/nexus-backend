package com.nexus.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.user = ?1")
    Optional<Usuario> findByUsername(String username);

    @Query("SELECT u FROM Usuario u " +
           "LEFT JOIN Compra c ON c.producto.vendedor = u " +
           "WHERE u.esVerificado = true " +
           "GROUP BY u " +
           "ORDER BY SUM(CASE WHEN c.estado IN (com.nexus.entity.EstadoCompra.COMPLETADA, com.nexus.entity.EstadoCompra.ENTREGADO, com.nexus.entity.EstadoCompra.ENVIADO) THEN c.precioFinal ELSE 0 END) DESC, " +
           "u.reputacion DESC")
    List<Usuario> findTopVendedores(Pageable pageable);

    Optional<Usuario> findByGoogleId(String googleId);
}