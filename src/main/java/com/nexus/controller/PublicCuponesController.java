package com.nexus.controller;

import com.nexus.entity.Cupon;
import com.nexus.entity.CuponUso;
import com.nexus.repository.CuponRepository;
import com.nexus.service.CuponValidacionService;
import com.nexus.repository.CuponUsoRepository;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.CompraRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/cupones")
public class PublicCuponesController {

    private final CuponValidacionService validacionService;
    private final CuponRepository cuponRepo;
    private final CuponUsoRepository cuponUsoRepo;
    private final ActorRepository actorRepo;
    private final CompraRepository compraRepo;

    public PublicCuponesController(
            CuponValidacionService validacionService, 
            CuponRepository cuponRepo,
            CuponUsoRepository cuponUsoRepo,
            ActorRepository actorRepo,
            CompraRepository compraRepo) {
        this.validacionService = validacionService;
        this.cuponRepo = cuponRepo;
        this.cuponUsoRepo = cuponUsoRepo;
        this.actorRepo = actorRepo;
        this.compraRepo = compraRepo;
    }

    @PostMapping("/aplicar")
    public CuponValidacionService.ValidacionResult aplicar(@RequestBody CuponValidacionService.AplicarCuponRequest req) {
        return validacionService.validar(req);
    }

    @PostMapping("/confirmar-uso")
    @Transactional
    public void confirmarUso(@RequestBody ConfirmarUsoRequest req) {
        Cupon cupon = cuponRepo.findById(req.cuponId())
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        
        CuponUso uso = new CuponUso();
        uso.setCupon(cupon);
        uso.setUsuario(actorRepo.findById(req.usuarioId()).orElseThrow());
        uso.setCompra(compraRepo.findById(req.compraId()).orElseThrow());
        uso.setImporteAhorro(req.importeAhorro());
        uso.setFechaUso(LocalDateTime.now());
        
        cuponUsoRepo.save(uso);
        
        // Incrementar usos en el cupón
        cupon.setTotalUsos(cupon.getTotalUsos() + 1);
        cuponRepo.save(cupon);
    }

    public record ConfirmarUsoRequest(Integer cuponId, Integer usuarioId, Integer compraId, java.math.BigDecimal importeAhorro) {}
}
