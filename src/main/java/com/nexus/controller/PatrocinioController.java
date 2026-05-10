package com.nexus.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.*;
import com.nexus.repository.*;
import com.nexus.service.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Gestiona las solicitudes de patrocinio iniciadas por usuarios y empresas.
 * Flujo:
 *   1. Usuario/Empresa solicita patrocinio de un producto, oferta o vehículo → SOLICITUD_USUARIO
 *   2. Admin aprueba → APROBADO_PENDIENTE_PAGO + notificación al usuario
 *   3. Usuario paga con Stripe → ACTIVE + item marcado como patrocinado
 *   4. Admin puede cancelar en cualquier momento → CANCELLED
 *
 * El admin también puede patrocinar/despatrocinar directamente cualquier item sin pago.
 */
@RestController
@RequestMapping("/api/patrocinios")
@Tag(name = "Patrocinios", description = "Solicitudes y gestión de patrocinio de anuncios")
public class PatrocinioController {

    @Value("${nexus.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Autowired private ActorRepository actorRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private OfertaRepository ofertaRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private NotificacionService notificacionService;
    @Autowired private StripeService stripeService;

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Actor resolverActor(UserDetails ud) {
        if (ud == null) throw new IllegalArgumentException("No autenticado");
        String key = ud.getUsername();
        return actorRepository.findByUsername(key)
                .or(() -> actorRepository.findByEmail(key))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    // ── Endpoints para el usuario/empresa ───────────────────────────────────

    /**
     * El usuario solicita patrocinar uno de sus items.
     * Body: { tipoItem, itemId, diasPatrocinio?, precioEstimado? }
     */
    @PostMapping("/solicitar")
    @Transactional
    public ResponseEntity<?> solicitarPatrocinio(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {

        Actor actor = resolverActor(ud);
        String tipoItem = (String) body.get("tipoItem"); // PRODUCTO, OFERTA, VEHICULO
        Integer itemId  = body.get("itemId") != null ? Integer.parseInt(body.get("itemId").toString()) : null;
        Integer dias    = body.get("diasPatrocinio") != null ? Integer.parseInt(body.get("diasPatrocinio").toString()) : null;
        Double precio   = body.get("precioEstimado") != null ? Double.parseDouble(body.get("precioEstimado").toString()) : null;

        if (tipoItem == null || itemId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos obligatorios: tipoItem, itemId"));
        }

        // Obtener título e imagen del item y verificar que pertenece al actor
        String itemTitulo = "";
        String itemImagen = null;
        boolean esPropietario = false;

        switch (tipoItem.toUpperCase()) {
            case "PRODUCTO" -> {
                Optional<Producto> op = productoRepository.findById(itemId);
                if (op.isEmpty()) return ResponseEntity.notFound().build();
                Producto p = op.get();
                esPropietario = p.getVendedor() != null && p.getVendedor().getId().equals(actor.getId());
                itemTitulo = p.getTitulo();
                itemImagen = p.getImagenPrincipal();
            }
            case "OFERTA" -> {
                Optional<Oferta> oo = ofertaRepository.findById(itemId);
                if (oo.isEmpty()) return ResponseEntity.notFound().build();
                Oferta o = oo.get();
                esPropietario = o.getActor() != null && o.getActor().getId().equals(actor.getId());
                itemTitulo = o.getTitulo();
                itemImagen = o.getImagenPrincipal();
            }
            case "VEHICULO" -> {
                Optional<Vehiculo> ov = vehiculoRepository.findById(itemId);
                if (ov.isEmpty()) return ResponseEntity.notFound().build();
                Vehiculo v = ov.get();
                esPropietario = v.getPublicador() != null && v.getPublicador().getId().equals(actor.getId());
                itemTitulo = v.getTitulo();
                itemImagen = v.getImagenPrincipal();
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "tipoItem debe ser PRODUCTO, OFERTA o VEHICULO"));
            }
        }

        if (!esPropietario) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No eres el propietario de este anuncio"));
        }

        // Crear el contrato de patrocinio en estado SOLICITUD_USUARIO
        Contrato contrato = new Contrato();
        contrato.setActor(actor);
        contrato.setTipoContrato(TipoContrato.PUBLICACION);
        contrato.setEstado(EstadoContrato.SOLICITUD_USUARIO);
        contrato.setTipoItem(tipoItem.toUpperCase());
        contrato.setItemId(itemId);
        contrato.setItemTitulo(itemTitulo);
        contrato.setItemImagen(itemImagen);
        contrato.setDiasPatrocinio(dias);
        contrato.setMonto(precio);
        contrato.setFecha(LocalDateTime.now());
        String descripcionAutor = (actor.getNombre() != null ? actor.getNombre() : actor.getUser())
                + " solicita patrocinar su " + tipoItem.toLowerCase() + " «" + itemTitulo + "»"
                + (dias != null ? " durante " + dias + " días" : " de forma indefinida")
                + (precio != null ? " (precio estimado: " + String.format("%.2f €", precio) + ")" : "") + ".";
        contrato.setDescripcion(descripcionAutor);

        Contrato guardado = contratoRepository.save(contrato);

        // Notificar a todos los admins
        List<Integer> adminIds = adminRepository.findAll().stream()
                .map(a -> a.getId()).collect(Collectors.toList());
        String nombreActor = actor.getNombre() != null && !actor.getNombre().isBlank()
                ? actor.getNombre() : actor.getUser();
        notificacionService.notificarSolicitudPatrocinioAdmins(adminIds, guardado.getId(),
                nombreActor, tipoItem, itemTitulo);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Solicitud enviada correctamente. El equipo de Nexus la revisará pronto.",
                "contratoId", guardado.getId()
        ));
    }

    /**
     * Lista los patrocinios del actor autenticado.
     */
    @GetMapping("/mis-patrocinios")
    public ResponseEntity<List<Contrato>> misPatrocinios(@AuthenticationPrincipal UserDetails ud) {
        Actor actor = resolverActor(ud);
        return ResponseEntity.ok(contratoRepository.findByActor_IdOrderByFechaDesc(actor.getId()));
    }

    /**
     * El usuario acepta la aprobación del admin e inicia el pago Stripe.
     */
    @PostMapping("/{id}/pagar")
    @Transactional
    public ResponseEntity<?> pagar(@PathVariable Integer id, @AuthenticationPrincipal UserDetails ud) {
        Actor actor = resolverActor(ud);
        Contrato c = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patrocinio no encontrado"));

        if (c.getActor() == null || !c.getActor().getId().equals(actor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No autorizado"));
        }
        if (c.getEstado() != EstadoContrato.APROBADO_PENDIENTE_PAGO) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este patrocinio no está en estado de pago pendiente"));
        }
        if (c.getMonto() == null || c.getMonto() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Precio no establecido por el admin"));
        }

        try {
            String successUrl = frontendUrl + "/publicidad/patrocinios?pago=ok&session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl  = frontendUrl + "/publicidad/patrocinios?pago=cancel";

            var session = stripeService.crearCheckoutContrato(id, actor.getId(), c.getMonto(), successUrl, cancelUrl);
            c.setEstado(EstadoContrato.PENDIENTE_PAGO);
            c.setStripeCheckoutSessionId(session.getId());
            contratoRepository.save(c);

            return ResponseEntity.ok(Map.of("checkoutUrl", session.getUrl(), "sessionId", session.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al crear sesión de pago: " + e.getMessage()));
        }
    }

    // ── Endpoints para el admin ──────────────────────────────────────────────

    /**
     * Listar todas las solicitudes de patrocinio pendientes de revisión.
     */
    @GetMapping("/admin/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Contrato>> pendientesAdmin() {
        List<EstadoContrato> estados = Arrays.asList(
                EstadoContrato.SOLICITUD_USUARIO,
                EstadoContrato.APROBADO_PENDIENTE_PAGO,
                EstadoContrato.PENDIENTE_PAGO,
                EstadoContrato.ACTIVE,
                EstadoContrato.CANCELLED
        );
        return ResponseEntity.ok(contratoRepository.findByEstadoIn(estados));
    }

    /**
     * Admin aprueba una solicitud, establece el precio y notifica al usuario.
     */
    @PostMapping("/admin/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> aprobar(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Contrato c = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patrocinio no encontrado"));

        if (c.getEstado() != EstadoContrato.SOLICITUD_USUARIO) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se pueden aprobar solicitudes en estado SOLICITUD_USUARIO"));
        }

        Double monto = body.get("monto") != null ? Double.parseDouble(body.get("monto").toString()) : null;
        if (monto != null) c.setMonto(monto);

        c.setEstado(EstadoContrato.APROBADO_PENDIENTE_PAGO);
        contratoRepository.save(c);

        if (c.getActor() != null) {
            notificacionService.notificarPatrocinioAprobado(
                    c.getActor().getId(), c.getId(),
                    c.getItemTitulo() != null ? c.getItemTitulo() : "tu anuncio", monto);
        }

        return ResponseEntity.ok(Map.of("mensaje", "Solicitud aprobada. El usuario recibirá una notificación para pagar."));
    }

    /**
     * Admin cancela una solicitud o patrocinio activo.
     */
    @PostMapping("/admin/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> cancelar(@PathVariable Integer id) {
        Contrato c = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patrocinio no encontrado"));

        c.setEstado(EstadoContrato.CANCELLED);
        contratoRepository.save(c);

        // Si estaba ACTIVE, quitar el patrocinio del item
        if (c.getTipoItem() != null && c.getItemId() != null) {
            quitarPatrocinioItem(c.getTipoItem(), c.getItemId());
        }

        if (c.getActor() != null) {
            notificacionService.notificarPatrocinioCancelado(
                    c.getActor().getId(), c.getId(),
                    c.getItemTitulo() != null ? c.getItemTitulo() : "tu anuncio");
        }

        return ResponseEntity.ok(Map.of("mensaje", "Patrocinio cancelado."));
    }

    /**
     * Admin patrocina directamente un item (sin solicitud del usuario).
     * Body: { tipoItem, itemId, diasPatrocinio? }
     */
    @PostMapping("/admin/activar")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> activarDirecto(@RequestBody Map<String, Object> body) {
        String tipoItem = (String) body.get("tipoItem");
        Integer itemId  = body.get("itemId") != null ? Integer.parseInt(body.get("itemId").toString()) : null;
        Integer dias    = body.get("diasPatrocinio") != null ? Integer.parseInt(body.get("diasPatrocinio").toString()) : null;

        if (tipoItem == null || itemId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos: tipoItem, itemId"));
        }

        LocalDateTime hasta = dias != null ? LocalDateTime.now().plusDays(dias) : null;

        switch (tipoItem.toUpperCase()) {
            case "PRODUCTO" -> productoRepository.findById(itemId).ifPresent(p -> {
                p.setPatrocinado(true);
                p.setPatrocinioHasta(hasta);
                productoRepository.save(p);
            });
            case "OFERTA" -> ofertaRepository.findById(itemId).ifPresent(o -> {
                o.setPatrocinada(true);
                o.setPatrocinioHasta(hasta);
                ofertaRepository.save(o);
            });
            case "VEHICULO" -> vehiculoRepository.findById(itemId).ifPresent(v -> {
                v.setPatrocinado(true);
                v.setPatrocinioHasta(hasta);
                vehiculoRepository.save(v);
            });
            default -> { return ResponseEntity.badRequest().body(Map.of("error", "tipoItem inválido")); }
        }

        return ResponseEntity.ok(Map.of("mensaje", "Item patrocinado correctamente" + (dias != null ? " durante " + dias + " días" : " de forma indefinida")));
    }

    /**
     * Admin quita el patrocinio directamente de un item.
     */
    @PostMapping("/admin/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> desactivarDirecto(@RequestBody Map<String, Object> body) {
        String tipoItem = (String) body.get("tipoItem");
        Integer itemId  = body.get("itemId") != null ? Integer.parseInt(body.get("itemId").toString()) : null;

        if (tipoItem == null || itemId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos: tipoItem, itemId"));
        }

        quitarPatrocinioItem(tipoItem, itemId);
        return ResponseEntity.ok(Map.of("mensaje", "Patrocinio eliminado del item."));
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    private void quitarPatrocinioItem(String tipoItem, Integer itemId) {
        switch (tipoItem.toUpperCase()) {
            case "PRODUCTO" -> productoRepository.findById(itemId).ifPresent(p -> {
                p.setPatrocinado(false);
                p.setPatrocinioHasta(null);
                productoRepository.save(p);
            });
            case "OFERTA" -> ofertaRepository.findById(itemId).ifPresent(o -> {
                o.setPatrocinada(false);
                o.setPatrocinioHasta(null);
                ofertaRepository.save(o);
            });
            case "VEHICULO" -> vehiculoRepository.findById(itemId).ifPresent(v -> {
                v.setPatrocinado(false);
                v.setPatrocinioHasta(null);
                vehiculoRepository.save(v);
            });
        }
    }
}
