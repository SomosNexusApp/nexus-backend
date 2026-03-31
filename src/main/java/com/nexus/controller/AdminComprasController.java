package com.nexus.controller;

import com.nexus.entity.Actor;
import com.nexus.entity.Compra;
import com.nexus.entity.Envio;
import com.nexus.entity.Producto;
import com.nexus.repository.CompraRepository;
import com.nexus.repository.EnvioRepository;
import com.nexus.repository.ProductoRepository;
import com.nexus.service.CompraService;
import com.nexus.service.EnvioService;
import com.nexus.service.NotificacionService;
import com.nexus.service.ShippingPriceService;
import com.nexus.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/compras")
@PreAuthorize("hasRole('ADMIN')")
public class AdminComprasController {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private CompraService compraService;

    @Autowired
    private EnvioService envioService;

    @Autowired
    private ShippingPriceService shippingPriceService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private EmailService emailService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCompras(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String metodoEntrega) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("fechaCompra").descending());
        Page<Compra> paged;

        // Filtrado simple (mejorable con Specification si crecen los filtros)
        try {
            if (estado != null && !estado.isBlank()) {
                paged = compraRepository.findAllByEstado(com.nexus.entity.EstadoCompra.valueOf(estado.toUpperCase().trim()), pageRequest);
            } else {
                paged = compraRepository.findAll(pageRequest);
            }
        } catch (IllegalArgumentException e) {
            // Si el estado no es un enum válido, ignorar filtro o devolver error controlado
            paged = compraRepository.findAll(pageRequest);
        }

        List<Object> content = new ArrayList<>();
        for (Compra c : paged.getContent()) {
            // Filtrar por metodoEntrega en memoria si se solicita (para no complicar el Repo ahora)
            if (metodoEntrega != null && !metodoEntrega.isBlank()) {
                if (c.getMetodoEntrega() == null || !c.getMetodoEntrega().name().equalsIgnoreCase(metodoEntrega)) {
                    continue;
                }
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("precioFinal", c.getPrecioFinal() != null ? c.getPrecioFinal() : 0.0);
            m.put("costoEnvio", c.getCostoEnvio() != null ? c.getCostoEnvio() : 0.0);
            m.put("comisionNexus", c.getComisionNexus() != null ? c.getComisionNexus() : 0.0);
            m.put("estado", c.getEstado() != null ? c.getEstado().name() : "PENDIENTE");
            m.put("fechaCompra", c.getFechaCompra());
            m.put("metodoEntrega", c.getMetodoEntrega() != null ? c.getMetodoEntrega().name() : "ENVIO_PAQUETERIA");

            m.put("comprador", miniActor(c.getComprador()));
            m.put("producto", miniProducto(c.getProducto()));
            
            if (c.getProducto() != null) {
                m.put("vendedor", miniActor(c.getProducto().getPublicador()));
            }

            // Datos de envío si existe
            envioRepository.findByCompraId(c.getId()).ifPresent(envio -> {
                Map<String, Object> envInfo = new LinkedHashMap<>();
                envInfo.put("id", envio.getId());
                envInfo.put("codigoEnvio", envio.getCodigoEnvio());
                envInfo.put("transportista", envio.getTransportista());
                envInfo.put("estado", envio.getEstado() != null ? envio.getEstado().name() : null);
                m.put("envio", envInfo);
            });

            content.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("content", content);
        res.put("totalElements", paged.getTotalElements());
        res.put("totalPages", paged.getTotalPages());
        res.put("number", paged.getNumber());
        res.put("size", paged.getSize());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/reembolsar")
    @Transactional
    public ResponseEntity<Map<String, String>> reembolsarCompra(@PathVariable Integer id) {
        try {
            Compra compra = compraRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
            
            String motivo = "Incidencia administrativa";
            envioService.procesarReembolso(id);
            
            // Notificar al comprador
            notificacionService.crear(compra.getComprador().getId(), 
                com.nexus.entity.TipoNotificacion.REEMBOLSO_AUTOMATICO, 
                "Reembolso procesado", 
                "El administrador ha reembolsado tu compra de «" + compra.getProducto().getTitulo() + "».", 
                "/perfil?tab=compras");
            
            // Notificar al vendedor
            notificacionService.crear(compra.getProducto().getPublicador().getId(), 
                com.nexus.entity.TipoNotificacion.ACCION_ADMIN, 
                "Venta reembolsada por administración", 
                "Se ha procesado un reembolso para «" + compra.getProducto().getTitulo() + "». La venta ha sido anulada.", 
                "/perfil?tab=ventas");
            if (compra.getComprador().getEmail() != null) {
                emailService.enviarAdminReembolsoComprador(compra.getComprador().getEmail(), compra.getId(),
                        compra.getProducto().getTitulo(), motivo);
            }
            if (compra.getProducto().getPublicador().getEmail() != null) {
                emailService.enviarAdminReembolsoVendedor(compra.getProducto().getPublicador().getEmail(), compra.getId(),
                        compra.getProducto().getTitulo(), motivo);
            }

            return ResponseEntity.ok(Map.of("mensaje", "Reembolso procesado y usuarios notificados"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error procesando reembolso: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<Map<String, String>> cancelarCompra(@PathVariable Integer id) {
        try {
            Compra compra = compraRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));

            compraService.cancelar(id);

            // Notificaciones
            notificacionService.crear(compra.getComprador().getId(), 
                com.nexus.entity.TipoNotificacion.ACCION_ADMIN, 
                "Compra cancelada por Administración", 
                "Tu pedido de «" + compra.getProducto().getTitulo() + "» ha sido cancelado por el equipo de Nexus.", 
                "/perfil?tab=compras");

            notificacionService.crear(compra.getProducto().getPublicador().getId(), 
                com.nexus.entity.TipoNotificacion.ACCION_ADMIN, 
                "Venta cancelada por Administración", 
                "El pedido de «" + compra.getProducto().getTitulo() + "» ha sido cancelado por administración.", 
                "/perfil?tab=ventas");
            if (compra.getComprador().getEmail() != null) {
                emailService.enviarAdminCancelacion(compra.getComprador().getEmail(), compra.getId(),
                        compra.getProducto().getTitulo(), true);
            }
            if (compra.getProducto().getPublicador().getEmail() != null) {
                emailService.enviarAdminCancelacion(compra.getProducto().getPublicador().getEmail(), compra.getId(),
                        compra.getProducto().getTitulo(), false);
            }

            return ResponseEntity.ok(Map.of("mensaje", "Compra cancelada correctamente y usuarios notificados"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/regenerar-etiqueta")
    @Transactional
    public ResponseEntity<Map<String, Object>> regenerarEtiqueta(@PathVariable Integer id) {
        var envioOpt = envioRepository.findByCompraId(id);
        
        if (envioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No hay envío asociado a esta compra"));
        }

        Envio envio = envioOpt.get();
        Compra compra = envio.getCompra();

        if ("ENVIADO".equals(envio.getEstado().name()) || "ENTREGADO".equals(envio.getEstado().name())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El envío ya fue procesado y no se puede generar nueva etiqueta"));
        }

        String nuevoCodigo = shippingPriceService.generateShippingCode();
        while (envioRepository.findByCodigoEnvio(nuevoCodigo).isPresent()) {
            nuevoCodigo = shippingPriceService.generateShippingCode();
        }

        envio.setCodigoEnvio(nuevoCodigo);
        envio.setQrBase64(shippingPriceService.generateQrBase64(nuevoCodigo));
        envioRepository.save(envio);

        // Notificar al vendedor con la nueva guía
        notificacionService.crear(compra.getProducto().getPublicador().getId(), 
            com.nexus.entity.TipoNotificacion.ENVIO_ACTUALIZADO, 
            "Nueva etiqueta de envío generada", 
            "El administrador ha regenerado el código de envío para «" + compra.getProducto().getTitulo() + "». Por favor, usa la nueva etiqueta.", 
            "/compras/" + compra.getId() + "/enviar");
        if (compra.getProducto().getPublicador().getEmail() != null) {
            emailService.enviarNuevaEtiquetaVendedor(
                    compra.getProducto().getPublicador().getEmail(),
                    compra.getId(),
                    compra.getProducto().getTitulo(),
                    nuevoCodigo,
                    envio.getQrBase64());
        }

        return ResponseEntity.ok(Map.of(
            "mensaje", "Etiqueta regenerada existosamente y vendedor notificado",
            "nuevoCodigo", nuevoCodigo
        ));
    }

    @PostMapping("/{id}/refresh-tracking")
    @Transactional
    public ResponseEntity<?> refreshTracking(@PathVariable Integer id) {
        var envioOpt = envioRepository.findByCompraId(id);
        if (envioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No hay envío asociado a esta compra"));
        }
        Envio envio = envioOpt.get();
        return envioService.refrescarTrackingEnvio(envio.getId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("mensaje", "Sin cambios de tracking")));
    }

    private Map<String, Object> miniActor(Actor a) {
        if (a == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("user", a.getUser());
        m.put("avatar", a.getAvatar() != null ? a.getAvatar() : "");
        return m;
    }

    private Map<String, Object> miniProducto(Producto p) {
        if (p == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("titulo", p.getTitulo());
        m.put("imagenPrincipal", p.getImagenPrincipal() != null ? p.getImagenPrincipal() : "");
        return m;
    }
}
