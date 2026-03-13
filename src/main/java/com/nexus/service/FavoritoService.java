package com.nexus.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.dto.FavoritoDTO;
import com.nexus.dto.ProductoResumenDTO;
import com.nexus.entity.Favorito;
import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.Usuario;
import com.nexus.repository.FavoritoRepository;


@Service
public class FavoritoService {
    
    @Autowired
    private FavoritoRepository favoritoRepository;
    
    @Transactional(readOnly = true)
    public List<FavoritoDTO> obtenerPorUsuario(Integer usuarioId) {
        List<Favorito> favoritos = favoritoRepository.findByUsuarioId(usuarioId);
        
        return favoritos.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    private FavoritoDTO convertirADTO(Favorito favorito) {
        FavoritoDTO dto = new FavoritoDTO();
        dto.setId(favorito.getId());
        dto.setFechaGuardado(favorito.getFechaGuardado());
        dto.setNota(favorito.getNota());
        
        if (favorito.getProducto() != null) {
            Producto p = favorito.getProducto();
            ProductoResumenDTO pDto = new ProductoResumenDTO();
            pDto.setId(p.getId());
            pDto.setTitulo(p.getTitulo());
            pDto.setPrecio(p.getPrecio());
            pDto.setImagenPrincipal(p.getImagenPrincipal());
            pDto.setEstado(p.getEstado() != null ? p.getEstado().name() : null);
            
            dto.setProducto(pDto);
        }
        
        return dto;
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
    
    // En FavoritoService.java añade este método:
    public void eliminarPorUsuarioYProducto(Integer usuarioId, Integer productoId) {
        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndProducto(usuarioId, productoId);
        existente.ifPresent(favorito -> favoritoRepository.delete(favorito));
    }
}