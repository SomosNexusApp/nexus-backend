package com.nexus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nexus.entity.Admin;
import com.nexus.repository.AdminRepository;

@Service
public class AdminService {
    
    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;
    
    public Optional<Admin> findById(Integer id) {
        return this.adminRepository.findById(id);
    }
    
    public List<Admin> findAll() {
        return this.adminRepository.findAll();
    }
    
    public Admin save(Admin admin) {
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        return this.adminRepository.save(admin);
    }
    
    public Admin update(Integer id, Admin admin) {
        Optional<Admin> oAdmin = findById(id);
        
        if (oAdmin.isPresent()) {
            Admin a = oAdmin.get();
            
            a.setUser(admin.getUser());
            a.setEmail(admin.getEmail());
            
            // Solo ciframos si viene una nueva contraseña, si no, dejamos la que estaba
            if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
                a.setPassword(passwordEncoder.encode(admin.getPassword()));
            }
            
            a.setNivelAcceso(admin.getNivelAcceso());
            
            // Usamos el repositorio directo para evitar doble cifrado si llamamos a save()
            return this.adminRepository.save(a); 
        }
        return null;
    }
    
    public void delete(Integer id) {
        this.adminRepository.deleteById(id);
    }
}