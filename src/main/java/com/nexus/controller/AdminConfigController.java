package com.nexus.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.nexus.config.AdminConfig;
import com.nexus.repository.AdminConfigRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/config")
@Tag(name = "Admin Config", description = "Gestión de configuración global del sistema")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    @Autowired
    private AdminConfigRepository configRepository;

    @Autowired
    private com.nexus.service.ModerationService moderationService;

    @GetMapping
    @Operation(summary = "Obtener toda la configuración")
    public ResponseEntity<Map<String, String>> getAll() {
        List<AdminConfig> configs = configRepository.findAll();
        Map<String, String> map = configs.stream()
                .collect(Collectors.toMap(AdminConfig::getKey, AdminConfig::getValue));
        return ResponseEntity.ok(map);
    }

    @PostMapping("/batch")
    @Operation(summary = "Actualizar múltiples configuraciones")
    public ResponseEntity<Void> updateBatch(@RequestBody Map<String, String> configs) {
        List<AdminConfig> entities = configs.entrySet().stream()
                .map(entry -> new AdminConfig(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        configRepository.saveAll(entities);
        
        // Trigger re-compilation of moderation patterns
        moderationService.recompilarPatron();
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/moderation-words")
    @Operation(summary = "Obtener todas las palabras de moderación (Base + Personalizadas)")
    public ResponseEntity<String> getModerationWords() {
        return ResponseEntity.ok(moderationService.getAllWordsAsString());
    }
}
