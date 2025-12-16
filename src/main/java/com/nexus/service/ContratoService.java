package com.nexus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nexus.entity.Contrato;
import com.nexus.entity.Empresa;
import com.nexus.repository.ContratoRepository;

@Service
public class ContratoService {

	@Autowired
	private ContratoRepository contratoRepository;

	@Autowired
	private EmpresaService empresaService;

	public Optional<Contrato> findById(Integer id) {
		return this.contratoRepository.findById(id);
	}

	public List<Contrato> findAll() {
		return this.contratoRepository.findAll();
	}

	public Contrato save(Contrato contrato, Integer idEmpresa) {
		Optional<Empresa> oEmpresa = empresaService.findById(idEmpresa);
		
		if (oEmpresa.isEmpty()) {
			return null;
		}
		
		contrato.setEmpresa(oEmpresa.get());
		return this.contratoRepository.save(contrato);
	}

	public Contrato update(Integer id, Contrato contrato) {
		Optional<Contrato> oContrato = findById(id);
		if (oContrato.isPresent()) {
			Contrato c = oContrato.get();
			c.setTipoContrato(contrato.getTipoContrato());
			return this.contratoRepository.save(c);
		}
		return null;
	}

	public void delete(Integer id) {
		this.contratoRepository.deleteById(id);
	}
}