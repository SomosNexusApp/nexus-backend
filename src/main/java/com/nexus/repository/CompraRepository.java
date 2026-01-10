package com.nexus.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Compra;
import com.nexus.entity.Producto;
import com.nexus.entity.Usuario;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Integer> {

    // Ver el historial de compras de un usuario
    List<Compra> findByComprador(Usuario comprador);

    // Ver todas las transacciones asociadas a un producto concreto
    List<Compra> findByProducto(Producto producto);
}