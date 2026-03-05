package com.nexus.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.nexus.entity.*;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Integer> {

    // ── Alias que VehiculoService usa ─────────────────────────────────────
    default List<Vehiculo> findByPublicadorId(Integer publicadorId) {
        return findByPublicadorIdOrderByFechaPublicacionDesc(publicadorId);
    }

    List<Vehiculo> findByPublicadorIdOrderByFechaPublicacionDesc(Integer publicadorId);

    List<Vehiculo> findByEstadoVehiculo(EstadoVehiculo estado);

    List<Vehiculo> findByTipoVehiculoAndEstadoVehiculo(TipoVehiculo tipo, EstadoVehiculo estado);

    @Query("SELECT DISTINCT v.marca FROM Vehiculo v " +
           "WHERE v.estadoVehiculo = 'DISPONIBLE' AND v.marca IS NOT NULL ORDER BY v.marca")
    List<String> findMarcasDistintas();

    /**
     * JOIN FETCH publicador y categoria para evitar LazyInitializationException.
     * countQuery separado sin JOIN FETCH (Spring Data lo requiere para paginación).
     */
    @Query(
        value =
            "SELECT DISTINCT v FROM Vehiculo v " +
            "JOIN FETCH v.publicador p " +
            "LEFT JOIN FETCH v.categoria c " +
            "WHERE v.estadoVehiculo = com.nexus.entity.EstadoVehiculo.DISPONIBLE " +
            "AND (:tipo          IS NULL OR v.tipoVehiculo  = :tipo) " +
            "AND (:marca         IS NULL OR LOWER(v.marca)  LIKE LOWER(CONCAT('%', CAST(:marca  AS string), '%'))) " +
            "AND (:modelo        IS NULL OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', CAST(:modelo AS string), '%'))) " +
            "AND (:precioMin     IS NULL OR v.precio        >= :precioMin) " +
            "AND (:precioMax     IS NULL OR v.precio        <= :precioMax) " +
            "AND (:anioMin       IS NULL OR v.anio          >= :anioMin) " +
            "AND (:anioMax       IS NULL OR v.anio          <= :anioMax) " +
            "AND (:kmMax         IS NULL OR v.kilometros    <= :kmMax) " +
            "AND (:combustible   IS NULL OR v.combustible   = :combustible) " +
            "AND (:cambio        IS NULL OR LOWER(v.cambio) LIKE LOWER(CONCAT('%', CAST(:cambio AS string), '%'))) " +
            "AND (:busqueda      IS NULL OR LOWER(v.titulo) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) " +
            "                           OR LOWER(v.marca)  LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) " +
            "                           OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%'))) " +
            "AND (:potenciaMin   IS NULL OR v.potencia      >= :potenciaMin) " +
            "AND (:cilindradaMin IS NULL OR v.cilindrada    >= :cilindradaMin) " +
            "AND (:color         IS NULL OR LOWER(v.color)  LIKE LOWER(CONCAT('%', CAST(:color AS string), '%'))) " +
            "AND (:numeroPuertas IS NULL OR v.numeroPuertas = :numeroPuertas) " +
            "AND (:plazas        IS NULL OR v.plazas        = :plazas) " +
            "AND (:garantia      IS NULL OR v.garantia      = :garantia) " +
            "AND (:itv           IS NULL OR v.itv           = :itv)",

        countQuery =
            "SELECT COUNT(DISTINCT v) FROM Vehiculo v " +
            "WHERE v.estadoVehiculo = com.nexus.entity.EstadoVehiculo.DISPONIBLE " +
            "AND (:tipo          IS NULL OR v.tipoVehiculo  = :tipo) " +
            "AND (:marca         IS NULL OR LOWER(v.marca)  LIKE LOWER(CONCAT('%', CAST(:marca  AS string), '%'))) " +
            "AND (:modelo        IS NULL OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', CAST(:modelo AS string), '%'))) " +
            "AND (:precioMin     IS NULL OR v.precio        >= :precioMin) " +
            "AND (:precioMax     IS NULL OR v.precio        <= :precioMax) " +
            "AND (:anioMin       IS NULL OR v.anio          >= :anioMin) " +
            "AND (:anioMax       IS NULL OR v.anio          <= :anioMax) " +
            "AND (:kmMax         IS NULL OR v.kilometros    <= :kmMax) " +
            "AND (:combustible   IS NULL OR v.combustible   = :combustible) " +
            "AND (:cambio        IS NULL OR LOWER(v.cambio) LIKE LOWER(CONCAT('%', CAST(:cambio AS string), '%'))) " +
            "AND (:busqueda      IS NULL OR LOWER(v.titulo) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) " +
            "                           OR LOWER(v.marca)  LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) " +
            "                           OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%'))) " +
            "AND (:potenciaMin   IS NULL OR v.potencia      >= :potenciaMin) " +
            "AND (:cilindradaMin IS NULL OR v.cilindrada    >= :cilindradaMin) " +
            "AND (:color         IS NULL OR LOWER(v.color)  LIKE LOWER(CONCAT('%', CAST(:color AS string), '%'))) " +
            "AND (:numeroPuertas IS NULL OR v.numeroPuertas = :numeroPuertas) " +
            "AND (:plazas        IS NULL OR v.plazas        = :plazas) " +
            "AND (:garantia      IS NULL OR v.garantia      = :garantia) " +
            "AND (:itv           IS NULL OR v.itv           = :itv)"
    )
    Page<Vehiculo> buscarPaginado(
            @Param("tipo")          TipoVehiculo     tipo,
            @Param("marca")         String           marca,
            @Param("modelo")        String           modelo,
            @Param("precioMin")     Double           precioMin,
            @Param("precioMax")     Double           precioMax,
            @Param("anioMin")       Integer          anioMin,
            @Param("anioMax")       Integer          anioMax,
            @Param("kmMax")         Integer          kmMax,
            @Param("combustible")   TipoCombustible  combustible,
            @Param("cambio")        String           cambio,
            @Param("busqueda")      String           busqueda,
            @Param("potenciaMin")   Integer          potenciaMin,
            @Param("cilindradaMin") Integer          cilindradaMin,
            @Param("color")         String           color,
            @Param("numeroPuertas") Integer          numeroPuertas,
            @Param("plazas")        Integer          plazas,
            @Param("garantia")      Boolean          garantia,
            @Param("itv")           Boolean          itv,
            Pageable pageable);

    /**
     * Búsqueda simple legacy — conservada para retrocompatibilidad.
     */
    @Query(
        value =
            "SELECT v FROM Vehiculo v " +
            "JOIN FETCH v.publicador p " +
            "LEFT JOIN FETCH v.categoria c " +
            "WHERE v.estadoVehiculo = com.nexus.entity.EstadoVehiculo.DISPONIBLE " +
            "AND (:marca       IS NULL OR LOWER(v.marca) LIKE LOWER(CONCAT('%', CAST(:marca AS string), '%'))) " +
            "AND (:combustible IS NULL OR CAST(v.combustible AS string) = :combustible)",
        countQuery =
            "SELECT COUNT(v) FROM Vehiculo v " +
            "WHERE v.estadoVehiculo = com.nexus.entity.EstadoVehiculo.DISPONIBLE " +
            "AND (:marca       IS NULL OR LOWER(v.marca) LIKE LOWER(CONCAT('%', CAST(:marca AS string), '%'))) " +
            "AND (:combustible IS NULL OR CAST(v.combustible AS string) = :combustible)"
    )
    Page<Vehiculo> buscarConFiltros(
            @Param("marca")       String   marca,
            @Param("combustible") String   combustible,
            Pageable pageable);
}