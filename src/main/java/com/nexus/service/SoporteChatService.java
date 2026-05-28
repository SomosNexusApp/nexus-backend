package com.nexus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.soporte.SoporteChatMessage;
import com.nexus.soporte.SoporteChatSession;
import com.nexus.soporte.SoporteMensajeRol;
import com.nexus.soporte.SoporteSessionStatus;
import com.nexus.entity.TipoNotificacion;
import com.nexus.entity.Compra;
import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.Vehiculo;
import com.nexus.repository.CompraRepository;
import com.nexus.repository.FavoritoRepository;
import com.nexus.repository.OfertaRepository;
import com.nexus.repository.ProductoRepository;
import com.nexus.repository.SoporteChatMessageRepository;
import com.nexus.repository.SoporteChatSessionRepository;
import com.nexus.repository.VehiculoRepository;
import com.nexus.repository.ActorRepository;
import com.nexus.repository.SparkVotoRepository;
import com.nexus.entity.SparkVoto;

@Service
public class SoporteChatService {

    private final SoporteChatSessionRepository sessionRepository;
    private final SoporteChatMessageRepository messageRepository;
    private final com.nexus.repository.SoporteEncuestaRepository encuestaRepository;
    private final SoporteAiService soporteAiService;
    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final OfertaRepository ofertaRepository;
    private final VehiculoRepository vehiculoRepository;
    private final FavoritoRepository favoritoRepository;
    private final ActorRepository actorRepository;
    private final SparkVotoRepository sparkVotoRepository;
    private final EnvioService envioService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    @Value("${nexus.soporte.email-escalacion:somosnexusapp@gmail.com}")
    private String emailEscalacion;

    public SoporteChatService(SoporteChatSessionRepository sessionRepository,
            SoporteChatMessageRepository messageRepository,
            com.nexus.repository.SoporteEncuestaRepository encuestaRepository,
            SoporteAiService soporteAiService,
            CompraRepository compraRepository,
            ProductoRepository productoRepository,
            OfertaRepository ofertaRepository,
            VehiculoRepository vehiculoRepository,
            FavoritoRepository favoritoRepository,
            ActorRepository actorRepository,
            SparkVotoRepository sparkVotoRepository,
            EnvioService envioService,
            NotificacionService notificacionService,
            EmailService emailService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.encuestaRepository = encuestaRepository;
        this.soporteAiService = soporteAiService;
        this.compraRepository = compraRepository;
        this.productoRepository = productoRepository;
        this.ofertaRepository = ofertaRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.favoritoRepository = favoritoRepository;
        this.actorRepository = actorRepository;
        this.sparkVotoRepository = sparkVotoRepository;
        this.envioService = envioService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
    }

