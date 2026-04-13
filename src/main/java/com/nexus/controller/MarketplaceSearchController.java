package com.nexus.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.service.MarketplaceSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/market")
@Tag(name = "Marketplace Search", description = "Unified search across all entities")
public class MarketplaceSearchController {

    @Autowired
    private MarketplaceSearchService searchService;

    @GetMapping("/search")
    @Operation(summary = "Search for offers, products and vehicles in one call")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String condicion,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> results = searchService.buscarTodo(
                q, tipo, categoria, precioMin, precioMax, condicion, ubicacion, lat, lng, radius, usuarioId, orden, PageRequest.of(page, size));
        
        return ResponseEntity.ok(results);
    }
}
