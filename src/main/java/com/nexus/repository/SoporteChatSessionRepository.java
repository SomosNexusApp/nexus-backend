package com.nexus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.soporte.SoporteChatSession;

public interface SoporteChatSessionRepository extends JpaRepository<SoporteChatSession, Integer> {
    Optional<SoporteChatSession> findBySessionToken(String token);

    List<SoporteChatSession> findTop100ByOrderByActualizadoEnDesc();
}
