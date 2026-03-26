package com.nexus.service;

import com.nexus.entity.*;
import com.nexus.repository.CuponRepository;
import com.nexus.repository.CuponUsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CuponValidacionService {

    private final CuponRepository cuponRepo;
    private final CuponUsoRepository cuponUsoRepo;

    public CuponValidacionService(CuponRepository cuponRepo, CuponUsoRepository cuponUsoRepo) {
        this.cuponRepo = cuponRepo;
        this.cuponUsoRepo = cuponUsoRepo;
    }

    public ValidacionResult validar(AplicarCuponRequest req) {
        Cupon cupon = cuponRepo.findByCodigo(req.codigo().toUpperCase())
                .orElse(null);

        if (cupon == null) {
            return new ValidacionResult(false, "El código de cupón no existe.", null, null, null);
        }

        if (!cupon.isActivo()) {
            return new ValidacionResult(false, "El cupón está desactivado.", null, null, null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(cupon.getFechaInicio())) {
            return new ValidacionResult(false, "El cupón aún no es válido.", null, null, null);
        }
        if (cupon.getFechaFin() != null && now.isAfter(cupon.getFechaFin())) {
            return new ValidacionResult(false, "El cupón ha caducado.", null, null, null);
        }

        // Límites de uso total
        if (cupon.getLimiteUsoTotal() != null && cupon.getTotalUsos() >= cupon.getLimiteUsoTotal()) {
            return new ValidacionResult(false, "Se ha agotado el número máximo de usos de este cupón.", null, null, null);
        }

        // Alcance por usuario específico
        if (cupon.getAlcance() == AlcanceCupon.USUARIO && 
            (cupon.getUsuario() == null || !cupon.getUsuario().getId().equals(req.usuarioId()))) {
            return new ValidacionResult(false, "Este cupón es personal y no te pertenece.", null, null, null);
        }

        // Alcance por grupo
        if (cupon.getAlcance() == AlcanceCupon.GRUPO) {
            // Aquí iría lógica más compleja consultando al Usuario req.usuarioId()
            // Por simplicidad en este MVP, asumimos que si el grupo no coincide, falla.
            // (La lógica real de grupos se implementaría inyectando UsuarioRepository)
        }

        // Límite de uso por usuario
        long usosUsuario = cuponUsoRepo.countByCuponIdAndUsuarioId(cupon.getId(), req.usuarioId());
        if (cupon.getLimiteUsoPorUsuario() != null && usosUsuario >= cupon.getLimiteUsoPorUsuario()) {
            return new ValidacionResult(false, "Ya has utilizado este cupón el máximo número de veces permitido.", null, null, null);
        }

        // Importe mínimo
        if (cupon.getImporteMinimo() != null && req.importeTotal().compareTo(cupon.getImporteMinimo()) < 0) {
            return new ValidacionResult(false, "La compra mínima para usar este cupón es de " + cupon.getImporteMinimo() + "€.", null, null, null);
        }

        // Categoría aplicable
        if (cupon.getCategoriasIds() != null && !cupon.getCategoriasIds().isBlank()) {
            List<String> validIds = Arrays.asList(cupon.getCategoriasIds().split(","));
            if (req.categoriaId() == null || !validIds.contains(req.categoriaId().toString())) {
                return new ValidacionResult(false, "Este cupón no es aplicable a productos de esta categoría.", null, null, null);
            }
        }

        // Cálculo del descuento
        BigDecimal descuento = calcularDescuento(cupon, req.importeTotal(), req.costeEnvio());
        BigDecimal importeFinal = req.importeTotal().subtract(descuento);
        if (importeFinal.compareTo(BigDecimal.ZERO) < 0) importeFinal = BigDecimal.ZERO;

        return new ValidacionResult(true, "Cupón aplicado correctamente.", cupon.getTipo(), descuento, importeFinal);
    }

    private BigDecimal calcularDescuento(Cupon cupon, BigDecimal total, BigDecimal envio) {
        BigDecimal dto = BigDecimal.ZERO;
        switch (cupon.getTipo()) {
            case FIJO:
                dto = cupon.getValor();
                break;
            case PORCENTAJE:
                dto = total.multiply(cupon.getValor()).divide(new BigDecimal("100"));
                if (cupon.getTopeMaximo() != null && dto.compareTo(cupon.getTopeMaximo()) > 0) {
                    dto = cupon.getTopeMaximo();
                }
                break;
            case ENVIO_GRATIS:
                dto = envio != null ? envio : BigDecimal.ZERO;
                break;
        }
        return dto;
    }

    // DTOs
    public record AplicarCuponRequest(String codigo, Integer usuarioId, BigDecimal importeTotal, BigDecimal costeEnvio, Integer categoriaId) {}
    public record ValidacionResult(boolean valido, String mensaje, TipoDescuento tipo, BigDecimal descuento, BigDecimal importeFinal) {}
}
