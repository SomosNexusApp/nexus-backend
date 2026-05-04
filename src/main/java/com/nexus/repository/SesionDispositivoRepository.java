package com.nexus.repository;

import com.nexus.entity.SesionDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for device sessions.
 */
@Repository
public interface SesionDispositivoRepository extends JpaRepository<SesionDispositivo, Integer> {
    List<SesionDispositivo> findByUsuarioId(Integer usuarioId);
    Optional<SesionDispositivo> findByDeviceId(String deviceId);
}
