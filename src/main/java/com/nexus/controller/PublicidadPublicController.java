package com.nexus.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.dto.BannerPublicDTO;
import com.nexus.service.ContratoService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/public/publicidad")
@Tag(name = "Publicidad pública", description = "Banners patrocinados activos")
public class PublicidadPublicController {

    @Autowired
    private ContratoService contratoService;

    @GetMapping("/banners")
    public ResponseEntity<List<BannerPublicDTO>> banners() {
        return ResponseEntity.ok(contratoService.listarBannersActivosPublicos());
    }
}
