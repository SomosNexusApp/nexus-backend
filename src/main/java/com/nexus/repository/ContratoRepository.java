package com.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.entity.Contrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {
	
}
