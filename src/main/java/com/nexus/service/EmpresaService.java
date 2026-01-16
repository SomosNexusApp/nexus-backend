package com.nexus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nexus.entity.Empresa;
import com.nexus.repository.EmpresaRepository;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    public Optional<Empresa> findById(Integer id) {
        return this.empresaRepository.findById(id);
    }

    public List<Empresa> findAll() {
        return this.empresaRepository.findAll();
    }

    public Empresa save(Empresa empresa) {
        // Cifrado de contraseña al registrar una empresa nueva
        if (empresa.getPassword() != null && !empresa.getPassword().isEmpty()) {
            empresa.setPassword(passwordEncoder.encode(empresa.getPassword()));
        }
        return this.empresaRepository.save(empresa);
    }

    public Empresa update(Integer id, Empresa empresa) {
        Optional<Empresa> oEmpresa = findById(id);
        if (oEmpresa.isPresent()) {
            Empresa e = oEmpresa.get();
            
            e.setUser(empresa.getUser());
            e.setEmail(empresa.getEmail());
            
            // Solo ciframos si la empresa está cambiando su contraseña
            if (empresa.getPassword() != null && !empresa.getPassword().isEmpty()) {
                e.setPassword(passwordEncoder.encode(empresa.getPassword()));
            }
            
            e.setCif(empresa.getCif());
            
            // Guardamos directamente con el repositorio para evitar re-encriptar
            return this.empresaRepository.save(e);
        }
        return null;
    }

    public void delete(Integer id) {
        this.empresaRepository.deleteById(id);
    }
}