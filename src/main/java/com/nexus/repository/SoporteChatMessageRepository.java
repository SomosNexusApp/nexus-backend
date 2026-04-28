package com.nexus.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.soporte.SoporteChatMessage;

public interface SoporteChatMessageRepository extends JpaRepository<SoporteChatMessage, Integer> {
    List<SoporteChatMessage> findBySessionIdOrderByCreadoEnAsc(Integer sessionId);
}
