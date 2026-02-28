package com.nexus.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import com.nexus.entity.TipoOferta;
import com.nexus.entity.Usuario;
import com.nexus.repository.ProductoRepository;

@Service
public class ProductoService {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioService usuarioService;

    public List<Producto> findAll()                    { return productoRepository.findAll(); }
    public Optional<Producto> findById(Integer id)     { return productoRepository.findById(id); }
    public List<Producto> findDisponibles()            { return productoRepository.findByEstado(EstadoProducto.DISPONIBLE); }

    public Page<Producto> buscarConFiltrosPaginado(String busqueda, TipoOferta tipoOferta,
            Double precioMin, Double precioMax, Integer publicadorId, Pageable pageable) {

        List<Producto> filtrados = productoRepository
            .findByEstado(EstadoProducto.DISPONIBLE).stream()
            .filter(p -> busqueda == null || busqueda.isBlank()
                || p.getTitulo().toLowerCase().contains(busqueda.toLowerCase())
                || p.getDescripcion().toLowerCase().contains(busqueda.toLowerCase()))
            .filter(p -> tipoOferta  == null || tipoOferta.equals(p.getTipoOferta()))
            .filter(p -> precioMin   == null || p.getPrecio() >= precioMin)
            .filter(p -> precioMax   == null || p.getPrecio() <= precioMax)
            .filter(p -> publicadorId == null
                || (p.getPublicador() != null && p.getPublicador().getId() == publicadorId))
            .collect(Collectors.toList());

        int inicio = (int) pageable.getOffset();
        int fin    = Math.min(inicio + pageable.getPageSize(), filtrados.size());
        if (inicio > filtrados.size()) return new PageImpl<>(List.of(), pageable, filtrados.size());
        return new PageImpl<>(filtrados.subList(inicio, fin), pageable, filtrados.size());
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