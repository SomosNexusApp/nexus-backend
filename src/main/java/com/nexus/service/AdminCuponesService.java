package com.nexus.service;

import com.nexus.entity.*;
import com.nexus.repository.CuponRepository;
import com.nexus.repository.CuponUsoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class AdminCuponesService {

    private final CuponRepository cuponRepo;
    private final CuponUsoRepository cuponUsoRepo;
    private final AuditLogService auditLogService;

    public AdminCuponesService(CuponRepository cuponRepo, CuponUsoRepository cuponUsoRepo, AuditLogService auditLogService) {
        this.cuponRepo = cuponRepo;
        this.cuponUsoRepo = cuponUsoRepo;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<Cupon> buscar(Boolean activo, Boolean caducado, Pageable pageable) {
        return cuponRepo.findAdmin(activo, caducado, pageable);
    }

    public Cupon crear(Cupon cupon) {
        if (cupon.getCodigo() == null || cupon.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El código del cupón no puede estar vacío.");
        }
        if (cuponRepo.existsByCodigo(cupon.getCodigo().toUpperCase())) {
            throw new RuntimeException("El código de cupón ya existe.");
        }
        cupon.setCodigo(cupon.getCodigo().toUpperCase());
        Cupon saved = cuponRepo.save(cupon);
        auditLogService.registrar("CUPON_CREADO", saved.getId(), "admin", "Código: " + saved.getCodigo());
        return saved;
    }

    public Cupon editar(Integer id, Cupon datos) {
        Cupon c = cuponRepo.findById(id).orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        
        if (c.getTotalUsos() > 0) {
            // Solo permitir cambiar fecha fin y estado activo si ya se ha usado
            c.setFechaFin(datos.getFechaFin());
            c.setActivo(datos.isActivo());
        } else {
            // Edición completa
            c.setTipo(datos.getTipo());
            c.setValor(datos.getValor());
            c.setValorFijo(datos.getValorFijo());
            c.setValorPorcentaje(datos.getValorPorcentaje());
            c.setImporteMinimo(datos.getImporteMinimo());
            c.setTopeMaximo(datos.getTopeMaximo());
            c.setFechaInicio(datos.getFechaInicio());
            c.setFechaFin(datos.getFechaFin());
            c.setAlcance(datos.getAlcance());
            c.setUsuario(datos.getUsuario());
            c.setGrupoObjetivo(datos.getGrupoObjetivo());
            c.setLimiteUsoTotal(datos.getLimiteUsoTotal());
            c.setLimiteUsoPorUsuario(datos.getLimiteUsoPorUsuario());
            c.setCategoriasIds(datos.getCategoriasIds());
            c.setDescripcionInterna(datos.getDescripcionInterna());
        }
        
        auditLogService.registrar("CUPON_EDITADO", id, "admin", null);
        return cuponRepo.save(c);
    }

    public void desactivar(Integer id) {
        Cupon c = cuponRepo.findById(id).orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        c.setActivo(false);
        cuponRepo.save(c);
        auditLogService.registrar("CUPON_DESACTIVADO", id, "admin", null);
    }

    public void reactivar(Integer id) {
        Cupon c = cuponRepo.findById(id).orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        if (c.getFechaFin() != null && c.getFechaFin().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("No se puede reactivar un cupón cuya fecha de fin ya ha pasado.");
        }
        c.setActivo(true);
        cuponRepo.save(c);
        auditLogService.registrar("CUPON_REACTIVADO", id, "admin", null);
    }

    public void eliminar(Integer id) {
        Cupon c = cuponRepo.findById(id).orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        if (c.getTotalUsos() > 0) {
            throw new RuntimeException("No se puede eliminar un cupón que ya ha sido utilizado.");
        }
        cuponRepo.delete(c);
        auditLogService.registrar("CUPON_ELIMINADO", id, "admin", "Código: " + c.getCodigo());
    }

    @Transactional(readOnly = true)
    public CuponStats getStats() {
        BigDecimal ahorroTotal = cuponUsoRepo.sumTotalAhorro();
        if (ahorroTotal == null) ahorroTotal = BigDecimal.ZERO;
        
        long cuponesActivos = cuponRepo.countByActivoTrue();
        long usosMes = cuponUsoRepo.countUsosDesde(LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0));
        
        Page<Object[]> masUsado = cuponUsoRepo.findCuponMasUsado(Pageable.ofSize(1));
        String codeMasUsado = masUsado.isEmpty() ? "N/A" : (String) masUsado.getContent().get(0)[0];
        long numUsosMasUsado = masUsado.isEmpty() ? 0 : (long) masUsado.getContent().get(0)[1];
        
        Page<Object[]> mayorAhorro = cuponUsoRepo.findCuponMayorAhorro(Pageable.ofSize(1));
        String codeMayorAhorro = mayorAhorro.isEmpty() ? "N/A" : (String) mayorAhorro.getContent().get(0)[0];
        
        return new CuponStats(cuponesActivos, usosMes, ahorroTotal, codeMasUsado, numUsosMasUsado, codeMayorAhorro);
    }

    @Transactional(readOnly = true)
    public Page<CuponUso> getUsos(Integer cuponId, Pageable pageable) {
        return cuponUsoRepo.findByCuponId(cuponId, pageable);
    }

    public record CuponStats(long activos, long usosMes, BigDecimal ahorroTotal, String masUsado, long numUsos, String mayorAhorro) {}
}
