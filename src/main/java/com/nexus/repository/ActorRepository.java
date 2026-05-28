package com.nexus.repository;

import java.util.Optional;
import java.util.List;
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

    // busca un actor por su token de reset de contrasena (sustituye a PasswordResetTokenRepository)
    @Query("SELECT a FROM Actor a WHERE a.resetToken = ?1")
    Optional<Actor> findByResetToken(String token);

    @Query("SELECT CAST(a.fechaRegistro AS LocalDate) as dia, COUNT(a) as valor " +
           "FROM Actor a WHERE a.fechaRegistro >= :since " +
           "GROUP BY CAST(a.fechaRegistro AS LocalDate) ORDER BY CAST(a.fechaRegistro AS LocalDate) ASC")
    List<java.util.Map<String, Object>> getUsuariosPorDia(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Actor a SET a.flagFraude = false WHERE a.id = :id")
    void clearFlagFraude(@org.springframework.data.repository.query.Param("id") Integer id);
}