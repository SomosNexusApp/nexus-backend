package com.nexus.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Comentario;
import com.nexus.entity.Oferta;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Integer>{
}
