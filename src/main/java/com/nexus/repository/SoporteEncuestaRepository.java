package com.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nexus.soporte.SoporteEncuesta;
import java.util.Optional;

@Repository
public interface SoporteEncuestaRepository extends JpaRepository<SoporteEncuesta, Integer> {
    Optional<SoporteEncuesta> findBySessionId(Integer sessionId);
}
