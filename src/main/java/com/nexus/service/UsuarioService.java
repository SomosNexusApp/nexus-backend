package com.nexus.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import com.nexus.entity.*;
import com.nexus.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${nexus.verification.expiry-minutes:30}")
    private int verifyExpiry;

    private final ConcurrentHashMap<Integer, VerifEntry> codes = new ConcurrentHashMap<>();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Actor actor = actorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No encontrado: " + username));

        if (actor.isCuentaEliminada())
            throw new UsernameNotFoundException("Cuenta eliminada");

        Collection<GrantedAuthority> auth = List.of(new SimpleGrantedAuthority(obtenerRol(actor)));
        return new User(actor.getUser(), actor.getPassword(), auth);
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Integer id) {
        return usuarioRepository.findById(id);
    }

    @Transactional
    public Usuario save(Usuario u) {
        if (u.getPassword() != null && !u.getPassword().startsWith("$2a$")) {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
        }
        return usuarioRepository.save(u);
    }

    @Transactional
    public void delete(Integer id) {
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setCuentaEliminada(true);
            u.setEmail("deleted_" + id + "@nexus.deleted");
            usuarioRepository.save(u);
        });
    }

    @Transactional
    public Usuario registrarUsuario(Usuario u) {
        // 1. Verificaciones previas
        if (actorRepository.findByUsername(u.getUser()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }
        if (actorRepository.findByEmail(u.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // 2. Encriptar la contraseña de forma segura y marcar como NO verificada
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        u.setCuentaVerificada(false);

        // 3. Guardar en base de datos
        Usuario g = usuarioRepository.save(u);

        // 4. Generar código de 6 dígitos y guardarlo en memoria temporal
        String cod = codigo6();
        codes.put(g.getId(), new VerifEntry(cod, g.getEmail(), LocalDateTime.now().plusMinutes(verifyExpiry)));

        // 5. Enviar el correo electrónico con el código
        // (Si el usuario introdujo nombre usa el nombre, sino usa el username)
        String nombreParaCorreo = (g.getNombre() != null && !g.getNombre().isBlank()) ? g.getNombre() : g.getUser();
        emailService.enviarVerificacion(g.getEmail(), nombreParaCorreo, cod);

        return g;
    }

    @Transactional
    public boolean verificarCuenta(String email, String codigo) {
        Actor a = actorRepository.findByEmail(email).orElse(null);
        if (a == null)
            return false;

        VerifEntry e = codes.get(a.getId());
        if (e == null || !e.cod().equals(codigo))
            return false;

        if (e.expira().isBefore(LocalDateTime.now())) {
            codes.remove(a.getId());
            return false;
        }

        a.setCuentaVerificada(true);
        actorRepository.save(a);
        codes.remove(a.getId());
        return true;
    }

    @Transactional
    public Map<String, Object> ingresarConGoogle(String tokenId) throws Exception {
        GoogleIdTokenVerifier v = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(List.of(googleClientId)).build();

        GoogleIdToken t = v.verify(tokenId);
        if (t == null)
            throw new IllegalArgumentException("Token inválido");

        GoogleIdToken.Payload p = t.getPayload();
        String email = p.getEmail();
        String nombre = (String) p.get("given_name");
        String foto = (String) p.get("picture");
        if (foto == null || foto.trim().isEmpty()) {
            String baseName = nombre != null ? nombre : email.split("@")[0];
            foto = "https://ui-avatars.com/api/?name="
                    + java.net.URLEncoder.encode(baseName, java.nio.charset.StandardCharsets.UTF_8)
                    + "&background=random";
        }

        Map<String, Object> resultado = new HashMap<>();
        Optional<Actor> existente = actorRepository.findByEmail(email);

        if (existente.isPresent()) {
            Actor a = existente.get();
            // Siempre sincronizamos la foto de Google si el usuario la tiene
            if (foto != null && !foto.trim().isEmpty()) {
                a.setAvatar(foto);
                actorRepository.save(a);
            }
            resultado.put("actor", a);
            resultado.put("esNuevo", false);
        } else {
            // Generar el nombre de usuario base a partir del correo (ej: pepe@gmail.com ->
            // pepe)
            String baseUser = email.contains("@") ? email.substring(0, email.indexOf("@")) : nombre;

            Usuario nu = new Usuario();
            nu.setEmail(email);
            nu.setUser(usernameUnico(baseUser));
            nu.setNombre(nombre); // Guardamos su nombre real de Google
            nu.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            nu.setAvatar(foto);
            nu.setCuentaVerificada(true); // Las cuentas de Google ya están verificadas por Google

            resultado.put("actor", usuarioRepository.save(nu));
            resultado.put("esNuevo", true); // Usuario recién creado, disparará el popup de términos
        }

        return resultado;
    }

    public String obtenerRol(Actor a) {
        if (a instanceof Admin)
            return "ADMIN";
        if (a instanceof Empresa)
            return "EMPRESA";
        return "USUARIO";
    }

    private String codigo6() {
        return String.format("%06d", new SecureRandom().nextInt(999999));
    }

    private String usernameUnico(String base) {
        String l = (base != null) ? base.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() : "user";
        if (l.isEmpty())
            l = "user";

        String c = l;
        int i = 1;
        while (actorRepository.findByUsername(c).isPresent()) {
            c = l + i++;
        }
        return c;
    }

    private record VerifEntry(String cod, String email, LocalDateTime expira) {
    }

    @Transactional
    public void convertirAEmpresa(Usuario u, Map<String, String> datosEmpresa) {
        Integer actorId = u.getId();
        String cif = datosEmpresa.getOrDefault("cif", "");
        String web = datosEmpresa.getOrDefault("web", "");
        String telefono = datosEmpresa.get("telefonoEmpresa");

        // 0. VERIFICACIÓN DE CIF ÚNICO
        if (cif != null && !cif.isEmpty()) {
            Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM empresa WHERE cif = :cif")
                    .setParameter("cif", cif)
                    .getSingleResult();

            if (count.intValue() > 0) {
                throw new IllegalArgumentException(
                        "El CIF introducido ya está registrado por otra cuenta empresarial.");
            }
        }

        // 1. Actualizamos el teléfono en la tabla padre (Actor)
        if (telefono != null && !telefono.isEmpty()) {
            entityManager.createNativeQuery("UPDATE actor SET telefono = :tel WHERE id = :id")
                    .setParameter("tel", telefono)
                    .setParameter("id", actorId)
                    .executeUpdate();
        }

        // 2. LIMPIEZA DE DEPENDENCIAS EXCLUSIVAS DE USUARIO
        entityManager.createNativeQuery("DELETE FROM favorito WHERE usuario_id = :id")
                .setParameter("id", actorId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM usuario_bloqueados WHERE usuario_id = :id")
                .setParameter("id", actorId)
                .executeUpdate();

        // AQUÍ ESTABA EL ERROR: Las columnas correctas son remitente_id y receptor_id
        entityManager.createNativeQuery("DELETE FROM chat_mensaje WHERE remitente_id = :id OR receptor_id = :id")
                .setParameter("id", actorId)
                .executeUpdate();

        // 3. CAMBIO DE IDENTIDAD (El corazón de la herencia JOINED)
        // Como hemos limpiado las dependencias, PostgreSQL nos dejará borrar al
        // usuario.
        entityManager.createNativeQuery("DELETE FROM usuario WHERE actor_id = :id")
                .setParameter("id", actorId)
                .executeUpdate();

        // Insertamos el registro en la tabla hija 'empresa'
        entityManager
                .createNativeQuery(
                        "INSERT INTO empresa (actor_id, cif, web, verificada) VALUES (:id, :cif, :web, false)")
                .setParameter("id", actorId)
                .setParameter("cif", cif)
                .setParameter("web", web)
                .executeUpdate();

        // 4. Limpiamos la caché de Hibernate
        entityManager.clear();
    }

    @Transactional
    public void convertirAUsuarioPersonal(Integer actorId) {
        // 1. Borramos los datos fiscales de la tabla hija 'empresa'
        entityManager.createNativeQuery("DELETE FROM empresa WHERE actor_id = :id")
                .setParameter("id", actorId)
                .executeUpdate();

        // 2. Insertamos el registro en la tabla hija 'usuario' con los valores por
        // defecto (Not Null)
        entityManager.createNativeQuery(
                "INSERT INTO usuario (actor_id, cuenta_privada, terminos_aceptados, newsletter_suscrito, " +
                        "reputacion, total_ventas, es_verificado, perfil_publico, mostrar_telefono, mostrar_ubicacion, "
                        +
                        "permitir_mensajes_desconocidos, notif_nuevos_mensajes, notif_nueva_compra, notif_valoracion, "
                        +
                        "notif_ofertas, notif_envios, notif_novedades, tipo_cuenta) " +
                        "VALUES (:id, false, true, false, 0.0, 0, false, true, false, true, true, true, true, true, true, true, true, 'PERSONAL')")
                .setParameter("id", actorId)
                .executeUpdate();

        // 3. Limpiamos la caché para que Spring Security asimile el cambio
        entityManager.clear();
    }
}