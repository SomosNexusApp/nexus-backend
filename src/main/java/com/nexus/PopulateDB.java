package com.nexus;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nexus.entity.Admin;
import com.nexus.entity.Empresa;
import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Oferta;
import com.nexus.entity.Producto;
import com.nexus.entity.TipoOferta;
import com.nexus.entity.Usuario;
import com.nexus.repository.AdminRepository;
import com.nexus.repository.EmpresaRepository;
import com.nexus.repository.OfertaRepository;
import com.nexus.repository.ProductoRepository;
import com.nexus.repository.UsuarioRepository;

@Component
public class PopulateDB {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void popular() {
        // Evitamos duplicar datos si ya existen admins
        if (adminRepository.count() > 0) {
            System.out.println("ℹ️ La base de datos ya contiene datos. Se omite el PopulateDB.");
            return;
        }

        System.out.println("🌱 Ejecutando PopulateDB...");
        
        // --- 1. CREAR ADMIN ---
        Admin admin = new Admin();
        admin.setUser("admin");
        admin.setEmail("admin@nexus.com");
        admin.setPassword(passwordEncoder.encode("Admin123!")); 
        admin.setFechaRegistro(LocalDateTime.now());
        admin.setNivelAcceso(1);
        adminRepository.save(admin);
        System.out.println("✅ Admin creado: admin / Admin123!");

        // --- 2. CREAR USUARIO (Alice) ---
        Usuario usuario = new Usuario();
        usuario.setUser("alice_doe");
        usuario.setEmail("alice@gmail.com");
        usuario.setPassword(passwordEncoder.encode("Usuario123!"));
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setTelefono("+34 600 123 456");
        usuario.setBiografia("Fotógrafa y coleccionista de tecnología.");
        usuario.setUbicacion("Madrid, España");
        usuario.setEsVerificado(true);
        usuario.setReputacion(5);
        usuario.setFotoPerfil("https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80");
        
        usuario = usuarioRepository.save(usuario);
        System.out.println("✅ Usuario creado: alice_doe / Usuario123!");

        // --- 3. CREAR EMPRESA (Tech Solutions) ---
        Empresa empresa = new Empresa();
        empresa.setUser("tech_solutions");
        empresa.setEmail("contacto@techsolutions.com");
        empresa.setPassword(passwordEncoder.encode("Empresa123!"));
        empresa.setFechaRegistro(LocalDateTime.now());
        empresa.setCif("B12345678");
        
        empresa = empresaRepository.save(empresa);
        System.out.println("✅ Empresa creada: tech_solutions / Empresa123!");

        // --- 4. CREAR PRODUCTOS (De Alice) ---
        Producto p1 = new Producto();
        p1.setTitulo("Cámara Canon AE-1");
        p1.setDescripcion("Cámara analógica clásica en perfecto estado. Incluye lente 50mm.");
        p1.setPrecio(250.00);
        p1.setTipoOferta(TipoOferta.VENTA);
        p1.setEstadoProducto(EstadoProducto.DISPONIBLE);
        p1.setPublicador(usuario);
        
        Producto p2 = new Producto();
        p2.setTitulo("MacBook Pro M1");
        p2.setDescripcion("Usado solo 6 meses. Color gris espacial. 16GB RAM.");
        p2.setPrecio(1100.50);
        p2.setTipoOferta(TipoOferta.VENTA);
        p2.setEstadoProducto(EstadoProducto.DISPONIBLE);
        p2.setPublicador(usuario);

        Producto p3 = new Producto();
        p3.setTitulo("Clases de Spring Boot");
        p3.setDescripcion("Te enseño a crear APIs REST seguras.");
        p3.setPrecio(20.00);
        p3.setTipoOferta(TipoOferta.INTERCAMBIO); 
        p3.setEstadoProducto(EstadoProducto.DISPONIBLE);
        p3.setPublicador(usuario);

        productoRepository.saveAll(List.of(p1, p2, p3));
        System.out.println("✅ 3 Productos creados.");

        // --- 5. CREAR OFERTAS (Links externos) ---
        Oferta of1 = new Oferta();
        of1.setTienda("Nike Store");
        of1.setPrecioOriginal(120.00);
        of1.setPrecioOferta(85.00);
        of1.setFechaExpiracion(LocalDateTime.now().plusDays(7));
        of1.setActor(empresa); 
        of1.setUrlOferta("https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80");
        
        Oferta of2 = new Oferta();
        of2.setTienda("Amazon");
        of2.setPrecioOriginal(50.00);
        of2.setPrecioOferta(25.00);
        of2.setFechaExpiracion(LocalDateTime.now().plusDays(2));
        of2.setActor(usuario);
        of2.setUrlOferta("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80");

        ofertaRepository.saveAll(List.of(of1, of2));
        System.out.println("✅ 2 Ofertas creadas.");
    }
}