    @Transactional
    public Map<String, Object> nuevaSesion(Integer usuarioIdOpcional) {
        SoporteChatSession s = new SoporteChatSession();
        s.setSessionToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8));
        s.setUsuarioId(usuarioIdOpcional);
        s = sessionRepository.save(s);

        SoporteChatMessage bienvenida = new SoporteChatMessage();
        bienvenida.setSession(s);
        bienvenida.setRol(SoporteMensajeRol.ASSISTANT);
        bienvenida.setContenido(
                "Hola, soy el asistente de Nexus. Cuéntame qué problema tienes (envío, pago, cuenta) y te ayudo.");
        messageRepository.save(bienvenida);

        return Map.of(
                "sessionId", s.getId(),
                "sessionToken", s.getSessionToken(),
                "status", s.getStatus().name(),
                "mensajes", aDtos(messageRepository.findBySessionIdOrderByCreadoEnAsc(s.getId())));
    }

    @Transactional
    public Map<String, Object> enviarMensajeUsuario(String sessionToken, String texto) {
        SoporteChatSession s = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no válida"));
        s.setActualizadoEn(LocalDateTime.now());

        SoporteChatMessage userMsg = new SoporteChatMessage();
        userMsg.setSession(s);
        userMsg.setRol(SoporteMensajeRol.USER);
        userMsg.setContenido(texto);
        messageRepository.save(userMsg);

        if (s.getStatus() == SoporteSessionStatus.CLOSED) {
            SoporteChatMessage m = new SoporteChatMessage();
            m.setSession(s);
            m.setRol(SoporteMensajeRol.SYSTEM);
            m.setContenido("Esta conversación está cerrada. Si necesitas más ayuda, inicia un nuevo chat.");
            messageRepository.save(m);
            return respuestaMap(s.getId(), s);
        }

        if (soporteAiService.pideHumanoExplicito(texto)) {
            s.setInsistenciaAgente(s.getInsistenciaAgente() + 1);
        }

        if (s.isHumanTakeover()) {
            sessionRepository.save(s);
            return respuestaMap(s.getId(), s);
        }

        List<SoporteChatMessage> historial = messageRepository.findBySessionIdOrderByCreadoEnAsc(s.getId());
        List<String> histText = historial.stream()
                .map(m -> m.getRol().name() + ": " + m.getContenido())
                .collect(Collectors.toList());

        SoporteAiService.SoporteAiResponse resAi = soporteAiService.responder(texto, histText);
        
        SoporteChatMessage bot = new SoporteChatMessage();
        bot.setSession(s);
        bot.setRol(SoporteMensajeRol.ASSISTANT);
        bot.setContenido(resAi.getContenido());
        bot.setTipoContenido(resAi.getTipoContenido());
        bot.setReferenciaId(resAi.getReferenciaId());
        messageRepository.save(bot);
        sessionRepository.save(s);

        return respuestaMap(s.getId(), s);
    }

    private Map<String, Object> respuestaMap(Integer sessionId, SoporteChatSession s) {
        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
        m.put("mensajes", aDtos(messageRepository.findBySessionIdOrderByCreadoEnAsc(sessionId)));
        m.put("humanTakeover", s.isHumanTakeover());
        m.put("status", s.getStatus().name());
        if (s.getInsistenciaAgente() >= 2) {
            m.put("escalationEmail", emailEscalacion);
        }
        return m;
    }

    public List<Map<String, Object>> mensajesPorToken(String sessionToken) {
        SoporteChatSession s = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no válida"));
        return aDtos(messageRepository.findBySessionIdOrderByCreadoEnAsc(s.getId()));
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    public List<Map<String, Object>> listarSesionesAdmin() {
        List<SoporteChatSession> list = sessionRepository.findTop100ByOrderByActualizadoEnDesc();
        List<Map<String, Object>> out = new ArrayList<>();
        for (SoporteChatSession s : list) {
            int n = messageRepository.findBySessionIdOrderByCreadoEnAsc(s.getId()).size();
            java.util.HashMap<String, Object> row = new java.util.HashMap<>();
            row.put("id", s.getId());
            row.put("sessionToken", s.getSessionToken());
            row.put("usuarioId", s.getUsuarioId());
            row.put("humanTakeover", s.isHumanTakeover());
            row.put("status", s.getStatus().name());
            row.put("insistenciaAgente", s.getInsistenciaAgente());
            row.put("actualizadoEn", s.getActualizadoEn() != null ? s.getActualizadoEn().toString() : "");
            row.put("numMensajes", n);

            // Enrich with user profile data when session is authenticated
            if (s.getUsuarioId() != null) {
                actorRepository.findById(s.getUsuarioId()).ifPresent(actor -> {
                    java.util.HashMap<String, Object> u = new java.util.HashMap<>();
                    u.put("id", actor.getId());
                    u.put("user", actor.getUser() != null ? actor.getUser() : "");
                    u.put("nombre", actor.getNombre() != null ? actor.getNombre() : "");
                    u.put("apellidos", actor.getApellidos() != null ? actor.getApellidos() : "");
                    u.put("email", actor.getEmail() != null ? actor.getEmail() : "");
                    u.put("avatar", actor.getAvatar() != null ? actor.getAvatar() : "");
                    u.put("baneado", actor.isBaneado());
                    u.put("verificado", actor.isCuentaVerificada());
                    row.put("usuario", u);
                });
            }

            out.add(row);
        }
        return out;
    }

    @Transactional
    public void takeoverAdmin(Integer sessionId) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        s.setHumanTakeover(true);
        s.setActualizadoEn(LocalDateTime.now());
        sessionRepository.save(s);

        SoporteChatMessage m = new SoporteChatMessage();
        m.setSession(s);
        m.setRol(SoporteMensajeRol.SYSTEM);
        m.setContenido("Un agente de Nexus se ha unido a la conversación. La respuesta automática está pausada.");
        messageRepository.save(m);
    }

    @Transactional
    public void responderAdmin(Integer sessionId, String texto, String tipoContenido, Integer referenciaId) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        s.setHumanTakeover(true);
        s.setActualizadoEn(LocalDateTime.now());
        sessionRepository.save(s);

        SoporteChatMessage m = new SoporteChatMessage();
        m.setSession(s);
        m.setRol(SoporteMensajeRol.ADMIN);
        m.setContenido(texto);
        m.setTipoContenido(tipoContenido);
        m.setReferenciaId(referenciaId);
        messageRepository.save(m);
    }

    @Transactional
    public void reanudarAi(Integer sessionId) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        s.setHumanTakeover(false);
        s.setActualizadoEn(LocalDateTime.now());
        sessionRepository.save(s);

        SoporteChatMessage m = new SoporteChatMessage();
        m.setSession(s);
        m.setRol(SoporteMensajeRol.SYSTEM);
        m.setContenido("Un agente ha reanudado el asistente de Nexus.");
        messageRepository.save(m);
    }

    @Transactional
    public void cerrarSesion(Integer sessionId) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        s.setStatus(SoporteSessionStatus.CLOSED);
        s.setActualizadoEn(LocalDateTime.now());
        sessionRepository.save(s);

        SoporteChatMessage m = new SoporteChatMessage();
        m.setSession(s);
        m.setRol(SoporteMensajeRol.SYSTEM);
        m.setContenido("La conversación ha sido cerrada por el equipo de soporte.");
        messageRepository.save(m);
    }

    @Transactional
    public void solicitarEncuesta(Integer sessionId) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        s.setStatus(SoporteSessionStatus.WAITING_SURVEY);
        s.setActualizadoEn(LocalDateTime.now());
        sessionRepository.save(s);

        SoporteChatMessage m = new SoporteChatMessage();
        m.setSession(s);
        m.setRol(SoporteMensajeRol.SYSTEM);
        m.setContenido("El chat ha finalizado. Por favor, califica nuestra atención para ayudarnos a mejorar.");
        m.setTipoContenido("ENCUESTA");
        messageRepository.save(m);
    }

    @Transactional
    public void guardarEncuesta(String sessionToken, int valoracion, String comentario) {
        SoporteChatSession s = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no válida"));
        com.nexus.soporte.SoporteEncuesta e = new com.nexus.soporte.SoporteEncuesta();
        e.setSessionId(s.getId());
        e.setValoracion(valoracion);
        e.setComentario(comentario);
        encuestaRepository.save(e);

        s.setStatus(SoporteSessionStatus.CLOSED);
        sessionRepository.save(s);
    }

    public List<Map<String, Object>> mensajesAdmin(Integer sessionId) {
        return aDtos(messageRepository.findBySessionIdOrderByCreadoEnAsc(sessionId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarComprasUsuarioSesion(Integer sessionId) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        if (s.getUsuarioId() == null) {
            return List.of();
        }
        return compraRepository.findByCompradorIdOrderByFechaCompraDesc(s.getUsuarioId()).stream()
                .limit(10)
                .map(c -> {
                    java.util.HashMap<String, Object> row = new java.util.HashMap<>();
                    row.put("id", c.getId());
                    row.put("estado", c.getEstado() != null ? c.getEstado().name() : "");
                    row.put("precioFinal", c.getPrecioFinal() != null ? c.getPrecioFinal() : 0.0);
                    row.put("fechaCompra", c.getFechaCompra() != null ? c.getFechaCompra().toString() : "");
                    row.put("productoTitulo", c.getProducto() != null ? c.getProducto().getTitulo() : "Producto");
                    row.put("origen", "usuario_sesion");
                    return row;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void reembolsarCompraDesdeSoporte(Integer sessionId, Integer compraId, String motivo) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        var compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
        if (s.getUsuarioId() == null || !s.getUsuarioId().equals(compra.getComprador().getId())) {
            throw new IllegalArgumentException("La compra no pertenece al usuario de la sesión");
        }

        envioService.procesarReembolso(compraId);
        notificacionService.crear(compra.getComprador().getId(), TipoNotificacion.REEMBOLSO_AUTOMATICO,
                "Reembolso desde soporte",
                "Tu compra de «" + compra.getProducto().getTitulo() + "» ha sido reembolsada por soporte. Motivo: " + motivo,
                "/perfil?tab=compras");
        notificacionService.crear(compra.getProducto().getPublicador().getId(), TipoNotificacion.ACCION_ADMIN,
                "Venta reembolsada desde soporte",
                "El equipo de soporte ha reembolsado «" + compra.getProducto().getTitulo() + "». Motivo: " + motivo,
                "/perfil?tab=ventas");
        if (compra.getComprador().getEmail() != null) {
            emailService.enviarEmailHtml(compra.getComprador().getEmail(), "Reembolso gestionado por soporte",
                    "<h2>Reembolso procesado</h2><p>Se ha reembolsado tu compra de <b>"
                            + compra.getProducto().getTitulo() + "</b>.</p><p>Motivo: "
                            + motivo + "</p>");
        }

        SoporteChatMessage m = new SoporteChatMessage();
        m.setSession(s);
        m.setRol(SoporteMensajeRol.SYSTEM);
        m.setContenido("✅ Soporte ha procesado el reembolso de la compra #" + compraId + ". Motivo: " + motivo);
        messageRepository.save(m);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarReferenciasSesion(Integer sessionId, String tipoRaw, String qRaw) {
        SoporteChatSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        if (s.getUsuarioId() == null) {
            return List.of();
        }
        String tipo = (tipoRaw == null ? "" : tipoRaw.trim().toUpperCase(Locale.ROOT));
        String q = qRaw == null ? "" : qRaw.trim().toLowerCase(Locale.ROOT);

        if ("PRODUCTO".equals(tipo)) {
            LinkedHashMap<Integer, Map<String, Object>> acc = new LinkedHashMap<>();
            // Productos publicados por el usuario
            for (Producto p : productoRepository.findByVendedorIdOrderByFechaPublicacionDesc(s.getUsuarioId())) {
                addReferenciaProducto(acc, p, "SUBIDO");
            }
            // Productos que el usuario ha comprado
            for (Compra c : compraRepository.findByCompradorIdOrderByFechaCompraDesc(s.getUsuarioId())) {
                if (c.getProducto() != null) addReferenciaProducto(acc, c.getProducto(), "COMPRADO");
            }
            // Productos en los que el usuario ha sido vendedor
            for (Compra c : compraRepository.findByVendedorId(s.getUsuarioId())) {
                if (c.getProducto() != null) addReferenciaProducto(acc, c.getProducto(), "VENDIDO");
            }
            // Productos que el usuario tiene en favoritos
            for (com.nexus.entity.Favorito fav : favoritoRepository.findByActorId(s.getUsuarioId())) {
                if (fav.getProducto() != null && !acc.containsKey(fav.getProducto().getId())) {
                    Map<String, Object> row = refProducto(fav.getProducto());
                    row.put("origen", "FAVORITO");
                    acc.put(fav.getProducto().getId(), row);
                }
            }
            // Productos a los que el usuario ha reaccionado (spark/drip)
            for (SparkVoto voto : sparkVotoRepository.findByActorId(s.getUsuarioId())) {
                if (voto.getProducto() != null && !acc.containsKey(voto.getProducto().getId())) {
                    Map<String, Object> row = refProducto(voto.getProducto());
                    row.put("origen", voto.getValor() > 0 ? "SPARK" : "DRIP");
                    acc.put(voto.getProducto().getId(), row);
                }
            }
            return acc.values().stream()
                    .filter(row -> q.isBlank() || ((String) row.getOrDefault("titulo", "")).toLowerCase(Locale.ROOT).contains(q))
                    .limit(30)
                    .collect(Collectors.toList());
        }
        if ("OFERTA".equals(tipo)) {
            LinkedHashMap<Integer, Map<String, Object>> acc = new LinkedHashMap<>();
            // Ofertas publicadas por el usuario
            for (Oferta o : ofertaRepository.findByActorId(s.getUsuarioId())) {
                if (!acc.containsKey(o.getId())) {
                    Map<String, Object> row = refOferta(o);
                    row.put("origen", "PUBLICADA");
                    acc.put(o.getId(), row);
                }
            }
            // Ofertas que tiene en favoritos
            for (com.nexus.entity.Favorito fav : favoritoRepository.findByActorId(s.getUsuarioId())) {
                if (fav.getOferta() != null && !acc.containsKey(fav.getOferta().getId())) {
                    Map<String, Object> row = refOferta(fav.getOferta());
                    row.put("origen", "FAVORITO");
                    acc.put(fav.getOferta().getId(), row);
                }
            }
            // Ofertas a las que el usuario ha reaccionado (spark/drip)
            for (SparkVoto voto : sparkVotoRepository.findByActorId(s.getUsuarioId())) {
                if (voto.getOferta() != null && !acc.containsKey(voto.getOferta().getId())) {
                    Map<String, Object> row = refOferta(voto.getOferta());
                    row.put("origen", voto.getValor() > 0 ? "SPARK" : "DRIP");
                    acc.put(voto.getOferta().getId(), row);
                }
            }
            return acc.values().stream()
                    .filter(row -> q.isBlank() || ((String) row.getOrDefault("titulo", "")).toLowerCase(Locale.ROOT).contains(q))
                    .limit(30)
                    .collect(Collectors.toList());
        }
        if ("VEHICULO".equals(tipo)) {
            LinkedHashMap<Integer, Map<String, Object>> acc = new LinkedHashMap<>();
            // Vehículos publicados por el usuario
            for (Vehiculo v : vehiculoRepository.findByPublicadorIdOrderByFechaPublicacionDesc(s.getUsuarioId())) {
                if (!acc.containsKey(v.getId())) {
                    Map<String, Object> row = refVehiculo(v);
                    row.put("origen", "PUBLICADO");
                    acc.put(v.getId(), row);
                }
            }
            // Vehículos que tiene en favoritos
            for (com.nexus.entity.Favorito fav : favoritoRepository.findByActorId(s.getUsuarioId())) {
                if (fav.getVehiculo() != null && !acc.containsKey(fav.getVehiculo().getId())) {
                    Map<String, Object> row = refVehiculo(fav.getVehiculo());
                    row.put("origen", "FAVORITO");
                    acc.put(fav.getVehiculo().getId(), row);
                }
            }
            return acc.values().stream()
                    .filter(row -> q.isBlank() || ((String) row.getOrDefault("titulo", "")).toLowerCase(Locale.ROOT).contains(q))
                    .limit(30)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void addReferenciaProducto(LinkedHashMap<Integer, Map<String, Object>> acc, Producto p, String origen) {
        if (p == null || p.getId() == null || acc.containsKey(p.getId())) return;
        Map<String, Object> row = refProducto(p);
        row.put("origen", origen);
        acc.put(p.getId(), row);
    }

    private List<Map<String, Object>> aDtos(List<SoporteChatMessage> list) {
        return list.stream().map(m -> {
            java.util.HashMap<String, Object> row = new java.util.HashMap<>();
            row.put("id", m.getId());
            row.put("rol", m.getRol().name());
            row.put("contenido", m.getContenido());
            row.put("tipoContenido", m.getTipoContenido());
            row.put("referenciaId", m.getReferenciaId());
            row.put("referencia", resolveReferencia(m.getTipoContenido(), m.getReferenciaId()));
            row.put("creadoEn", m.getCreadoEn() != null ? m.getCreadoEn().toString() : "");
            return row;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> resolveReferencia(String tipoContenido, Integer referenciaId) {
        if (tipoContenido == null || referenciaId == null) return null;
        String tipo = tipoContenido.toUpperCase(Locale.ROOT);
        if ("PRODUCTO".equals(tipo)) {
            return productoRepository.findById(referenciaId).map(this::refProducto).orElse(null);
        }
        if ("OFERTA".equals(tipo)) {
            return ofertaRepository.findById(referenciaId).map(this::refOferta).orElse(null);
        }
        if ("VEHICULO".equals(tipo)) {
            return vehiculoRepository.findById(referenciaId).map(this::refVehiculo).orElse(null);
        }
        return null;
    }

    private Map<String, Object> refProducto(Producto p) {
        java.util.HashMap<String, Object> row = new java.util.HashMap<>();
        row.put("id", p.getId());
        row.put("tipo", "PRODUCTO");
        row.put("titulo", p.getTitulo());
        row.put("precio", p.getPrecio() != null ? p.getPrecio() : 0.0);
        row.put("imagen", p.getImagenPrincipal());
        return row;
    }

    private Map<String, Object> refOferta(Oferta o) {
        java.util.HashMap<String, Object> row = new java.util.HashMap<>();
        row.put("id", o.getId());
        row.put("tipo", "OFERTA");
        row.put("titulo", o.getTitulo());
        row.put("precio", o.getPrecioOferta() != null ? o.getPrecioOferta() : 0.0);
        row.put("imagen", o.getImagenPrincipal());
        return row;
    }

    private Map<String, Object> refVehiculo(Vehiculo v) {
        java.util.HashMap<String, Object> row = new java.util.HashMap<>();
        row.put("id", v.getId());
        row.put("tipo", "VEHICULO");
        row.put("titulo", v.getTitulo());
        row.put("precio", v.getPrecio() != null ? v.getPrecio() : 0.0);
        row.put("imagen", v.getImagenPrincipal());
        return row;
    }
}
