package com.nexus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


import com.nexus.entity.Empresa;
import com.nexus.repository.EmpresaRepository;

@Service
public class EmpresaService {

	@Autowired
	private EmpresaRepository empresaRepository;

	public Optional<Empresa> findById(Integer id) {
		return this.empresaRepository.findById(id);
	}

	public List<Empresa> findAll() {
		return this.empresaRepository.findAll();
	}

	public Empresa save(Empresa empresa) {
		return this.empresaRepository.save(empresa);
	}

	public Empresa update(Integer id, Empresa empresa) {
		Optional<Empresa> oEmpresa = findById(id);
		if (oEmpresa.isPresent()) {
			Empresa e = oEmpresa.get();
			e.setUser(empresa.getUser());
			e.setEmail(empresa.getEmail());
			e.setPassword(empresa.getPassword());
			e.setCif(empresa.getCif());
			return save(e);
		}
		return null;
	}

	public void delete(Integer id) {
		this.empresaRepository.deleteById(id);
	}
}