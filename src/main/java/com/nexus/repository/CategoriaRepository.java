package com.nexus.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    Optional<Categoria> findBySlug(String slug);
    Optional<Categoria> findByNombre(String nombre);

    @Query("SELECT c FROM Categoria c WHERE c.parent IS NULL AND c.activa = true ORDER BY c.orden ASC")
    List<Categoria> findRaizActivas();

    List<Categoria> findByActivaTrueOrderByNombreAsc();

    List<Categoria> findByParentIdAndActivaTrue(Integer parentId);

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Todas las categorías raíz ordenadas (sin filtro activa). */
    List<Categoria> findByParentIsNullOrderByOrdenAsc();

    /** Unicidad de slug para validación en tiempo real. */
    boolean existsBySlug(String slug);

    /** Conteo de productos activos para impedir borrado de categorías con contenido. */
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.categoria.id = :categoriaId")
    long countByCategoriaId(Integer categoriaId);

    /** Conteo de ofertas activas por categoría (para stats en árbol). */
    @Query("SELECT COUNT(o) FROM Oferta o WHERE o.categoria.id = :categoriaId AND o.esActiva = true")
    long countOfertasActivasByCategoriaId(Integer categoriaId);
}