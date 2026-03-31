package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nexus.entity.Mensaje;
import com.nexus.entity.Producto;
import com.nexus.entity.Usuario;
import com.nexus.repository.MensajeRepository;

@Service
public class MensajeService {

    @Autowired
    private MensajeRepository mensajeRepository;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private ProductoService productoService;

    // Obtener todos
    public List<Mensaje> findAll() {
        return mensajeRepository.findAll();
    }

    // Buscar por ID
    public Optional<Mensaje> findById(Integer id) {
        return mensajeRepository.findById(id);
    }

    // MEJORA: Obtener chat por producto ORDENADO por fecha ASC (Estilo WhatsApp)
    public List<Mensaje> findByProductoId(Integer productoId) {

        Producto p = new Producto(); 
        p.setId(productoId);
        return mensajeRepository.findByProducto(p); // Ordenado por defecto o añadir Sort en repository
    }

    @Autowired
    private ModerationService moderationService;

    // Enviar mensaje
    public Mensaje save(Mensaje mensaje, Integer usuarioId, Integer productoId) {
        Optional<Usuario> oUsuario = usuarioService.findById(usuarioId);
        Optional<Producto> oProducto = productoService.findById(productoId);

        if (oUsuario.isPresent() && oProducto.isPresent()) {
            if (mensaje.getTexto() == null || mensaje.getTexto().trim().isEmpty()) {
                throw new IllegalArgumentException("El mensaje no puede estar vacío");
            }

            // Aplicar censura
            String textoCensurado = moderationService.censurarTexto(mensaje.getTexto());
            mensaje.setTexto(textoCensurado);

            mensaje.setUsuario(oUsuario.get());
            mensaje.setProducto(oProducto.get());
            
            if (mensaje.getFechaCreacion() == null) {
                mensaje.setFechaCreacion(LocalDateTime.now());
            }
            mensaje.setEstaActivo(true);
            
            return mensajeRepository.save(mensaje);
        }
        return null;
    }

    public void delete(Integer id) {
        mensajeRepository.deleteById(id);
    }
}