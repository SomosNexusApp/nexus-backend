package com.nexus.repository;
import com.nexus.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Integer> {
	List<Comentario> findByOfertaIdOrderByFechaDesc(Integer ofertaId);
    List<Comentario> findByActorIdOrderByFechaDesc(Integer actorId);
    long countByOfertaId(Integer id);
}
