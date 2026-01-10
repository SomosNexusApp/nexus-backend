package com.nexus.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import com.nexus.entity.Usuario;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // Recuperar todos los productos con cierto estado 
    List<Producto> findByEstadoProducto(EstadoProducto estadoProducto);

    // Recuperar los productos publicados por un usuario específico
    List<Producto> findByPublicador(Usuario publicador);
    
    // Búsqueda simple por título (útil para una barra de búsqueda básica)
    List<Producto> findByTituloContainingIgnoreCase(String texto);
}