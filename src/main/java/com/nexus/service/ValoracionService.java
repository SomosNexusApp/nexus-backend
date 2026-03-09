package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexus.entity.*;
import com.nexus.repository.*;

@Service
public class ValoracionService {
    @Autowired
    private ValoracionRepository valoracionRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private CompraRepository compraRepository;

    public List<Valoracion> findAll() {
        return valoracionRepository.findAll();
    }

    public Optional<Valoracion> findById(Integer id) {
        return valoracionRepository.findById(id);
    }

    public List<Valoracion> getValoracionesVendedor(Integer id) {
        return valoracionRepository.findByVendedorId(id);
    }

    public List<Valoracion> getMisValoraciones(Integer id) {
        return valoracionRepository.findByCompradorId(id);
    }

    public Map<String, Object> getResumenVendedor(Integer vendedorId) {
        List<Valoracion> vals = valoracionRepository.findByVendedorId(vendedorId);
        int totalCompras = compraRepository.findByCompradorIdOrderByFechaCompraDesc(vendedorId).size();

        if (vals.isEmpty()) {
            return Map.of("media", 0.0, "total", 0, "desglose", Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 0),
                    "totalCompras", totalCompras, "tasaRespuesta", 100);
        }
        double media = Math.round(vals.stream().mapToInt(Valoracion::getPuntuacion).average().orElse(0.0) * 10.0)
                / 10.0;
        int[] d = new int[6];
        vals.forEach(v -> d[Math.min(5, Math.max(1, v.getPuntuacion()))]++);
        return Map.of("media", media, "total", vals.size(), "desglose",
                Map.of("1", d[1], "2", d[2], "3", d[3], "4", d[4], "5", d[5]), "totalCompras", totalCompras,
                "tasaRespuesta", 100);
    }

    @Transactional
    public Valoracion valorar(Integer compradorId, Integer compraId, Integer puntuacion, String comentario) {
        Actor comprador = actorRepository.findById(compradorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado"));
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
        if (!compra.getComprador().getId().equals(comprador.getId()))
            throw new IllegalArgumentException("Compra no pertenece al comprador");
        if (valoracionRepository.findByCompraId(compraId).isPresent())
            throw new IllegalStateException("Ya valorada");
        if (compra.getEstado() != EstadoCompra.COMPLETADA && compra.getEstado() != EstadoCompra.ENTREGADO)
            throw new IllegalStateException("Compra no completada");
        if (puntuacion == null || puntuacion < 1 || puntuacion > 5)
            throw new IllegalArgumentException("Puntuacion 1-5");
        Valoracion v = new Valoracion();
        v.setComprador((Usuario) comprador);
        v.setVendedor(compra.getVendedor());
        v.setCompra(compra);
        v.setPuntuacion(puntuacion);
        v.setComentario(comentario);
        Valoracion g = valoracionRepository.save(v);
        actualizarReputacion(compra.getVendedor().getId());
        return g;
    }

    @Transactional
    public Valoracion responder(Integer valoracionId, Integer vendedorId, String respuesta) {
        Valoracion v = valoracionRepository.findById(valoracionId)
                .orElseThrow(() -> new IllegalArgumentException("Valoracion no encontrada"));
        if (!v.getVendedor().getId().equals(vendedorId))
            throw new IllegalArgumentException("Sin permiso");
        v.setRespuestaVendedor(respuesta);
        v.setFechaRespuesta(LocalDateTime.now());
        return valoracionRepository.save(v);
    }

    @Transactional
    public void eliminar(Integer id) {
        valoracionRepository.deleteById(id);
    }

    private void actualizarReputacion(Integer vendedorId) {
        List<Valoracion> vals = valoracionRepository.findByVendedorId(vendedorId);
        if (vals.isEmpty())
            return;
        double media = vals.stream().mapToInt(Valoracion::getPuntuacion).average().orElse(0.0);
        actorRepository.findById(vendedorId).ifPresent(a -> {
            if (a instanceof Usuario u) {
                u.setReputacion(Math.round(media * 10.0) / 10.0);
                actorRepository.save(u);
            }
        });
    }
}
