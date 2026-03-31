package com.nexus.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Actor;
import com.nexus.entity.Contrato;
import com.nexus.entity.Empresa;
import com.nexus.repository.ActorRepository;
import com.nexus.service.ContratoService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/empresas/contratos")
@Tag(name = "Empresa contratos", description = "Propuestas y pago de publicidad")
public class EmpresaContratoController {

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private ActorRepository actorRepository;

    /** UserDetails usa el login; el JWT puede traer email o user. */
    private Actor resolverActor(UserDetails ud) {
        if (ud == null) {
            throw new IllegalArgumentException("No autenticado");
        }
        String key = ud.getUsername();
        return actorRepository.findByUsername(key)
                .or(() -> actorRepository.findByEmail(key))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    @GetMapping("/mios")
    public ResponseEntity<List<Contrato>> misContratos(@AuthenticationPrincipal UserDetails ud) {
        Actor a = resolverActor(ud);
        if (!(a instanceof Empresa)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(contratoService.listarPorEmpresaActorId(a.getId()));
    }

    @PostMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptar(@PathVariable Integer id, @AuthenticationPrincipal UserDetails ud) {
        Actor a = resolverActor(ud);
        if (!(a instanceof Empresa)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo cuentas empresa"));
        }
        try {
            return ResponseEntity.ok(contratoService.aceptarPropuesta(id, a.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Integer id, @AuthenticationPrincipal UserDetails ud) {
        Actor a = resolverActor(ud);
        if (!(a instanceof Empresa)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo cuentas empresa"));
        }
        try {
            contratoService.rechazarPropuesta(id, a.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
