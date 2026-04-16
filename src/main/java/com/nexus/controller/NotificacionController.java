package com.nexus.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.NotificacionInApp;
import com.nexus.service.NotificacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Fix line 50: getNoLeidas() devuelve List<NotificacionInApp> pero el endpoint
 * espera Map<String,Long>.  Usar countNoLeidas() que devuelve long directamente.
 */
@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Gestion de notificaciones in-app")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Listado paginado.
     * GET /api/notificaciones/{actorId}?page=0&size=20
     */
    @GetMapping("/{actorId}")
    @Operation(summary = "Listar notificaciones paginadas")
    public ResponseEntity<Page<NotificacionInApp>> listar(
            @PathVariable Integer actorId,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificacionService.getNotificaciones(actorId, filter, page, size));
    }

    /**
     * Numero de no leidas (para el badge del icono).
     * GET /api/notificaciones/no-leidas/{actorId}
     *
     * FIX line 50: usa countNoLeidas() -> long, no getNoLeidas() -> List
     * Devuelve Map<String,Long> como exige la firma del metodo.
     */
    @GetMapping("/no-leidas/{actorId}")
    @Operation(summary = "Numero de notificaciones no leidas (badge)")
    public ResponseEntity<Map<String, Long>> noLeidas(@PathVariable Integer actorId) {
        long count = notificacionService.countNoLeidas(actorId);
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }

    /**
     * Notificaciones destacadas sin leer (modal prioritario al iniciar sesión).
     */
    @GetMapping("/destacadas-pendientes/{actorId}")
    @Operation(summary = "Listar notificaciones destacadas no leidas")
    public ResponseEntity<List<NotificacionInApp>> destacadasPendientes(@PathVariable Integer actorId) {
        return ResponseEntity.ok(notificacionService.getDestacadasPendientes(actorId));
    }

    /**
     * Marcar una notificacion como leida.
     * PUT /api/notificaciones/{id}/leer
     */
    @PutMapping("/{id}/leer")
    @Operation(summary = "Marcar notificacion como leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Integer id) {
        notificacionService.marcarLeida(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Marcar todas como leidas.
     * PUT /api/notificaciones/leer-todas/{actorId}
     */
    @PutMapping("/leer-todas/{actorId}")
    @Operation(summary = "Marcar todas como leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@PathVariable Integer actorId) {
        notificacionService.marcarTodasLeidas(actorId);
        return ResponseEntity.ok().build();
    }

    /**
     * Alternar estado destacado.
     */
    @PutMapping("/{id}/toggle-destacada")
    @Operation(summary = "Alternar estado destacado de una notificacion")
    public ResponseEntity<Void> toggleDestacada(@PathVariable Integer id) {
        notificacionService.toggleDestacada(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Eliminar notificacion.
     * DELETE /api/notificaciones/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar notificacion")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
