package com.nexus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.checkout.Session;

import com.nexus.dto.BannerPublicDTO;
import com.nexus.entity.Contrato;
import com.nexus.entity.Empresa;
import com.nexus.entity.EstadoContrato;
import com.nexus.entity.Producto;
import com.nexus.entity.TipoContrato;
import com.nexus.repository.ContratoRepository;
import com.nexus.repository.ProductoRepository;

@Service
public class ContratoService {

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StripeService stripeService;

    @Value("${nexus.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public Optional<Contrato> findById(Integer id) {
        return this.contratoRepository.findById(id);
    }

    public List<Contrato> findAll() {
        return this.contratoRepository.findAll();
    }

    /**
     * Admin: crea propuesta con presupuesto; la empresa recibe notificación para aceptar y pagar.
     */
    @Transactional
    public Contrato proponerDesdeAdmin(Contrato contrato, Integer idEmpresa) {
        Optional<Empresa> oEmpresa = empresaService.findById(idEmpresa);
        if (oEmpresa.isEmpty()) {
            return null;
        }
        Empresa emp = oEmpresa.get();
        contrato.setEmpresa(emp);
        contrato.setEstado(EstadoContrato.PROPUESTA_ADMIN);
        contrato.setFecha(contrato.getFecha() != null ? contrato.getFecha() : LocalDateTime.now());
        Contrato guardado = this.contratoRepository.save(contrato);
        String resumen = guardado.getDescripcion() != null ? guardado.getDescripcion() : "";
        // Notificación in-app
        notificacionService.notificarContratoPropuesta(emp.getId(), guardado.getId(), guardado.getMonto(), resumen);
        // Email a la empresa
        if (emp.getEmail() != null && !emp.getEmail().isBlank()) {
            String nombreEmpresa = emp.getNombreComercial() != null ? emp.getNombreComercial() : emp.getUser();
            String urlContratos = frontendUrl + "/publicidad/contratos";
            emailService.enviarContratoNuevaPropuesta(
                    emp.getEmail(), nombreEmpresa,
                    guardado.getMonto() != null ? guardado.getMonto() : 0.0,
                    resumen, urlContratos);
        }
        return guardado;
    }

    public List<Contrato> listarPorEmpresaActorId(Integer empresaActorId) {
        return contratoRepository.findByEmpresa_IdOrderByFechaDesc(empresaActorId);
    }

