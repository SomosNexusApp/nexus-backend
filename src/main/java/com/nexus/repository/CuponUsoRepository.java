package com.nexus.repository;

import com.nexus.entity.CuponUso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface CuponUsoRepository extends JpaRepository<CuponUso, Integer> {

    Page<CuponUso> findByCuponId(Integer cuponId, Pageable pageable);

    @Query("SELECT SUM(cu.importeAhorro) FROM CuponUso cu WHERE cu.cupon.id = :cuponId")
    BigDecimal sumAhorroByCuponId(Integer cuponId);

    @Query("SELECT SUM(cu.importeAhorro) FROM CuponUso cu")
    BigDecimal sumTotalAhorro();

    @Query("SELECT COUNT(cu) FROM CuponUso cu WHERE cu.fechaUso >= :desde")
    long countUsosDesde(LocalDateTime desde);

    @Query("SELECT cu.cupon.codigo, COUNT(cu) as usos FROM CuponUso cu GROUP BY cu.cupon.codigo ORDER BY usos DESC")
    Page<Object[]> findCuponMasUsado(Pageable pageable);

    @Query("SELECT cu.cupon.codigo, SUM(cu.importeAhorro) as ahorro FROM CuponUso cu GROUP BY cu.cupon.codigo ORDER BY ahorro DESC")
    Page<Object[]> findCuponMayorAhorro(Pageable pageable);

    long countByCuponIdAndUsuarioId(Integer cuponId, Integer usuarioId);
}
