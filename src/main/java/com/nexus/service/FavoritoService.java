package com.nexus.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.dto.FavoritoDTO;
import com.nexus.dto.ProductoResumenDTO;
import com.nexus.dto.VehiculoResumenDTO;
import com.nexus.entity.Actor;
import com.nexus.entity.Favorito;
import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.Vehiculo;
import com.nexus.entity.Usuario;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.FavoritoRepository;
import com.nexus.repository.OfertaRepository;
import com.nexus.repository.ProductoRepository;
import com.nexus.repository.VehiculoRepository;


@Service
public class FavoritoService {
    
    @Autowired
    private FavoritoRepository favoritoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private OfertaRepository ofertaRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private NotificacionService notificacionService;

    private String nombreFan(Integer usuarioId) {
        return actorRepository.findById(usuarioId)
                .map(a -> (a.getNombre() != null && !a.getNombre().isBlank()) ? a.getNombre() : a.getUser())
                .orElse("Alguien");
    }
    
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

        if (favorito.getVehiculo() != null) {
            Vehiculo v = favorito.getVehiculo();
            VehiculoResumenDTO vDto = new VehiculoResumenDTO();
            vDto.setId(v.getId());
            vDto.setTitulo(v.getTitulo());
            vDto.setPrecio(v.getPrecio());
            vDto.setImagenPrincipal(v.getImagenPrincipal());
            vDto.setMarca(v.getMarca());
            vDto.setModelo(v.getModelo());
            vDto.setKilometros(v.getKilometros());
            vDto.setEstado(v.getEstadoVehiculo() != null ? v.getEstadoVehiculo().name() : null);
            dto.setVehiculo(vDto);
        }
        
        return dto;
    }
    
    @Transactional
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
        
        Favorito guardado = favoritoRepository.save(favorito);
        ofertaRepository.findById(ofertaId).ifPresent(o -> {
            Actor prop = o.getActor();
            if (prop != null && prop.getId() != null && !prop.getId().equals(usuarioId)) {
                notificacionService.notificarFavoritoOferta(prop.getId(), nombreFan(usuarioId), o.getTitulo(), ofertaId);
            }
        });
        return guardado;
    }
    
    @Transactional
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
        
        Favorito guardado = favoritoRepository.save(favorito);
        productoRepository.findById(productoId).ifPresent(p -> {
            Actor v = p.getVendedor();
            if (v != null && v.getId() != null && !v.getId().equals(usuarioId)) {
                notificacionService.notificarFavoritoProducto(v.getId(), nombreFan(usuarioId), p.getTitulo(), productoId);
            }
        });
        return guardado;
    }
    
    public void eliminar(Integer favoritoId) {
        favoritoRepository.deleteById(favoritoId);
    }
    
    @Transactional
    public Favorito guardarVehiculo(Integer usuarioId, Integer vehiculoId) {
        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndVehiculo(usuarioId, vehiculoId);
        if (existente.isPresent()) {
            return existente.get();
        }
        
        Favorito favorito = new Favorito();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(vehiculoId);
        
        favorito.setUsuario(usuario);
        favorito.setVehiculo(vehiculo);
        
        Favorito guardado = favoritoRepository.save(favorito);
        vehiculoRepository.findById(vehiculoId).ifPresent(v -> {
            Actor pub = v.getPublicador();
            if (pub != null && pub.getId() != null && !pub.getId().equals(usuarioId)) {
                notificacionService.notificarFavoritoVehiculo(pub.getId(), nombreFan(usuarioId), v.getTitulo(), vehiculoId);
            }
        });
        return guardado;
    }

    public void eliminarPorUsuarioYProducto(Integer usuarioId, Integer productoId) {
        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndProducto(usuarioId, productoId);
        existente.ifPresent(favorito -> favoritoRepository.delete(favorito));
    }

    public void eliminarPorUsuarioYVehiculo(Integer usuarioId, Integer vehiculoId) {
        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndVehiculo(usuarioId, vehiculoId);
        existente.ifPresent(favorito -> favoritoRepository.delete(favorito));
    }

    public void eliminarPorUsuarioYOferta(Integer usuarioId, Integer ofertaId) {
        Optional<Favorito> existente = favoritoRepository.findByUsuarioAndOferta(usuarioId, ofertaId);
        existente.ifPresent(favorito -> favoritoRepository.delete(favorito));
    }
}