    /**
     * Empresa acepta la propuesta: se genera sesión de pago Stripe.
     */
    @Transactional
    public Map<String, String> aceptarPropuesta(Integer contratoId, Integer empresaActorId) throws Exception {
        Contrato c = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));
        if (c.getEmpresa() == null || !c.getEmpresa().getId().equals(empresaActorId)) {
            throw new IllegalStateException("No autorizado");
        }
        if (c.getEstado() != EstadoContrato.PROPUESTA_ADMIN) {
            throw new IllegalStateException("El contrato no está pendiente de aceptación");
        }
        if (c.getMonto() == null || c.getMonto() <= 0) {
            throw new IllegalStateException("Presupuesto inválido");
        }

        String successUrl = frontendUrl + "/publicidad/contratos?pago=ok&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendUrl + "/publicidad/contratos?pago=cancel";

        var session = stripeService.crearCheckoutContrato(contratoId, empresaActorId, c.getMonto(), successUrl,
                cancelUrl);
        c.setEstado(EstadoContrato.PENDIENTE_PAGO);
        c.setStripeCheckoutSessionId(session.getId());
        contratoRepository.save(c);

        Map<String, String> out = new HashMap<>();
        out.put("checkoutUrl", session.getUrl());
        out.put("sessionId", session.getId());
        return out;
    }

    @Transactional
    public void rechazarPropuesta(Integer contratoId, Integer empresaActorId) {
        Contrato c = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado"));
        if (c.getEmpresa() == null || !c.getEmpresa().getId().equals(empresaActorId)) {
            throw new IllegalStateException("No autorizado");
        }
        if (c.getEstado() != EstadoContrato.PROPUESTA_ADMIN) {
            throw new IllegalStateException("El contrato no está pendiente de aceptación");
        }
        c.setEstado(EstadoContrato.RECHAZADO);
        contratoRepository.save(c);
    }

    /**
     * Llamado desde el webhook de Stripe al completarse checkout.session.completed.
     */
    @Transactional
    public void activarTrasCheckoutCompletado(String sessionId) {
        Session stripeSession;
        try {
            stripeSession = Session.retrieve(sessionId);
        } catch (Exception e) {
            return;
        }
        if (!"paid".equals(stripeSession.getPaymentStatus())) {
            return;
        }
        Optional<Contrato> oc = contratoRepository.findByStripeCheckoutSessionId(sessionId);
        if (oc.isEmpty()) {
            return;
        }
        Contrato c = oc.get();
        if (c.getEstado() == EstadoContrato.ACTIVE) {
            return;
        }
        if (c.getEstado() != EstadoContrato.PENDIENTE_PAGO) {
            return;
        }

        String pi = stripeSession.getPaymentIntent();
        if (pi != null && !pi.isBlank()) {
            c.setStripePaymentIntentId(pi);
        }

        c.setEstado(EstadoContrato.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        if (c.getFechaInicio() == null) {
            c.setFechaInicio(now);
        }
        if (c.getFechaFin() == null) {
            c.setFechaFin(now.plusDays(30));
        }

        if (c.getTipoContrato() == TipoContrato.PUBLICACION && c.getProductoId() != null) {
            productoRepository.findById(c.getProductoId()).ifPresent(p -> {
                p.setPatrocinado(true);
                productoRepository.save(p);
            });
        }

        contratoRepository.save(c);

        // Email de confirmación de activación a la empresa
        if (c.getEmpresa() != null && c.getEmpresa().getEmail() != null) {
            String nombreEmpresa = c.getEmpresa().getNombreComercial() != null
                    ? c.getEmpresa().getNombreComercial() : c.getEmpresa().getUser();
            emailService.enviarContratoActivado(c.getEmpresa().getEmail(), nombreEmpresa,
                    c.getTipoContrato() != null ? c.getTipoContrato().name() : "BANNER");
        }
    }

    public List<BannerPublicDTO> listarBannersActivosPublicos() {
        LocalDateTime now = LocalDateTime.now();
        List<BannerPublicDTO> out = new ArrayList<>();
        for (Contrato c : contratoRepository.findByEstadoWithEmpresa(EstadoContrato.ACTIVE)) {
            if (c.getTipoContrato() != TipoContrato.BANNER) {
                continue;
            }
            if (c.getFechaFin() != null && c.getFechaFin().isBefore(now)) {
                continue;
            }
            if (c.getFechaInicio() != null && c.getFechaInicio().isAfter(now)) {
                continue;
            }
            if (c.getTextoBanner() == null || c.getTextoBanner().isBlank()) {
                continue;
            }
            BannerPublicDTO b = new BannerPublicDTO();
            b.setContratoId(c.getId());
            b.setTipoContrato(c.getTipoContrato());
            b.setTextoBanner(c.getTextoBanner());
            b.setUrlClick(c.getUrlClick());
            b.setProductoId(c.getProductoId());
            if (c.getEmpresa() != null) {
                b.setEmpresaNombre(c.getEmpresa().getNombreComercial() != null
                        ? c.getEmpresa().getNombreComercial()
                        : c.getEmpresa().getUser());
            }
            out.add(b);
        }
        return out;
    }

    public Contrato save(Contrato contrato, Integer idEmpresa) {
        Optional<Empresa> oEmpresa = empresaService.findById(idEmpresa);

        if (oEmpresa.isEmpty()) {
            return null;
        }

        contrato.setEmpresa(oEmpresa.get());
        if (contrato.getEstado() == null) {
            contrato.setEstado(EstadoContrato.DRAFT);
        }
        if (contrato.getFecha() == null) {
            contrato.setFecha(LocalDateTime.now());
        }

        return this.contratoRepository.save(contrato);
    }

    public Contrato update(Integer id, Contrato contrato) {
        Optional<Contrato> oContrato = findById(id);
        if (oContrato.isPresent()) {
            Contrato c = oContrato.get();
            c.setTipoContrato(contrato.getTipoContrato());
            c.setEstado(contrato.getEstado());
            c.setFechaInicio(contrato.getFechaInicio());
            c.setFechaFin(contrato.getFechaFin());
            c.setMonto(contrato.getMonto());
            c.setDescripcion(contrato.getDescripcion());
            c.setProductoId(contrato.getProductoId());
            c.setTextoBanner(contrato.getTextoBanner());
            c.setUrlClick(contrato.getUrlClick());
            return this.contratoRepository.save(c);
        }
        return null;
    }

    public void delete(Integer id) {
        this.contratoRepository.deleteById(id);
    }
}
