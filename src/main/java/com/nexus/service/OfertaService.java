package com.nexus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nexus.entity.Oferta;
import com.nexus.repository.OfertaRepository;

@Service
public class OfertaService {
	@Autowired
	private OfertaRepository ofertaRepository;
	
	// Obtener todas las ofertas
	public List<Oferta> findAll() {
		return ofertaRepository.findAll();
	}
	
	
	// Obtener una oferta por id 
	public Optional<Oferta> findById(Integer id) {
		return ofertaRepository.findById(id);
	}
	
	
	// Guardar oferta
	public Oferta save(Oferta oferta) {
		return ofertaRepository.save(oferta);
	}
	
	
	// Eliminar oferta por id
	public void deleteById(Integer id) {
		ofertaRepository.deleteById(id); 
	}
}
