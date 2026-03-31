package com.nexus.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.nexus.entity.EstadoSuscripcion;
import com.nexus.entity.NewsletterSuscripcion;

@Repository
public interface NewsletterRepository extends JpaRepository<NewsletterSuscripcion, Integer> {

    Optional<NewsletterSuscripcion> findByEmail(String email);
    long countByEstado(EstadoSuscripcion estado);
    Optional<NewsletterSuscripcion> findByTokenConfirmacion(String token);
    Optional<NewsletterSuscripcion> findByTokenBaja(String token);

    List<NewsletterSuscripcion> findByEstado(EstadoSuscripcion estado);

    @Query("SELECT n FROM NewsletterSuscripcion n WHERE n.estado = 'ACTIVO' " +
           "AND (:frecuencia IS NULL OR n.frecuencia = :frecuencia)")
    List<NewsletterSuscripcion> findActivosByFrecuencia(String frecuencia);

    @Query("SELECT n FROM NewsletterSuscripcion n WHERE n.estado = 'ACTIVO' " +
           "AND n.recibirOfertas = true")
    List<NewsletterSuscripcion> findActivosConOfertas();

    @Query("SELECT COUNT(n) FROM NewsletterSuscripcion n WHERE n.estado = 'ACTIVO'")
    long countActivos();
}