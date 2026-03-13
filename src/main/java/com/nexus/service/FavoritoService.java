package com.nexus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nexus.entity.Favorito;
import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.Usuario;
import com.nexus.repository.FavoritoRepository;

@Service
public class FavoritoService {
    
    @Autowired
    private FavoritoRepository favoritoRepository;
    
    public List<Favorito> obtenerPorUsuario(Integer usuarioId) {
        return favoritoRepository.findByUsuarioId(usuarioId);
    }
    
    public Favorito guardarOferta(Integer usuarioId, Integer ofertaId) {
        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndOferta(usuarioId, ofertaId);
        if (existente.isPresent()) {
            return existente.get();
        }
        
        Favorito favorito = new Favorito();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Oferta oferta = new Oferta();
        oferta.setId(ofertaId);
        
        favorito.setUsuario(usuario);
        favorito.setOferta(oferta);
        
        return favoritoRepository.save(favorito);
    }
    
    public Favorito guardarProducto(Integer usuarioId, Integer productoId) {
        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndProducto(usuarioId, productoId);
        if (existente.isPresent()) {
            return existente.get();
        }
        
        Favorito favorito = new Favorito();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Producto producto = new Producto();
        producto.setId(productoId);
        
        favorito.setUsuario(usuario);
        favorito.setProducto(producto);
        
        return favoritoRepository.save(favorito);
    }
    
    public void eliminar(Integer favoritoId) {
        favoritoRepository.deleteById(favoritoId);
    }
}