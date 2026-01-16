package com.nexus.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Mensaje;
import com.nexus.entity.Producto;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {

    // Recuperar todos los mensajes asociados a un producto pasando la entidad completa
    List<Mensaje> findByProducto(Producto producto);

    // Recuperar mensajes usando directamente el ID del producto (más eficiente si solo tienes el ID)
    List<Mensaje> findByProductoId(int productoId);
    
    // Recuperar los mensajes de un producto ordenados por fecha (útil para mostrar el chat: los más recientes arriba)
    List<Mensaje> findByProductoIdOrderByFechaCreacionDesc(int productoId);
}