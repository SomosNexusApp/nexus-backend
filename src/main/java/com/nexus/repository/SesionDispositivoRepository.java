package com.nexus.repository;

import com.nexus.entity.SesionDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SesionDispositivoRepository extends JpaRepository<SesionDispositivo, Integer> {

    List<SesionDispositivo> findByActorIdOrderByFechaLoginDesc(Integer actorId);

    @Transactional
    @Modifying
    void deleteByActorId(Integer actorId);
}