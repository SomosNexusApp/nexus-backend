package com.nexus.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Actor;
import com.nexus.entity.Comentario;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.ComentarioRepository;
import com.nexus.repository.OfertaRepository;
import com.nexus.repository.VehiculoRepository;


import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/comentario")
@Tag(name = "Comentarios", description = "Opiniones en ofertas y vehículos")
public class ComentarioController {

    @Autowired private ComentarioRepository comentarioRepository;
    @Autowired private OfertaRepository ofertaRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private ActorRepository actorRepository;

    @GetMapping("/oferta/{ofertaId}")
    public List<Comentario> porOferta(@PathVariable Integer ofertaId) {
        return comentarioRepository.findByOfertaIdOrderByFechaDesc(ofertaId);
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public List<Comentario> porVehiculo(@PathVariable Integer vehiculoId) {
        return comentarioRepository.findByVehiculoIdOrderByFechaDesc(vehiculoId);
    }

    @PostMapping
    public ResponseEntity<?> comentar(
            @RequestParam(required = false) Integer ofertaId,
            @RequestParam(required = false) Integer vehiculoId,
            @RequestParam Integer actorId,
            @RequestBody Map<String, String> body) {

        Optional<Actor> actor = actorRepository.findById(actorId);
        if (actor.isEmpty()) return ResponseEntity.badRequest().body("Actor no encontrado");

        String texto = body.get("texto");
        if (texto == null || texto.trim().isEmpty()) return ResponseEntity.badRequest().body("Texto vacío");

        Comentario comentario = new Comentario();
        comentario.setTexto(texto);
        comentario.setActor(actor.get());

        if (ofertaId != null) ofertaRepository.findById(ofertaId).ifPresent(comentario::setOferta);
        else if (vehiculoId != null) vehiculoRepository.findById(vehiculoId).ifPresent(comentario::setVehiculo);
        
        if (body.containsKey("pollJson")) comentario.setPollJson(body.get("pollJson"));

        return ResponseEntity.ok(comentarioRepository.save(comentario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        if (comentarioRepository.existsById(id)) {
            comentarioRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Borrado"));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return comentarioRepository.findById(id).map(c -> {
            if (body.containsKey("texto")) c.setTexto(body.get("texto"));
            if (body.containsKey("pollJson")) c.setPollJson(body.get("pollJson"));
            return ResponseEntity.ok(comentarioRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }
}
