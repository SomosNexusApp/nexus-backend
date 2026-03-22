package com.nexus.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.*;
import com.nexus.repository.*;

@Service
public class BloqueoService {

    @Autowired private BloqueoRepository  bloqueoRepository;
    @Autowired private UsuarioRepository  usuarioRepository;

    @Transactional
    public void bloquear(Integer bloqueadorId, Integer bloqueadoId, String motivo) {
        if (bloqueadorId.equals(bloqueadoId))
            throw new IllegalArgumentException("No puedes bloquearte a ti mismo");

        if (bloqueoRepository.existsByBloqueadorIdAndBloqueadoId(bloqueadorId, bloqueadoId))
            throw new IllegalStateException("Ya tienes a este usuario bloqueado");

        Usuario bloqueador = usuarioRepository.findById(bloqueadorId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Usuario bloqueado  = usuarioRepository.findById(bloqueadoId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Bloqueo b = new Bloqueo();
        b.setBloqueador(bloqueador);
        b.setBloqueado(bloqueado);
        b.setMotivo(motivo);
        bloqueoRepository.save(b);
    }

    @Transactional
    public void desbloquear(Integer bloqueadorId, Integer bloqueadoId) {
        bloqueoRepository.desbloquear(bloqueadorId, bloqueadoId);
    }

    public boolean estaBloqueado(Integer bloqueadorId, Integer bloqueadoId) {
        return bloqueoRepository.existsByBloqueadorIdAndBloqueadoId(bloqueadorId, bloqueadoId);
    }

    /** IDs de todos los usuarios que el usuario ha bloqueado */
    public List<Integer> getIdsBloqueados(Integer usuarioId) {
        return bloqueoRepository.findByBloqueadorId(usuarioId).stream()
                .map(b -> b.getBloqueado().getId())
                .collect(Collectors.toList());
    }

    /** IDs de todos los usuarios que han bloqueado al usuario */
    public List<Integer> getIdsBloqueadores(Integer usuarioId) {
        return bloqueoRepository.findByBloqueadoId(usuarioId).stream()
                .map(b -> b.getBloqueador().getId())
                .collect(Collectors.toList());
    }

    /** Unión de ambos: usuarios con los que no debe haber interacción */
    public List<Integer> getRelacionesBloqueo(Integer usuarioId) {
        if (usuarioId == null) return List.of();
        List<Integer> bloqueados = getIdsBloqueados(usuarioId);
        List<Integer> bloqueadores = getIdsBloqueadores(usuarioId);
        bloqueados.addAll(bloqueadores);
        return bloqueados.stream().distinct().collect(Collectors.toList());
    }
}