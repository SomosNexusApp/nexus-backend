package com.nexus.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nexus.entity.Actor;
import com.nexus.entity.Admin;
import com.nexus.entity.Empresa;
import com.nexus.entity.Usuario;
import com.nexus.repository.UsuarioRepository;
import com.nexus.repository.ActorRepository;

// Servicio principal de usuarios. Implementa UserDetailsService para que Spring Security
// sepa como cargar un usuario por su nombre (lo necesita para el login normal)
@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    // actorRepository lo usamos porque Actor es la clase padre de Usuario, Empresa y Admin
    private ActorRepository actorRepository;

    @Autowired
    // el passwordEncoder se encarga de hashear las contraseñas (bcrypt)
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // el EntityManager lo necesitamos para hacer queries nativas (SQL directo)
    // especialmente en convertirAEmpresa, donde hacemos cosas que JPA no puede hacer bien
    @PersistenceContext
    private EntityManager entityManager;

    // guardamos los codigos de verificacion y los datos de registro en memoria porque son temporales
    // ojo: esto se pierde si el servidor se reinicia, pero para una verificacion de email va bien
    private final Map<String, String> verificationCodes = new HashMap<>();
    private final Map<String, Usuario> pendingRegistrations = new HashMap<>();

    // metodos basicos CRUD — nada especial aqui
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Integer id) {
        return usuarioRepository.findById(id);
    }

    public Usuario save(Usuario u) {
        return usuarioRepository.save(u);
    }

    public void delete(Integer id) {
        usuarioRepository.deleteById(id);
    }

    // Spring Security llama a este metodo cuando alguien intenta hacer login.
    // primero buscamos por username y si no encuentra, por email (para que se pueda
    // hacer login con cualquiera de los dos)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Actor actor = actorRepository.findByUsername(username)
                .or(() -> actorRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String rol = obtenerRol(actor);

        // creamos el UserDetails que necesita Spring con: usuario, contraseña hasheada y rol
        return new org.springframework.security.core.userdetails.User(
                actor.getUser(),
                actor.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol))
        );
    }

    // determina el rol del actor mirando de qué clase es
    // el orden importa: primero Admin, luego Empresa, si no es ninguno -> USUARIO
    public String obtenerRol(Actor actor) {
        if (actor instanceof Admin) return "ADMIN";
        if (actor instanceof Empresa) return "EMPRESA";
        return "USUARIO";
    }

    public Usuario registrarUsuario(Usuario u) {
        // comprobamos que el username y el email no estén ya ocupados en la base de datos (usuarios reales)
        if (actorRepository.findByUsername(u.getUser()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }
        if (actorRepository.findByEmail(u.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso.");
        }

        // hasheamos la contraseña antes de guardarla temporalmente
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        u.setCuentaVerificada(false);
        u.setFechaRegistro(LocalDateTime.now());
        
        // guardamos los datos del usuario en el mapa temporal (no en la base de datos)
        pendingRegistrations.put(u.getEmail(), u);

        // generamos un codigo de 6 digitos y lo guardamos en memoria (mapa)
        String code = String.format("%06d", new Random().nextInt(999999));
        verificationCodes.put(u.getEmail(), code);
        emailService.enviarVerificacion(u.getEmail(), u.getUser(), code);

        // devolvemos el objeto usuario (aun no tiene ID de base de datos)
        return u;
    }

    @Transactional
    public Usuario verificarCuentaCompleto(String email, String codigo) {
        // buscamos el codigo que guardamos cuando se registró
        String storedCode = verificationCodes.get(email);
        if (storedCode != null && storedCode.equals(codigo)) {
            Usuario u = pendingRegistrations.get(email);
            if (u != null) {
                // todo ok: marcamos la cuenta como verificada, la guardamos en la base de datos
                u.setCuentaVerificada(true);
                Usuario saved = usuarioRepository.save(u);
                
                // borramos los datos temporales
                verificationCodes.remove(email);
                pendingRegistrations.remove(email);
                
                return saved;
            }
        }
        return null; // codigo incorrecto o registro expirado/no encontrado
    }

    @Transactional
    public boolean verificarCuenta(String email, String codigo) {
        // Este metodo se mantiene por compatibilidad si se usa en otros sitios, 
        // pero redirigimos a la nueva lógica si hay un registro pendiente.
        if (pendingRegistrations.containsKey(email)) {
            return verificarCuentaCompleto(email, codigo) != null;
        }
        
        // Lógica antigua para usuarios que ya existen (ej: cambios de email o 2FA)
        String storedCode = verificationCodes.get(email);
        if (storedCode != null && storedCode.equals(codigo)) {
            Usuario u = usuarioRepository.findByEmail(email).orElse(null);
            if (u != null) {
                u.setCuentaVerificada(true);
                usuarioRepository.save(u);
                verificationCodes.remove(email);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public Map<String, Object> ingresarConGoogle(String token) {
        // NOTA: esto es una implementación de prueba/simulada.
        // En producción habria que validar el token con la API de Google
        // y sacar el googleId y email reales del token JWT de Google.
        String googleId = "google_" + token.length(); 
        String email = "googleuser@" + googleId + ".com"; 
        
        Optional<Usuario> existing = usuarioRepository.findByGoogleId(googleId);
        boolean esNuevo = existing.isEmpty(); // para saber si hay que mostrar el onboarding
        
        // si ya existe el usuario con ese googleId lo devolvemos, sino lo creamos
        Usuario u = existing.orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setUser("user_" + googleId);
            nuevo.setEmail(email);
            // ponemos una contraseña dummy porque los usuarios de Google no tienen contraseña local
            nuevo.setPassword(passwordEncoder.encode("google-oauth-dummy"));
            nuevo.setGoogleId(googleId);
            nuevo.setCuentaVerificada(true); // el email ya está verificado por Google
            return usuarioRepository.save(nuevo);
        });

        // devolvemos el actor y si es nuevo para que el controlador decida
        // si tiene que mostrar el onboarding o no
        Map<String, Object> result = new HashMap<>();
        result.put("actor", u);
        result.put("esNuevo", esNuevo);
        return result;
    }

    // actualiza las preferencias de privacidad del usuario
    // usamos getOrDefault para no romper si el frontend no manda todos los campos
    @Transactional
    public void updatePrivacidad(Integer userId, Map<String, Boolean> config) {
        Usuario u = usuarioRepository.findById(userId).orElseThrow();
        u.setMostrarUbicacion(config.getOrDefault("mostrarUbicacion", true));
        u.setMostrarTelefono(config.getOrDefault("mostrarTelefono", false));
        u.setPermitirMensajesDesconocidos(config.getOrDefault("permitirMensajesDesconocidos", true));
        usuarioRepository.save(u);
    }

    // guarda que tipo de avatar quiere usar el usuario (GOOGLE, INITIALS, CUSTOM)
    @Transactional
    public void actualizarAvatarChoice(Integer actorId, String choice) {
        Actor actor = actorRepository.findById(actorId).orElseThrow();
        actor.setAvatarSource(choice);
        actorRepository.save(actor);
    }

    // marca el onboarding como completado e incrementa la jwtVersion
    // al subir jwtVersion se invalidan los tokens anteriores, forzando al cliente
    // a hacer login de nuevo (o a refrescar su token) para obtener uno actualizado
    @Transactional
    public void completeOnboarding(Integer actorId) {
        Actor actor = actorRepository.findById(actorId).orElseThrow();
        actor.setOnboardingCompletado(true);
        actor.setJwtVersion(actor.getJwtVersion() + 1);
        actorRepository.save(actor);
    }

    // Convierte un usuario personal en empresa. Esto es el metodo mas complicado del servicio.
    // Usamos SQL nativo porque JPA no sabe lidiar bien con la herencia JOINED cuando
    // hay que cambiar el tipo de entidad de una fila ya existente.
    // El actor ya existe en la tabla 'actor', solo hay que cambiar su subtipo (de usuario a empresa)
    @Transactional
    public void convertirAEmpresa(Integer actorId, Map<String, String> datosEmpresa) {
        String cif = datosEmpresa.get("cif");
        String nombreComercial = datosEmpresa.get("nombreComercial");
        String web = datosEmpresa.get("web");
        String desc = datosEmpresa.getOrDefault("descripcion", "");

        // limpiamos la cache de JPA para evitar que use datos obsoletos
        entityManager.flush();
        entityManager.clear();

        // eliminamos registros que pueden generar conflicto de clave foranea
        // antes de borrar la fila de usuario. Si fallan no pasa nada (puede que no existan)
        try {
            entityManager.createNativeQuery("DELETE FROM favorito WHERE actor_id = :id").setParameter("id", actorId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM bloqueo WHERE bloqueador_id = :id OR bloqueado_id = :id").setParameter("id", actorId).executeUpdate();
        } catch (Exception e) {}

        // borramos la fila de la tabla usuario (subtipo antiguo)
        entityManager.createNativeQuery("DELETE FROM usuario WHERE actor_id = :id")
                .setParameter("id", actorId)
                .executeUpdate();

        // comprobamos si ya existe una fila de empresa para este actor
        // (puede pasar si se intento la conversion antes y fallo a medias)
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM empresa WHERE actor_id = :id")
                .setParameter("id", actorId)
                .getSingleResult();

        if (count.intValue() == 0) {
            // primera vez: insertamos la fila de empresa
            entityManager
                    .createNativeQuery(
                            "INSERT INTO empresa (actor_id, cif, nombre_comercial, descripcion, web, verificada) " +
                            "VALUES (:id, :cif, :nom, :desc, :web, false)")
                    .setParameter("id", actorId)
                    .setParameter("cif", cif)
                    .setParameter("nom", nombreComercial)
                    .setParameter("desc", desc)
                    .setParameter("web", web)
                    .executeUpdate();
        } else {
            // ya existia: actualizamos los datos de empresa
            entityManager
                    .createNativeQuery(
                            "UPDATE empresa SET cif = :cif, nombre_comercial = :nom, descripcion = :desc, web = :web WHERE actor_id = :id")
                    .setParameter("id", actorId)
                    .setParameter("cif", cif)
                    .setParameter("nom", nombreComercial)
                    .setParameter("desc", desc)
                    .setParameter("web", web)
                    .executeUpdate();
        }
        // limpiamos el cache de nuevo para que el entityManager no tenga estado viejo
        entityManager.clear();
    }

    // lo contrario de convertirAEmpresa: vuelve al tipo usuario personal
    // misma logica de SQL nativo para evitar problemas con la herencia JPA
    @Transactional
    public void convertirAUsuarioPersonal(Integer actorId) {
        entityManager.flush();
        entityManager.clear();

        // eliminamos la fila de empresa
        entityManager.createNativeQuery("DELETE FROM empresa WHERE actor_id = :id")
                .setParameter("id", actorId)
                .executeUpdate();

        // creamos la fila de usuario con todos los valores por defecto.
        // los terminos los ponemos como aceptados porque ya los acepto cuando se registro
        // y los contadores de reputacion/ventas empiezan en 0
        entityManager.createNativeQuery(
                "INSERT INTO usuario (actor_id, cuenta_privada, terminos_aceptados, newsletter_suscrito, " +
                        "reputacion, total_ventas, es_verificado, perfil_publico, mostrar_telefono, mostrar_ubicacion, " +
                        "permitir_mensajes_desconocidos, notif_nuevos_mensajes, notif_nueva_compra, notif_valoracion, " +
                        "notif_ofertas, notif_envios, notif_novedades, tipo_cuenta) " +
                        "VALUES (:id, false, true, false, 0.0, 0, false, true, false, true, true, true, true, true, true, true, true, 'PERSONAL')")
                .setParameter("id", actorId)
                .executeUpdate();

        entityManager.clear();
    }
}