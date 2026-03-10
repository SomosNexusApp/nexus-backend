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
            resultado.put("actor", existente.get());
            resultado.put("esNuevo", false); // Ya existía, hace login normal
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
        // Creamos la nueva instancia de Empresa
        Empresa e = new Empresa();

        // Copiamos sus credenciales y datos base
        e.setUser(u.getUser());
        e.setEmail(u.getEmail());
        e.setPassword(u.getPassword());
        e.setNombre(u.getNombre());
        e.setApellidos(u.getApellidos());
        e.setAvatar(u.getAvatar());
        e.setCuentaVerificada(u.isCuentaVerificada());
        e.setTwoFactorEnabled(u.isTwoFactorEnabled());
        e.setTwoFactorMethod(u.getTwoFactorMethod());
        e.setFechaRegistro(u.getFechaRegistro());

        // Añadimos los datos específicos que vienen del frontend
        if (datosEmpresa.containsKey("cif"))
            e.setCif(datosEmpresa.get("cif"));
        if (datosEmpresa.containsKey("web"))
            e.setWeb(datosEmpresa.get("web"));
        // if (datosEmpresa.containsKey("nombreComercial"))
        // e.setNombreComercial(datosEmpresa.get("nombreComercial"));

        // Eliminamos el usuario y guardamos la empresa de forma atómica
        actorRepository.delete(u);
        actorRepository.save(e);
    }
}