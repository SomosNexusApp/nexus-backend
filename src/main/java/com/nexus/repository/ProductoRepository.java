package com.nexus.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

	List<Producto> findByVendedorIdOrderByFechaPublicacionDesc(Integer id);

	List<Producto> findByEstado(EstadoProducto estado);

	@Query("SELECT p FROM Producto p WHERE p.estado = com.nexus.entity.EstadoProducto.DISPONIBLE " +
	           "AND (?1 IS NULL OR p.categoria.nombre = ?1) " +
	           "AND (?2 IS NULL OR p.precio >= ?2) " +
	           "AND (?3 IS NULL OR p.precio <= ?3) " +
	           "AND (?4 IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', ?4, '%'))) " +
	           "AND (?5 IS NULL OR p.ubicacion LIKE CONCAT('%', ?5, '%'))")
	    Page<Producto> buscarConFiltros(
	            String categoria, 
	            Double precioMin, 
	            Double precioMax, 
	            String busqueda, 
	            String ubicacion, 
	            Pageable pageable);

	@Query("SELECT DISTINCT p.categoria.nombre FROM Producto p WHERE p.estado = com.nexus.entity.EstadoProducto.DISPONIBLE AND p.categoria IS NOT NULL ORDER BY p.categoria.nombre")
    List<String> findCategoriasDistintas();
}