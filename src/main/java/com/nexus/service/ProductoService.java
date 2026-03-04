package com.nexus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import com.nexus.entity.TipoOferta;
import com.nexus.repository.ProductoRepository;



@Service
public class ProductoService {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<Producto> findAll(  )                    { return productoRepository.findAllWithDetails(); }
    @Transactional(readOnly = true)
    public Optional<Producto> findById(Integer id)     { return productoRepository.findById(id); }
    @Transactional(readOnly = true)
    public List<Producto> findDisponibles()            { return productoRepository.findByEstado(EstadoProducto.DISPONIBLE); }

 // Reemplaza el método buscarConFiltrosPaginado en ProductoService.java
 // En ProductoService.java
    public Page<Producto> buscarConFiltrosPaginado(String categoria, TipoOferta tipoOferta, Double precioMin, 
            Double precioMax, Integer publicadorId, String busqueda, 
            String ubicacion, Pageable pageable) {
return productoRepository.buscarConFiltros(categoria, precioMin, precioMax, busqueda, ubicacion, pageable);
}

    public Producto cambiarEstado(Integer id, EstadoProducto nuevo) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        p.setEstadoProducto(nuevo);
        return productoRepository.save(p);
    }

    public Producto publicar(Producto producto, Integer usuarioId) {
        return usuarioService.findById(usuarioId).map(u -> {
            producto.setPublicador(u);
            producto.setEstadoProducto(EstadoProducto.DISPONIBLE);
            return productoRepository.save(producto);
        }).orElse(null);
    }

    public Producto update(Integer id, Producto d) {
        return productoRepository.findById(id).map(p -> {
            p.setTitulo(d.getTitulo());
            p.setDescripcion(d.getDescripcion());
            p.setPrecio(d.getPrecio());
            p.setTipoOferta(d.getTipoOferta());
            if (d.getImagenPrincipal() != null) p.setImagenPrincipal(d.getImagenPrincipal());
            return productoRepository.save(p);
        }).orElse(null);
    }

    public void delete(Integer id) {
        if (productoRepository.existsById(id)) productoRepository.deleteById(id);
    }
}