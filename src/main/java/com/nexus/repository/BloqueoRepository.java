package com.nexus.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import com.nexus.entity.Bloqueo;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Integer> {

    Optional<Bloqueo> findByBloqueadorIdAndBloqueadoId(Integer bloqueadorId, Integer bloqueadoId);

    @Query("SELECT b FROM Bloqueo b WHERE b.bloqueador.id = ?1")
    List<Bloqueo> findByBloqueadorId(Integer bloqueadorId);

    @Query("SELECT b FROM Bloqueo b WHERE b.bloqueado.id = ?1")
    List<Bloqueo> findByBloqueadoId(Integer bloqueadoId);

    boolean existsByBloqueadorIdAndBloqueadoId(Integer bloqueadorId, Integer bloqueadoId);

    @Modifying @Transactional
    @Query("DELETE FROM Bloqueo b WHERE b.bloqueador.id = ?1 AND b.bloqueado.id = ?2")
    void desbloquear(Integer bloqueadorId, Integer bloqueadoId);
}