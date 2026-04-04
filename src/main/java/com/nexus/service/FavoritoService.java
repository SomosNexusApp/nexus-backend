package com.nexus.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.dto.FavoritoDTO;
import com.nexus.dto.ProductoResumenDTO;
import com.nexus.dto.VehiculoResumenDTO;
import com.nexus.dto.OfertaResumenDTO;
import com.nexus.entity.Actor;
import com.nexus.entity.Favorito;
import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.Vehiculo;
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
        List<Favorito> favoritos = favoritoRepository.findByActorId(usuarioId);
        return favoritos.stream().map(this::convertirADTO).collect(Collectors.toList());
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
        
        if (favorito.getOferta() != null) {
            Oferta o = favorito.getOferta();
            OfertaResumenDTO oDto = new OfertaResumenDTO();
            oDto.setId(o.getId());
            oDto.setTitulo(o.getTitulo());
            oDto.setPrecio(o.getPrecioOferta());
            oDto.setImagenPrincipal(o.getImagenPrincipal());
            dto.setOferta(oDto);
        }
        
        return dto;
    }
    
    @Transactional
    public void guardarOferta(Integer usuarioId, Integer ofertaId) {
        if (favoritoRepository.findByActorAndOferta(usuarioId, ofertaId).isPresent()) return;
        
        Actor actor = actorRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));
        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new IllegalArgumentException("Oferta no encontrada"));
        
        Favorito favorito = new Favorito();
        favorito.setActor(actor);
        favorito.setOferta(oferta);
        favoritoRepository.save(favorito);

        Actor pub = oferta.getActor();
        if (pub != null && !pub.getId().equals(usuarioId)) {
            notificacionService.notificarFavoritoOferta(pub.getId(), nombreFan(usuarioId), oferta.getTitulo(), ofertaId);
        }
    }
    
    @Transactional
    public void guardarProducto(Integer usuarioId, Integer productoId) {
        if (favoritoRepository.findByActorAndProducto(usuarioId, productoId).isPresent()) return;
        
        Actor actor = actorRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        
        Favorito favorito = new Favorito();
        favorito.setActor(actor);
        favorito.setProducto(producto);
        favoritoRepository.save(favorito);

        Actor vend = producto.getVendedor();
        if (vend != null && !vend.getId().equals(usuarioId)) {
            notificacionService.notificarFavoritoProducto(vend.getId(), nombreFan(usuarioId), producto.getTitulo(), productoId);
        }
    }
    
    @Transactional
    public void guardarVehiculo(Integer usuarioId, Integer vehiculoId) {
        if (favoritoRepository.findByActorAndVehiculo(usuarioId, vehiculoId).isPresent()) return;

        Actor actor = actorRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));
        
        Favorito favorito = new Favorito();
        favorito.setActor(actor);
        favorito.setVehiculo(vehiculo);
        favoritoRepository.save(favorito);

        Actor pub = vehiculo.getPublicador();
        if (pub != null && !pub.getId().equals(usuarioId)) {
            notificacionService.notificarFavoritoVehiculo(pub.getId(), nombreFan(usuarioId), vehiculo.getTitulo(), vehiculoId);
        }
    }

    @Transactional
    public void eliminar(Integer favoritoId) {
        favoritoRepository.deleteById(favoritoId);
    }
    
    @Transactional
    public void eliminarPorUsuarioYProducto(Integer usuarioId, Integer productoId) {
        favoritoRepository.findByActorAndProducto(usuarioId, productoId)
                .ifPresent(f -> favoritoRepository.delete(f));
    }

    @Transactional
    public void eliminarPorUsuarioYVehiculo(Integer usuarioId, Integer vehiculoId) {
        favoritoRepository.findByActorAndVehiculo(usuarioId, vehiculoId)
                .ifPresent(f -> favoritoRepository.delete(f));
    }

    @Transactional
    public void eliminarPorUsuarioYOferta(Integer usuarioId, Integer ofertaId) {
        favoritoRepository.findByActorAndOferta(usuarioId, ofertaId)
                .ifPresent(f -> favoritoRepository.delete(f));
    }
}