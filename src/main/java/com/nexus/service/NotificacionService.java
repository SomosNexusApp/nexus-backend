package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.Actor;
import com.nexus.entity.NotificacionInApp;
import com.nexus.entity.Producto;
import com.nexus.entity.TipoNotificacion;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.NotificacionRepository;
import com.nexus.repository.ProductoRepository;

/**
 * Notificaciones in-app: persisten en BD y se replican por STOMP a
 * /user/queue/notificaciones con el mismo payload JSON.
 */
@Service
public class NotificacionService {
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private ActorRepository actorRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    public List<NotificacionInApp> getNoLeidas(Integer actorId) {
        return notificacionRepository.findByActorIdAndLeidaFalseOrderByFechaDesc(actorId);
    }

    public List<NotificacionInApp> getTodas(Integer actorId) {
        return notificacionRepository.findByActorIdOrderByFechaDesc(actorId);
    }

    public Page<NotificacionInApp> getNotificaciones(Integer actorId, String filter, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        if ("no-leidas".equals(filter)) {
            return notificacionRepository.findByActorIdAndLeidaFalseOrderByFechaDesc(actorId, pr);
        } else if ("destacadas".equals(filter)) {
            return notificacionRepository.findByActorIdAndDestacadaTrueOrderByFechaDesc(actorId, pr);
        }
        return notificacionRepository.findByActorIdOrderByFechaDesc(actorId, pr);
    }

    public List<NotificacionInApp> getDestacadasPendientes(Integer actorId) {
        return notificacionRepository.findByActorIdAndDestacadaTrueAndLeidaFalseOrderByFechaDesc(actorId);
    }

    public long countNoLeidas(Integer actorId) {
        return notificacionRepository.countByActorIdAndLeidaFalse(actorId);
    }

