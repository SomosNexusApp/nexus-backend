package com.nexus.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nexus.entity.Actor;
import com.nexus.entity.Admin;
import com.nexus.entity.Empresa;
import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository; 
import com.nexus.repository.UsuarioRepository;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ActorRepository actorRepository; 
    @Autowired
    @Lazy 
    private PasswordEncoder passwordEncoder;


    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscamos en la tabla general de Actores para permitir login de Admin/Empresa/Usuario
        Actor actor = actorRepository.findByUser(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String rol;
        if (actor instanceof Admin) {
            rol = "ADMIN";
        } else if (actor instanceof Empresa) {
            rol = "EMPRESA";
        } else {
            rol = "USUARIO";
        }

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(rol));
        return new User(actor.getUser(), actor.getPassword(), authorities);
    }

  

    public Optional<Usuario> findById(Integer id) {
        return this.usuarioRepository.findById(id);
    }

    public List<Usuario> findAll() {
        return this.usuarioRepository.findAll();
    }

    public Usuario save(Usuario usuario) {
        
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return this.usuarioRepository.save(usuario);
    }

    public Usuario update(Integer id, Usuario usuario) {
        Optional<Usuario> oUsuario = findById(id);

        if (oUsuario.isPresent()) {
            Usuario u = oUsuario.get();

            u.setUser(usuario.getUser());
            u.setEmail(usuario.getEmail());
            
           
            if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                u.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }

            u.setTelefono(usuario.getTelefono());
            u.setEsVerificado(usuario.getEsVerificado());
            u.setFotoPerfil(usuario.getFotoPerfil());
            u.setBiografia(usuario.getBiografia());
            u.setUbicacion(usuario.getUbicacion());
            u.setReputacion(usuario.getReputacion());

         
            return this.usuarioRepository.save(u);
        }
        return null;
    }

    public void delete(Integer id) {
        this.usuarioRepository.deleteById(id);
    }
}