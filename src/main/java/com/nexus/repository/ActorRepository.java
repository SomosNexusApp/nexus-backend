package com.nexus.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.Actor;


@Repository
public interface ActorRepository extends JpaRepository<Actor, Integer> {

    @Query("SELECT a FROM Actor a WHERE a.user = ?1 AND a.cuentaEliminada = false")
    Optional<Actor> findByUsername(String username);

    @Query("SELECT a FROM Actor a WHERE a.email = ?1 AND a.cuentaEliminada = false")
    Optional<Actor> findByEmail(String email);
}