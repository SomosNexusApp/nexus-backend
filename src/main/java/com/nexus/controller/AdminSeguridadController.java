package com.nexus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.nexus.entity.Admin;
import com.nexus.security.JWTUtils;
import com.nexus.service.AdminService;
import com.nexus.service.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/seguridad")
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class AdminSeguridadController {

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;
    @Autowired
    private JWTUtils jwtUtils;
    @Autowired
    private AdminService adminService;

    @GetMapping("/2fa/setup")
    @Operation(summary = "Generar secreto y QR para configuración inicial de 2FA")
    public ResponseEntity<?> setup2fa() {
        Admin admin = jwtUtils.userLogin();
        if (admin == null) return ResponseEntity.status(401).build();

        String secret = admin.getTwoFactorSecret();
        if (secret == null || secret.isEmpty()) {
            secret = twoFactorAuthService.generateNewSecret();
            admin.setTwoFactorSecret(secret);
            admin.setTwoFactorMethod("TOTP");
            adminService.save(admin);
        }

        try {
            String qrCode = twoFactorAuthService.getQrCodeImage(secret, admin.getEmail());
            return ResponseEntity.ok(Map.of(
                "secret", secret,
                "qrCode", qrCode,
                "enabled", admin.isTwoFactorEnabled()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error generando QR"));
        }
    }

    @PostMapping("/2fa/activar")
    @Operation(summary = "Verificar código y activar 2FA")
    public ResponseEntity<?> activar2fa(@RequestBody Map<String, String> req) {
        Admin admin = jwtUtils.userLogin();
        String code = req.get("code");

        if (twoFactorAuthService.isCodeValid(admin.getTwoFactorSecret(), code)) {
            admin.setTwoFactorEnabled(true);
            adminService.save(admin);
            return ResponseEntity.ok(Map.of("mensaje", "2FA activado correctamente"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Código inválido"));
    }

    @PostMapping("/2fa/desactivar")
    @Operation(summary = "Desactivar 2FA")
    public ResponseEntity<?> desactivar2fa(@RequestBody Map<String, String> req) {
        Admin admin = jwtUtils.userLogin();
        String code = req.get("code");

        if (twoFactorAuthService.isCodeValid(admin.getTwoFactorSecret(), code)) {
            admin.setTwoFactorEnabled(false);
            adminService.save(admin);
            return ResponseEntity.ok(Map.of("mensaje", "2FA desactivado correctamente"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Código inválido"));
    }
}
