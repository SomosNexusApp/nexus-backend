package com.nexus.repository;

import com.nexus.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Integer> {

    /** Todos los tokens activos de un actor (puede tener varios dispositivos). */
    List<PushToken> findByActorIdAndActivoTrue(Integer actorId);

    /** Buscar por valor del token para upsert. */
    Optional<PushToken> findByToken(String token);

    /** Desactiva un token concreto (logout o token inválido reportado por FCM). */
    @Modifying
    @Query("UPDATE PushToken t SET t.activo = false WHERE t.token = :token")
    void deactivateByToken(@Param("token") String token);

    /** Elimina físicamente un token. */
    void deleteByToken(String token);
}