    @Transactional
    public void marcarLeida(Integer id) {
        notificacionRepository.findById(id).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    @Transactional
    public void marcarTodasLeidas(Integer actorId) {
        getNoLeidas(actorId).forEach(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    @Transactional
    public void toggleDestacada(Integer id) {
        notificacionRepository.findById(id).ifPresent(n -> {
            n.setDestacada(!n.isDestacada());
            notificacionRepository.save(n);
        });
    }

    @Transactional
    public void eliminar(Integer id) {
        notificacionRepository.deleteById(id);
    }

    /**
     * Crea notificación persistida y envía el mismo objeto por WebSocket.
     */
    @Transactional
    public NotificacionInApp crear(Integer actorId, TipoNotificacion tipo, String titulo, String mensaje, String url) {
        return crear(actorId, tipo, titulo, mensaje, url, false, null);
    }

    @Transactional
    public NotificacionInApp crear(Integer actorId, TipoNotificacion tipo, String titulo, String mensaje,
            String url, boolean destacada, String metadata) {
        Actor actor = actorRepository.findById(actorId).orElse(null);
        if (actor == null) return null;
        NotificacionInApp n = new NotificacionInApp();
        n.setActor(actor);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setUrl(url);
        n.setDestacada(destacada);
        n.setMetadata(metadata);
        n.setLeida(false);
        n.setFecha(LocalDateTime.now());
        NotificacionInApp g = notificacionRepository.save(n);
        enviarPorWebSocket(g);
        return g;
    }

    /**
     * Envía una notificación por WebSocket sin persistirla en la base de datos.
     * Útil para mensajes de chat que ya tienen su propia persistencia y contador.
     */
    public void enviarSoloWS(Integer actorId, TipoNotificacion tipo, String titulo, String mensaje, String url, String metadata) {
        Actor actor = actorRepository.findById(actorId).orElse(null);
        if (actor == null) return;
        
        NotificacionInApp n = new NotificacionInApp();
        n.setId(0); // ID temporal para que el frontend lo acepte
        n.setActor(actor);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setUrl(url);
        n.setMetadata(metadata);
        n.setFecha(LocalDateTime.now());
        
        enviarPorWebSocket(n);
    }

    private void enviarPorWebSocket(NotificacionInApp g) {
        if (g.getActor() == null || g.getActor().getUser() == null) return;
        String username = g.getActor().getUser();
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/notificaciones", g);
        } catch (Exception e) {
            System.err.println("WS notificacion error para " + username + ": " + e.getMessage());
        }
    }

    public void notificarNuevoMensaje(Integer id, String remitente) {
        crear(id, TipoNotificacion.NUEVO_MENSAJE, "Nuevo mensaje",
                "Tienes un nuevo mensaje de " + remitente, "/mensajes");
    }

    /**
     * Chat (WS o REST): persiste notificación al receptor con texto coherente.
     */
    public void notificarMensajeChatRecibido(Integer receptorId, Integer remitenteId, Integer productoId,
            boolean esOferta, Double precioOferta, String roomId) {
        if (receptorId == null || remitenteId == null) return;
        String nombre = actorRepository.findById(remitenteId)
                .map(a -> (a.getNombre() != null && !a.getNombre().isBlank()) ? a.getNombre() : a.getUser())
                .orElse("Alguien");
        String tituloProducto = productoId != null
                ? productoRepository.findById(productoId).map(Producto::getTitulo).orElse("tu producto")
                : "tu producto";
        
        String metadata = roomId != null ? "{\"roomId\":\"" + roomId + "\"}" : null;

        if (Boolean.TRUE.equals(esOferta) && precioOferta != null) {
            enviarSoloWS(receptorId, TipoNotificacion.OFERTA_CHAT, "Nueva oferta de precio",
                nombre + " te ofrece " + String.format("%.2f €", precioOferta) + " por " + tituloProducto, "/mensajes", metadata);
        } else {
            enviarSoloWS(receptorId, TipoNotificacion.NUEVO_MENSAJE, "Nuevo mensaje",
                "Tienes un nuevo mensaje de " + nombre, "/mensajes", metadata);
        }
    }

    public void notificarNuevoMensajeOferta(Integer id, String remitente, double precio, String tituloProducto) {
        crear(id, TipoNotificacion.OFERTA_CHAT, "Nueva oferta de precio",
                remitente + " te ofrece " + String.format("%.2f €", precio) + " por " + tituloProducto, "/mensajes");
    }

    public void notificarNuevaCompraVendedor(Integer id, String titulo, Integer compraId, boolean destacada) {
        String meta = compraId != null ? "{\"compraId\":" + compraId + "}" : null;
        String url = compraId != null ? "/perfil?tab=ventas&compraId=" + compraId : "/perfil?tab=ventas";
        crear(id, TipoNotificacion.COMPRA_PAGADA_VENDEDOR, "¡Han comprado tu producto!",
                "Pago confirmado: " + titulo + ". Prepara el envío desde Mis ventas.", url, destacada, meta);
    }

    /** Notificación genérica de venta (compatibilidad). */
    public void notificarNuevaCompra(Integer id, String titulo) {
        crear(id, TipoNotificacion.NUEVA_COMPRA, "Nueva venta", "Han comprado tu producto: " + titulo, "/perfil?tab=ventas");
    }

    public void notificarCompraConfirmadaComprador(Integer id, String titulo, Integer compraId) {
        String url = compraId != null ? "/perfil?tab=compras&compraId=" + compraId : "/perfil?tab=compras";
        crear(id, TipoNotificacion.COMPRA_PAGADA_COMPRADOR, "Compra confirmada",
                "Tu compra de «" + titulo + "» está en marcha. El vendedor preparará el envío.", url);
    }

    public void notificarCompraConfirmada(Integer id, String titulo) {
        notificarCompraConfirmadaComprador(id, titulo, null);
    }

    public void notificarEnvio(Integer id, String titulo) {
        crear(id, TipoNotificacion.ENVIO_ACTUALIZADO, "Pedido enviado",
                "Tu pedido de " + titulo + " ha sido enviado", "/perfil?tab=compras");
    }

    public void notificarNuevaValoracion(Integer id, int puntuacion) {
        crear(id, TipoNotificacion.NUEVA_VALORACION, "Nueva valoración",
                "Has recibido " + puntuacion + " estrellas", "/perfil?tab=resumen");
    }

    public void notificarSparkEnOferta(Integer id, String titulo) {
        crear(id, TipoNotificacion.SPARK_EN_OFERTA, "Tu oferta tiene nuevos votos",
                "\"" + titulo + "\" ha recibido Sparks", "/ofertas");
    }

    public void notificarNuevoComentario(Integer id, String titulo) {
        crear(id, TipoNotificacion.NUEVO_COMENTARIO, "Nuevo comentario",
                "Han comentado en: " + titulo, "/ofertas");
    }

    public void notificarDevolucion(Integer id, String titulo) {
        crear(id, TipoNotificacion.DEVOLUCION, "Solicitud de devolución",
                "Han solicitado devolución de: " + titulo, "/perfil?tab=ventas");
    }

    public void notificarDevolucionActualizacion(Integer actorId, String titulo, String mensaje, String url) {
        crear(actorId, TipoNotificacion.DEVOLUCION_ACTUALIZACION, "Actualización de devolución", mensaje, url);
    }

    public void notificarAccionAdmin(Integer id, String titulo, String mensaje, String url) {
        crear(id, TipoNotificacion.ACCION_ADMIN, titulo, mensaje, url);
    }

    /** Propuesta de contrato publicitario (presupuesto) enviada desde el panel admin. */
    public void notificarContratoPropuesta(Integer empresaActorId, Integer contratoId, Double presupuesto,
            String resumen) {
        String url = contratoId != null ? "/publicidad/contratos?destacar=" + contratoId : "/publicidad/contratos";
        String msg = resumen != null && !resumen.isBlank()
                ? resumen
                : "Presupuesto indicado: " + (presupuesto != null ? String.format("%.2f €", presupuesto) : "—")
                        + ". Revisa la propuesta y acepta para pagar con tarjeta (Stripe). "
                        + "También puedes entrar desde Perfil, sección Publicidad.";
        crear(empresaActorId, TipoNotificacion.CONTRATO_PROPUESTA, "Propuesta de publicidad Nexus",
                msg, url, true, contratoId != null ? "{\"contratoId\":" + contratoId + "}" : null);
    }

    public void notificarSistema(Integer id, String mensaje) {
        crear(id, TipoNotificacion.SISTEMA, "Notificación del sistema", mensaje, "/ayuda");
    }

    public void notificarCaducidadAnuncio(Integer id, String titulo, String mensaje, String url) {
        crear(id, TipoNotificacion.CADUCIDAD_ANUNCIO, "Tu anuncio caduca pronto", mensaje, url);
    }

    public void notificarEnvioPlazo(Integer id, String mensaje, String url) {
        crear(id, TipoNotificacion.ENVIO_PLAZO, "Plazo de envío", mensaje, url);
    }

    public void notificarReembolsoAutomatico(Integer id, String titulo, String mensaje, String url) {
        crear(id, TipoNotificacion.REEMBOLSO_AUTOMATICO, titulo, mensaje, url);
    }

    public void notificarFavoritoProducto(Integer vendedorId, String fanName, String tituloProducto, Integer productoId) {
        String url = productoId != null ? "/productos/" + productoId : "/perfil?tab=favoritos";
        crear(vendedorId, TipoNotificacion.FAVORITO_PRODUCTO, "Nuevo favorito en tu anuncio",
                fanName + " ha guardado «" + tituloProducto + "» en favoritos.", url);
    }

    public void notificarFavoritoVehiculo(Integer publisherId, String fanName, String tituloVehiculo, Integer vehiculoId) {
        String url = vehiculoId != null ? "/vehiculos/" + vehiculoId : "/perfil?tab=favoritos";
        crear(publisherId, TipoNotificacion.FAVORITO_PRODUCTO, "Nuevo favorito en tu vehículo",
                fanName + " ha guardado «" + tituloVehiculo + "» en favoritos.", url);
    }

    public void notificarFavoritoOferta(Integer actorId, String fanName, String tituloOferta, Integer ofertaId) {
        String url = ofertaId != null ? "/ofertas/" + ofertaId : "/perfil?tab=favoritos";
        crear(actorId, TipoNotificacion.FAVORITO_OFERTA, "Nuevo favorito en tu oferta",
                fanName + " ha guardado «" + tituloOferta + "» en favoritos.", url);
    }

    /**
     * Tras el pago: instrucciones amplias para el vendedor (pasos envío + CTA pantalla envío).
     */
    public void notificarGuiaEnvioVendedor(Integer vendedorId, String titulo, String codigoEnvio,
            String ciudadOrigen, String urlPantallaEnvio, int diasPlazo) {
        String ciudad = ciudadOrigen != null && !ciudadOrigen.isBlank() ? ciudadOrigen : "tu zona";
        String msg = String.join(
                " ",
                "Código interno Nexus: " + codigoEnvio + ".",
                "Tienes " + diasPlazo + " días para entregar el paquete al transportista.",
                "Empaqueta bien el artículo, imprime o muestra el QR desde la pantalla de envío.",
                "Puntos de recogida en " + ciudad + " en la app.",
                "Si no envías a tiempo, el comprador será reembolsado.");
        crear(vendedorId, TipoNotificacion.GUIA_ENVIO_VENDEDOR, "Cómo enviar «" + titulo + "»",
                msg, urlPantallaEnvio, false, null);
    }

    public void notificarSeguimientoVendedor(Integer vendedorId, String titulo, String tracking, String url) {
        crear(vendedorId, TipoNotificacion.ENVIO_ACTUALIZADO, "Seguimiento registrado",
                "Has marcado como enviado «" + titulo + "». Nº seguimiento: " + tracking + ".", url);
    }

    /** Notifica al admin de una nueva solicitud de patrocinio de un usuario/empresa. */
    public void notificarSolicitudPatrocinioAdmins(List<Integer> adminIds, Integer contratoId,
            String nombreActor, String tipoItem, String itemTitulo) {
        String url = "/admin/patrocinios/" + contratoId;
        String msg = nombreActor + " ha solicitado patrocinar su " + tipoItem.toLowerCase()
                + " «" + itemTitulo + "». Revísalo en el panel de administración.";
        for (Integer adminId : adminIds) {
            crear(adminId, TipoNotificacion.SOLICITUD_PATROCINIO, "Nueva solicitud de patrocinio", msg, url, true,
                    "{\"contratoId\":" + contratoId + "}");
        }
    }

    /** Notifica al usuario que su patrocinio fue aprobado por el admin y puede pagar. */
    public void notificarPatrocinioAprobado(Integer actorId, Integer contratoId, String itemTitulo, Double monto) {
        String url = "/publicidad/patrocinios";
        String msg = "¡Tu solicitud de patrocinio para «" + itemTitulo + "» ha sido aprobada! "
                + (monto != null ? "Precio: " + String.format("%.2f €", monto) + ". " : "")
                + "Accede a 'Mis patrocinios' para completar el pago y activarlo.";
        crear(actorId, TipoNotificacion.PATROCINIO_APROBADO, "Patrocinio aprobado ✓", msg, url, true,
                "{\"contratoId\":" + contratoId + "}");
    }

    /** Notifica al usuario que su solicitud de patrocinio fue cancelada por el admin. */
    public void notificarPatrocinioCancelado(Integer actorId, Integer contratoId, String itemTitulo) {
        String url = "/publicidad/patrocinios";
        String msg = "Tu solicitud de patrocinio para «" + itemTitulo + "» ha sido revisada y no ha sido aprobada. "
                + "Puedes enviar una nueva solicitud si lo deseas.";
        crear(actorId, TipoNotificacion.PATROCINIO_CANCELADO, "Solicitud de patrocinio no aprobada", msg, url);
    }
}
