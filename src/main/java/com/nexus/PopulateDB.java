package com.nexus;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nexus.entity.*;
import com.nexus.repository.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PopulateDB — Datos de prueba completos para Nexus (Wallapop + Chollometro).
 *
 * REGLAS CRITICAS (no cambiar):
 *   - Admin/Empresa se guardan con actorRepository, NUNCA usuarioRepository
 *   - setCategoria() recibe objeto Categoria, NUNCA String
 *   - SparkVoto(actor, oferta, true)  → Spark (+1)
 *   - SparkVoto(actor, oferta, false) → Drip  (-1)
 *   - La clase es idempotente: comprueba actorRepository.count() > 0 antes de insertar
 *
 * Entidades cubiertas:
 *   Admin, Empresa, Usuario, Categoria, Producto, Vehiculo, Oferta,
 *   SparkVoto, Comentario, Favorito, Bloqueo, Mensaje, ChatMensaje,
 *   Compra, Envio, Devolucion, Valoracion, Reporte, Contrato,
 *   NewsletterSuscripcion, NotificacionInApp
 */
@Component
public class PopulateDB implements ApplicationListener<ContextRefreshedEvent> {

    // ── Repositories ─────────────────────────────────────────────────────────
    @Autowired private ActorRepository            actorRepository;
    @Autowired private UsuarioRepository          usuarioRepository;
    @Autowired private ProductoRepository         productoRepository;
    @Autowired private VehiculoRepository         vehiculoRepository;
    @Autowired private OfertaRepository           ofertaRepository;
    @Autowired private SparkVotoRepository        sparkVotoRepository;
    @Autowired private CategoriaRepository        categoriaRepository;
    @Autowired private ComentarioRepository       comentarioRepository;
    @Autowired private FavoritoRepository         favoritoRepository;
    @Autowired private BloqueoRepository          bloqueoRepository;
    @Autowired private MensajeRepository          mensajeRepository;
    @Autowired private ChatMensajeRepository      chatMensajeRepository;
    @Autowired private CompraRepository           compraRepository;
    @Autowired private EnvioRepository            envioRepository;
    @Autowired private DevolucionRepository       devolucionRepository;
    @Autowired private ValoracionRepository       valoracionRepository;
    @Autowired private ReporteRepository          reporteRepository;
    @Autowired private ContratoRepository         contratoRepository;
    @Autowired private EmpresaRepository          empresaRepository;
    @Autowired private NewsletterRepository       newsletterRepository;
    @Autowired private NotificacionRepository     notificacionRepository;
    @Autowired private PasswordEncoder            passwordEncoder;

    private boolean done = false;

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (done || actorRepository.count() > 0) { done = true; return; }
        done = true;

        System.out.println("=== PopulateDB: iniciando inserción de datos ===");

        // ── 1. CATEGORÍAS ─────────────────────────────────────────────────────
        Categoria catElectronica  = cat("Electrónica",     "electronica",     "devices",          "#1565C0", null, 1);
        Categoria catRopa         = cat("Ropa",             "ropa",            "checkroom",         "#6A1B9A", null, 2);
        Categoria catHogar        = cat("Hogar",            "hogar",           "home",              "#2E7D32", null, 3);
        Categoria catVehiculos    = cat("Vehículos",        "vehiculos",       "directions_car",    "#1976D2", null, 4);
        Categoria catInformatica  = cat("Informática",      "informatica",     "laptop",            "#00838F", null, 5);
        Categoria catVideojuegos  = cat("Videojuegos",      "videojuegos",     "sports_esports",    "#7B1FA2", null, 6);
        Categoria catDeportes     = cat("Deportes",         "deportes",        "sports",            "#E65100", null, 7);
        Categoria catLibros       = cat("Libros",           "libros",          "menu_book",         "#4E342E", null, 8);
        Categoria catJuguetes     = cat("Juguetes",         "juguetes",        "toys",              "#F57F17", null, 9);
        Categoria catInmuebles    = cat("Inmuebles",        "inmuebles",       "apartment",         "#37474F", null, 10);

        // Sub-categorías
        Categoria catMoviles      = cat("Móviles",          "moviles",         "smartphone",        "#1565C0", catElectronica, 1);
        Categoria catAudio        = cat("Audio",            "audio",           "headphones",        "#1565C0", catElectronica, 2);
        Categoria catTV           = cat("TV y Vídeo",       "tv-video",        "tv",                "#1565C0", catElectronica, 3);
        Categoria catCamaras      = cat("Cámaras",          "camaras",         "camera_alt",        "#1565C0", catElectronica, 4);
        Categoria catPCs          = cat("PCs y Portátiles", "pcs",             "computer",          "#00838F", catInformatica, 1);
        Categoria catSoftware     = cat("Software",         "software",        "code",              "#00838F", catInformatica, 2);
        Categoria catComponentes  = cat("Componentes",      "componentes-pc",  "memory",            "#00838F", catInformatica, 3);
        Categoria catCoches       = cat("Coches",           "coches",          "directions_car",    "#1976D2", catVehiculos, 1);
        Categoria catMotos        = cat("Motos",            "motos",           "two_wheeler",       "#1976D2", catVehiculos, 2);
        Categoria catRopaHombre   = cat("Ropa Hombre",      "ropa-hombre",     "man",               "#6A1B9A", catRopa, 1);
        Categoria catRopaMujer    = cat("Ropa Mujer",       "ropa-mujer",      "woman",             "#6A1B9A", catRopa, 2);
        Categoria catZapatillas   = cat("Zapatillas",       "zapatillas",      "directions_run",    "#6A1B9A", catRopa, 3);
        Categoria catConsolaJuego = cat("Consolas",         "consolas",        "videogame_asset",   "#7B1FA2", catVideojuegos, 1);
        Categoria catMuebles      = cat("Muebles",          "muebles",         "chair",             "#2E7D32", catHogar, 1);
        Categoria catElectrodomest= cat("Electrodomésticos","electrodomesticos","kitchen",           "#2E7D32", catHogar, 2);

        // ── 2. ADMINS ─────────────────────────────────────────────────────────
        Admin admin1 = new Admin();
        admin1.setUser("admin");
        admin1.setEmail("admin@nexus.test");
        admin1.setPassword(passwordEncoder.encode("Admin2026!"));
        admin1.setCuentaVerificada(true);
        admin1.setNivelAcceso(3);
        actorRepository.save(admin1);

        Admin admin2 = new Admin();
        admin2.setUser("moderador");
        admin2.setEmail("moderador@nexus.test");
        admin2.setPassword(passwordEncoder.encode("Mod2026!"));
        admin2.setCuentaVerificada(true);
        admin2.setNivelAcceso(2);
        actorRepository.save(admin2);

        // ── 3. EMPRESAS ───────────────────────────────────────────────────────
        Empresa techStore = new Empresa();
        techStore.setUser("techstore_oficial");
        techStore.setEmail("info@techstore.es");
        techStore.setPassword(passwordEncoder.encode("TechStore2026!"));
        techStore.setCuentaVerificada(true);
        techStore.setCif("B12345678");
        techStore.setNombreComercial("TechStore España");
        techStore.setDescripcion("Tienda oficial de tecnología con los mejores precios garantizados.");
        techStore.setWeb("https://www.techstore.es");
        techStore.setTelefono("900123456");
        techStore.setLogo("https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=200");
        techStore.setVerificada(true);
        actorRepository.save(techStore);

        Empresa modaFashion = new Empresa();
        modaFashion.setUser("modafashion_es");
        modaFashion.setEmail("contacto@modafashion.es");
        modaFashion.setPassword(passwordEncoder.encode("ModaFashion2026!"));
        modaFashion.setCuentaVerificada(true);
        modaFashion.setCif("B87654321");
        modaFashion.setNombreComercial("Moda Fashion");
        modaFashion.setDescripcion("Outlet de moda con descuentos de hasta 70% en marcas premium.");
        modaFashion.setWeb("https://www.modafashion.es");
        modaFashion.setTelefono("900654321");
        modaFashion.setVerificada(false);
        actorRepository.save(modaFashion);

        // ── 4. CONTRATOS ──────────────────────────────────────────────────────
        Contrato contrato1 = new Contrato();
        contrato1.setTipoContrato(TipoContrato.BANNER);
        contrato1.setEmpresa(techStore);
        contrato1.setFecha(LocalDateTime.now().minusMonths(2));
        contratoRepository.save(contrato1);

        Contrato contrato2 = new Contrato();
        contrato2.setTipoContrato(TipoContrato.PUBLICACION);
        contrato2.setEmpresa(modaFashion);
        contrato2.setFecha(LocalDateTime.now().minusWeeks(3));
        contratoRepository.save(contrato2);

        // ── 5. USUARIOS ───────────────────────────────────────────────────────
        Usuario carlos   = usuario("carlos_vendedor",  "carlos@nexus.test",   "Madrid, Chamberí",   "Vendedor de electrónica de confianza. +200 ventas.", 4.8, 212, true);
        Usuario maria    = usuario("maria_chollos",    "maria@nexus.test",    "Barcelona, Gràcia",  "Cazadora de chollos compulsiva. Siempre encuentro lo mejor.", 4.5, 87, true);
        Usuario pedro    = usuario("pedro_gamer",      "pedro@nexus.test",    "Valencia, Ruzafa",   "Gamer hardcore. Vendo y compro videojuegos y hardware.", 4.9, 310, true);
        Usuario lucia    = usuario("lucia_moda",       "lucia@nexus.test",    "Sevilla, Triana",    "Amante de la moda sostenible. Doy segunda vida a la ropa.", 4.6, 145, false);
        Usuario miguel   = usuario("miguel_motor",     "miguel@nexus.test",   "Bilbao, Casco Viejo","Mecánico aficionado. Compro y vendo vehículos y piezas.", 4.3, 56, true);
        Usuario sofia    = usuario("sofia_hogar",      "sofia@nexus.test",    "Madrid, Retiro",     "Interiorista. Renuevo muebles y electrodomésticos frecuentemente.", 4.7, 178, true);
        Usuario andres   = usuario("andres_libros",    "andres@nexus.test",   "Granada, Albaicín",  "Lector empedernido. Intercambio y vendo libros.", 4.2, 33, false);
        Usuario elena    = usuario("elena_deporte",    "elena@nexus.test",    "Zaragoza, Centro",   "Deportista. Vendo material deportivo que ya no uso.", 4.4, 61, true);

        // Dirección por defecto para Carlos
        DireccionEnvio dirCarlos = new DireccionEnvio();
        dirCarlos.setNombre("Carlos García");
        dirCarlos.setDireccion("Calle Fuencarral 42, 3ºA");
        dirCarlos.setCiudad("Madrid");
        dirCarlos.setCodigoPostal("28004");
        dirCarlos.setPais("España");
        dirCarlos.setTelefono("600111222");
        carlos.setDireccionPorDefecto(dirCarlos);
        usuarioRepository.save(carlos);

        // Dirección por defecto para María
        DireccionEnvio dirMaria = new DireccionEnvio();
        dirMaria.setNombre("María López");
        dirMaria.setDireccion("Carrer de Gràcia 15, 1º1ª");
        dirMaria.setCiudad("Barcelona");
        dirMaria.setCodigoPostal("08012");
        dirMaria.setPais("España");
        dirMaria.setTelefono("600333444");
        maria.setDireccionPorDefecto(dirMaria);
        usuarioRepository.save(maria);

        // ── 6. PRODUCTOS ──────────────────────────────────────────────────────

        // Electrónica - Móviles
        Producto iphone14 = producto(
            "iPhone 14 Pro 128GB - Azul Profundo",
            "iPhone 14 Pro en perfecto estado. Sin rayadas, batería al 97%. Incluye caja original, cargador y funda de piel. Comprado en noviembre 2022.",
            750.0, TipoOferta.VENTA, carlos, catMoviles, "Apple", "iPhone 14 Pro",
            CondicionProducto.COMO_NUEVO, true, 5.0, false, "Madrid",
            "https://images.unsplash.com/photo-1678685888221-cda773a3dcdb?w=800");

        Producto samsungS23 = producto(
            "Samsung Galaxy S23 Ultra 256GB",
            "Samsung S23 Ultra con S Pen. Color Phantom Black. 12GB RAM. Batería 89%. Siempre con funda. Sin golpes ni arañazos.",
            680.0, TipoOferta.VENTA, pedro, catMoviles, "Samsung", "Galaxy S23 Ultra",
            CondicionProducto.MUY_BUEN_ESTADO, true, 6.0, true, "Valencia",
            "https://images.unsplash.com/photo-1675272979687-aad85d4be5c7?w=800");

        Producto pixelPhone = producto(
            "Google Pixel 7 Pro 128GB",
            "Pixel 7 Pro con 12GB RAM. Cámara increíble. Actualizaciones garantizadas hasta 2026. Desbloqueo facial y fingerprint.",
            420.0, TipoOferta.VENTA, maria, catMoviles, "Google", "Pixel 7 Pro",
            CondicionProducto.BUEN_ESTADO, true, 4.99, true, "Barcelona",
            "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800");

        // Electrónica - Audio
        Producto sonyWH = producto(
            "Sony WH-1000XM5 - Cancelación ruido activa",
            "Auriculares premium Sony WH-1000XM5. ANC líder del mercado. 30h autonomía. Bluetooth 5.2. Como nuevos, usados 3 meses.",
            220.0, TipoOferta.VENTA, sofia, catAudio, "Sony", "WH-1000XM5",
            CondicionProducto.COMO_NUEVO, true, 6.0, false, "Madrid",
            "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=800");

        Producto airpodsMax = producto(
            "AirPods Max Plata - Caja original",
            "AirPods Max en color plata. Batería al 91%. Incluye estuche Smart Case. Sonido Hi-Fi extraordinario.",
            320.0, TipoOferta.VENTA, carlos, catAudio, "Apple", "AirPods Max",
            CondicionProducto.MUY_BUEN_ESTADO, true, 7.0, false, "Madrid",
            "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800");

        // Electrónica - TV
        Producto lgOled = producto(
            "LG OLED C2 55\" 4K 120Hz HDMI 2.1",
            "TV LG OLED C2 55 pulgadas. Panel OLED Evo, 4K, 120Hz, HDMI 2.1, G-Sync y FreeSync. Perfecto para gaming. 14 meses de uso.",
            820.0, TipoOferta.VENTA, carlos, catTV, "LG", "OLED55C2",
            CondicionProducto.MUY_BUEN_ESTADO, false, 0.0, true, "Madrid",
            "https://images.unsplash.com/photo-1593359677879-a4bb92f4834c?w=800");

        // Informática - PCs
        Producto macbookPro = producto(
            "MacBook Pro M2 14\" 16GB 512GB SSD",
            "MacBook Pro con chip Apple M2 Pro. 16GB RAM, 512GB SSD. Pantalla Liquid Retina XDR. Batería al 94%. Incluye cargador MagSafe.",
            1650.0, TipoOferta.VENTA, pedro, catPCs, "Apple", "MacBook Pro M2",
            CondicionProducto.COMO_NUEVO, true, 15.0, false, "Valencia",
            "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800");

        Producto lenovoThinkpad = producto(
            "Lenovo ThinkPad X1 Carbon Gen 10",
            "ThinkPad X1 Carbon 14\". Intel i7-1260P, 16GB LPDDR5, 512GB SSD NVMe. 1.12kg. Teclado retroiluminado. Excelente para trabajo.",
            950.0, TipoOferta.VENTA, maria, catPCs, "Lenovo", "ThinkPad X1 Carbon Gen 10",
            CondicionProducto.BUEN_ESTADO, true, 12.0, true, "Barcelona",
            "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800");

        // Informática - Componentes
        Producto rtx4070 = producto(
            "NVIDIA RTX 4070 Ti SUPER - ASUS TUF",
            "Tarjeta gráfica ASUS TUF Gaming RTX 4070 Ti SUPER OC. 16GB GDDR6X. Solo 8 meses de uso. Nunca para minería.",
            650.0, TipoOferta.VENTA, pedro, catComponentes, "ASUS", "TUF Gaming RTX 4070 Ti SUPER",
            CondicionProducto.COMO_NUEVO, false, 0.0, true, "Valencia",
            "https://images.unsplash.com/photo-1591489378430-ef2f4c626b35?w=800");

        Producto procesadorRyzen = producto(
            "AMD Ryzen 9 7950X - Caja original sin activar",
            "Procesador AMD Ryzen 9 7950X 16 núcleos / 32 hilos. 5.7GHz boost. 64MB caché L3. Sin usar, en caja sellada.",
            580.0, TipoOferta.VENTA, pedro, catComponentes, "AMD", "Ryzen 9 7950X",
            CondicionProducto.NUEVO, true, 5.0, false, "Valencia",
            "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=800");

        // Videojuegos
        Producto ps5Console = producto(
            "PlayStation 5 Edición Digital + 3 juegos",
            "PS5 Digital Edition en perfecto estado. Incluye: Spider-Man 2, Horizon FW y Returnal. Mando original DualSense sin stick drift.",
            420.0, TipoOferta.VENTA, carlos, catConsolaJuego, "Sony", "PlayStation 5 Digital",
            CondicionProducto.COMO_NUEVO, false, 0.0, true, "Madrid",
            "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=800");

        Producto nintendoSwitch = producto(
            "Nintendo Switch OLED Blanca + 5 juegos",
            "Nintendo Switch OLED blanca. Batería perfecta. Juegos: Zelda Tears of the Kingdom, Mario Kart 8, Animal Crossing, Splatoon 3 y Pokemon Violet.",
            290.0, TipoOferta.VENTA, maria, catConsolaJuego, "Nintendo", "Switch OLED",
            CondicionProducto.MUY_BUEN_ESTADO, true, 8.0, false, "Barcelona",
            "https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=800");

        Producto zelda = producto(
            "Zelda: Tears of the Kingdom - Nintendo Switch",
            "Juego físico para Nintendo Switch. Completo con caja y manual. Solo terminado una vez. Perfecto estado.",
            45.0, TipoOferta.VENTA, andres, catVideojuegos, "Nintendo", "Zelda TotK",
            CondicionProducto.MUY_BUEN_ESTADO, true, 3.99, true, "Granada",
            "https://images.unsplash.com/photo-1600456899121-68eda5705257?w=800");

        // Ropa
        Producto nikeSneakers = producto(
            "Nike Air Max 90 Talla 42 - Blancas",
            "Nike Air Max 90 talla 42. Usadas 3 veces para probarlas. Sin manchas ni deformaciones. Caja original incluida.",
            90.0, TipoOferta.VENTA, lucia, catZapatillas, "Nike", "Air Max 90",
            CondicionProducto.COMO_NUEVO, true, 4.99, false, "Sevilla",
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800");

        Producto converseClassic = producto(
            "Converse Chuck Taylor All Star 41 Negras",
            "Converse clásicas negras talla 41. Usadas con cuidado durante 6 meses. Sin roturas. Limpiadas y listas.",
            35.0, TipoOferta.VENTA, maria, catZapatillas, "Converse", "Chuck Taylor All Star",
            CondicionProducto.BUEN_ESTADO, true, 3.99, true, "Barcelona",
            "https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=800");

        Producto canadaGoose = producto(
            "Canada Goose Expedition Parka L - Negro",
            "Parka Canada Goose Expedition talla L. Plumón de ganso 625 Fill Power. Certificado REAL FUR. Perfecta para inviernos extremos.",
            650.0, TipoOferta.VENTA, carlos, catRopaHombre, "Canada Goose", "Expedition Parka",
            CondicionProducto.MUY_BUEN_ESTADO, true, 12.0, true, "Madrid",
            "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=800");

        Producto zaraVestido = producto(
            "Vestido Zara satén midi azul marino - S",
            "Vestido midi de satén Zara talla S. Estrenado una sola vez para una boda. Sin manchas ni descosidos.",
            28.0, TipoOferta.DONACION, lucia, catRopaMujer, "Zara", "Vestido Satén Midi",
            CondicionProducto.COMO_NUEVO, true, 3.99, false, "Sevilla",
            "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800");

        // Hogar
        Producto roomba = producto(
            "iRobot Roomba j7+ con base de vaciado",
            "Roomba j7+ con base de vaciado automático. Mapeo por IA, esquiva obstáculos. 18 meses de uso. Incluye cargador y bolsas.",
            320.0, TipoOferta.VENTA, sofia, catElectrodomest, "iRobot", "Roomba j7+",
            CondicionProducto.BUEN_ESTADO, false, 0.0, true, "Madrid",
            "https://images.unsplash.com/photo-1589782431773-c7b9ad4c37b2?w=800");

        Producto ikeaEscritorio = producto(
            "Escritorio IKEA Bekant 160x80 blanco",
            "Mesa de escritorio IKEA Bekant 160x80cm color blanco. Estructura metálica. Sin arañazos relevantes. Desmontada y lista para recoger.",
            120.0, TipoOferta.VENTA, sofia, catMuebles, "IKEA", "Bekant",
            CondicionProducto.BUEN_ESTADO, false, 0.0, true, "Madrid",
            "https://images.unsplash.com/photo-1593640495253-23196b27a87f?w=800");

        // Deportes
        Producto bicicletaCarretera = producto(
            "Bicicleta carretera Trek Domane AL 3 T52",
            "Trek Domane AL 3 talla 52. Grupo Shimano 105 11v. Frenos hidráulicos. Ruedas Bontrager Paradigm. 2 años, perfecto estado.",
            890.0, TipoOferta.VENTA, elena, catDeportes, "Trek", "Domane AL 3",
            CondicionProducto.MUY_BUEN_ESTADO, false, 0.0, false, "Zaragoza",
            "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800");

        Producto raquetaTenis = producto(
            "Raqueta Wilson Blade 98 v8 16x19",
            "Wilson Blade 98 v8, 305g. Grip 3. Encordado Luxilon Alu Power. Usada 1 temporada. Sin golpes en el marco.",
            130.0, TipoOferta.VENTA, elena, catDeportes, "Wilson", "Blade 98 v8",
            CondicionProducto.BUEN_ESTADO, true, 5.0, true, "Zaragoza",
            "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800");

        // Libros
        Producto libroCleanCode = producto(
            "Clean Code - Robert C. Martin (Robert C. Martin)",
            "Libro 'Clean Code: A Handbook of Agile Software Craftsmanship'. Edición inglesa. Subrayado mínimo con lápiz. Excelente estado.",
            18.0, TipoOferta.VENTA, andres, catLibros, null, null,
            CondicionProducto.BUEN_ESTADO, true, 2.99, false, "Granada",
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800");

        Producto libroDune = producto(
            "DUNE - Frank Herbert (Edición Especial)",
            "Dune edición especial con ilustraciones. Tapa dura. Como nueva. Solo leída una vez.",
            22.0, TipoOferta.INTERCAMBIO, andres, catLibros, null, null,
            CondicionProducto.COMO_NUEVO, true, 3.0, false, "Granada",
            "https://images.unsplash.com/photo-1513001900722-370f803f498d?w=800");

        // Cámara
        Producto sonyA7IV = producto(
            "Sony Alpha a7 IV + Objetivo 28-70mm",
            "Cámara Sony A7 IV full-frame 33MP. 120fps en 4K. Solo 8.000 disparos. Incluye objetivo 28-70mm OSS y dos baterías.",
            2200.0, TipoOferta.VENTA, carlos, catCamaras, "Sony", "Alpha a7 IV",
            CondicionProducto.COMO_NUEVO, true, 20.0, false, "Madrid",
            "https://images.unsplash.com/photo-1606983340126-99ab4feaa64a?w=800");

        // Juguetes
        Producto legoBatman = producto(
            "LEGO Batman Batmóvil 42127 - Sin abrir",
            "Set LEGO Technic Batman Batmóvil 42127. 422 piezas. Caja sellada sin abrir. Comprado como regalo pero tenemos uno igual.",
            55.0, TipoOferta.VENTA, lucia, catJuguetes, "LEGO", "Batmóvil Technic 42127",
            CondicionProducto.NUEVO, true, 4.99, false, "Sevilla",
            "https://images.unsplash.com/photo-1600456899121-68eda5705257?w=800");

        // ── 7. VEHÍCULOS ──────────────────────────────────────────────────────
        Vehiculo bmw320d = vehiculo(
            "BMW 320d xDrive Touring 2021",
            "BMW Serie 3 Touring 320d xDrive 190CV. Color gris mineral. 62.000 km. Mantenimiento en BMW oficial. Extras: parking automático, HUD, asientos calefactados.",
            32500.0, TipoVehiculo.COCHE, miguel, catVehiculos, "BMW", "320d xDrive Touring",
            2021, 62000, "DIESEL", "AUTOMATICO", 190, 1995, "Gris Mineral",
            4, 5, "4520FKL", true, true, "Bilbao",
            "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800");

        Vehiculo hondaCBR = vehiculo(
            "Honda CBR600RR 2019 - Solo 12.000 km",
            "Honda CBR600RR Rojo/Negro. 120CV. 12.000 km reales. ITV reciente. Nunca caída. Escape Arrow. Revisiones en concesionario oficial Honda.",
            7200.0, TipoVehiculo.MOTO, miguel, catVehiculos, "Honda", "CBR600RR",
            2019, 12000, "GASOLINA", "MANUAL", 120, 599, "Rojo/Negro",
            null, 2, "9482MNK", true, false, "Bilbao",
            "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800");

        Vehiculo teslaModel3 = vehiculo(
            "Tesla Model 3 Long Range AWD 2022",
            "Tesla Model 3 Long Range AWD 358CV. Color blanco perlado. 41.000 km. Autonomía real 500km. Autopilot. Cargador tipo 2 incluido.",
            38900.0, TipoVehiculo.COCHE, carlos, catVehiculos, "Tesla", "Model 3 Long Range",
            2022, 41000, "ELECTRICO", "AUTOMATICO", 358, 0, "Blanco Perlado",
            4, 5, "3381ABC", true, true, "Madrid",
            "https://images.unsplash.com/photo-1560958089-b8a1929cea89?w=800");

        Vehiculo fordTransit = vehiculo(
            "Ford Transit Custom 310 L1 2.0 TDCI 2020",
            "Ford Transit Custom 310 L1 130CV. Blanco. 85.000 km. Ideal para empresa. Neumáticos nuevos. Revisiones al día.",
            18500.0, TipoVehiculo.FURGONETA, miguel, catVehiculos, "Ford", "Transit Custom 310 L1",
            2020, 85000, "DIESEL", "MANUAL", 130, 1996, "Blanco",
            null, 2, "6621PQR", true, false, "Bilbao",
            "https://images.unsplash.com/photo-1595787572888-b6553d8073b7?w=800");

        Vehiculo vespa = vehiculo(
            "Vespa GTS 300 Super 2023 - Gris Titanio",
            "Vespa GTS 300 Super 2023. Solo 2.800 km. Color gris titanio. ABS, ASR, pantalla TFT, luz LED. Como nueva.",
            5800.0, TipoVehiculo.SCOOTER, lucia, catVehiculos, "Vespa", "GTS 300 Super",
            2023, 2800, "GASOLINA", "AUTOMATICO", 25, 278, "Gris Titanio",
            null, 2, "7763STU", true, true, "Sevilla",
            "https://images.unsplash.com/photo-1609630875171-b1321377ee65?w=800");

        // ── 8. OFERTAS (Chollometro) ──────────────────────────────────────────
        Oferta ofertaAirpods = oferta(
            "AirPods Pro 2ª Gen USB-C - Mínimo histórico en Amazon",
            "Los mejores auriculares TWS de Apple con cancelación activa de ruido H2, modo transparencia adaptativo y audio espacial. Precio mínimo histórico registrado.",
            179.0, 279.0, "Amazon", "https://amazon.es/airpods-pro-2",
            catAudio, techStore, BadgeOferta.CHOLLAZO, -72,
            "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800",
            156, 8, 4800, 312);

        Oferta ofertaWindows = oferta(
            "Windows 11 Pro OEM por solo 9,99€ - Key digital",
            "Licencia OEM original de Windows 11 Pro. Activación inmediata. Compatible con upgrade desde Windows 10. Entrega en menos de 5 minutos.",
            9.99, 145.0, "Kinguin", "https://kinguin.net/windows-11-pro",
            catSoftware, techStore, BadgeOferta.CHOLLAZO, -48,
            "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800",
            289, 22, 9800, 756);

        Oferta ofertaPS5 = oferta(
            "PlayStation 5 Standard 699€ + 2 mandos - PcComponentes",
            "Bundle PS5 con disco + 2 mandos DualSense. Ideal para no quedarte sin consola estas navidades. Stock limitado.",
            699.0, 789.0, "PcComponentes", "https://pccomponentes.com/ps5",
            catConsolaJuego, carlos, BadgeOferta.DESTACADA, -36,
            "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=800",
            98, 12, 5600, 289);

        Oferta ofertaXiaomi = oferta(
            "Xiaomi Redmi Note 13 Pro+ 5G 256GB por 249€",
            "Xiaomi Redmi Note 13 Pro+ con Dimensity 7200 Ultra, cámara de 200MP, pantalla AMOLED 120Hz y carga de 120W. Una barbaridad por este precio.",
            249.0, 399.0, "MediaMarkt", "https://mediamarkt.es/xiaomi",
            catMoviles, maria, BadgeOferta.NUEVA, -2,
            "https://images.unsplash.com/photo-1591337676887-a217a6970a8a?w=800",
            67, 4, 2100, 134);

        Oferta ofertaPSPlus = oferta(
            "PS Plus Essential 12 meses por 39,99€ - Código digital",
            "Suscripción PlayStation Plus Essential 12 meses. Acceso a juegos mensuales gratuitos y multijugador online. Precio mínimo del año.",
            39.99, 71.99, "PlayStation Store", "https://store.playstation.com",
            catConsolaJuego, pedro, BadgeOferta.DESTACADA, -24,
            "https://images.unsplash.com/photo-1605647540924-852290f6b0d5?w=800",
            78, 3, 3200, 189);

        Oferta ofertaSamsungQLED = oferta(
            "Samsung QLED 55\" Q80C 4K 2024 por 499€",
            "Smart TV Samsung QLED 55 pulgadas Q80C. 120Hz, HDR10+, Quantum Processor. El mejor precio del mercado para este panel.",
            499.0, 799.0, "El Corte Inglés", "https://elcorteingles.es/samsung-q80c",
            catTV, sofia, BadgeOferta.PORCENTAJE, -18,
            "https://images.unsplash.com/photo-1593359677879-a4bb92f4834c?w=800",
            43, 5, 1890, 97);

        Oferta ofertaRoombaIRobot = oferta(
            "iRobot Roomba j9+ con base autovaciado por 299€",
            "Roomba j9+ con base de vaciado automático Clean Base. Mapeo 3D, evita obstáculos por IA. Precio mínimo histórico con -46% de descuento.",
            299.0, 549.0, "Amazon", "https://amazon.es/roomba-j9",
            catElectrodomest, modaFashion, BadgeOferta.CHOLLAZO, -12,
            "https://images.unsplash.com/photo-1589782431773-c7b9ad4c37b2?w=800",
            112, 6, 3400, 201);

        Oferta ofertaRTX4060 = oferta(
            "RTX 4060 Ti 8GB ASUS TUF por 349€ en PCComponentes",
            "Tarjeta gráfica ASUS TUF Gaming RTX 4060 Ti 8GB OC Edition. Oferta relámpago con stock limitado. Rinde al nivel de la 3080 Ti.",
            349.0, 449.0, "PcComponentes", "https://pccomponentes.com/rtx4060ti",
            catComponentes, techStore, BadgeOferta.EXPIRA_HOY,
            LocalDateTime.now().plusHours(8),
            "https://images.unsplash.com/photo-1591489378430-ef2f4c626b35?w=800",
            201, 9, 6700, 445);

        Oferta ofertaNike = oferta(
            "Nike Air Force 1 '07 por 54,99€ en Nike.com - 3 colores",
            "Nike Air Force 1 blancas, negras y rojas disponibles en todas las tallas. Código de descuento aplicado automáticamente.",
            54.99, 89.99, "Nike.com", "https://nike.com/air-force-1",
            catZapatillas, lucia, BadgeOferta.PORCENTAJE, -6,
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800",
            34, 2, 1560, 88);

        Oferta ofertaLego = oferta(
            "LEGO Icons 10281 Árbol Bonsái por 32,99€",
            "Set LEGO Icons Árbol Bonsái 878 piezas. Precio más bajo del año. Ideal para adultos. Envío gratis con Prime.",
            32.99, 54.99, "Amazon", "https://amazon.es/lego-bonsai",
            catJuguetes, maria, BadgeOferta.NUEVA, -1,
            "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?w=800",
            23, 1, 890, 45);

        // Oferta gratuita
        Oferta ofertaNetflixTrial = oferta(
            "Netflix Premium 3 meses GRATIS con Vodafone",
            "Clientes Vodafone One pueden activar Netflix Premium durante 3 meses completamente gratis. Solo para nuevas altas.",
            0.0, 41.97, "Vodafone", "https://vodafone.es/netflix-gratis",
            catVideojuegos, techStore, BadgeOferta.GRATUITA, -3,
            "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=800",
            445, 15, 12000, 1890);

        // ── 9. SPARK VOTOS ────────────────────────────────────────────────────
        sparkVotoRepository.save(new SparkVoto(maria,  ofertaAirpods, true));
        sparkVotoRepository.save(new SparkVoto(pedro,  ofertaAirpods, true));
        sparkVotoRepository.save(new SparkVoto(sofia,  ofertaAirpods, true));
        sparkVotoRepository.save(new SparkVoto(lucia,  ofertaAirpods, true));
        sparkVotoRepository.save(new SparkVoto(andres, ofertaAirpods, true));

        sparkVotoRepository.save(new SparkVoto(carlos, ofertaWindows, true));
        sparkVotoRepository.save(new SparkVoto(pedro,  ofertaWindows, true));
        sparkVotoRepository.save(new SparkVoto(elena,  ofertaWindows, true));
        sparkVotoRepository.save(new SparkVoto(miguel, ofertaWindows, true));
        sparkVotoRepository.save(new SparkVoto(sofia,  ofertaWindows, false)); // drip

        sparkVotoRepository.save(new SparkVoto(maria,  ofertaPS5,    true));
        sparkVotoRepository.save(new SparkVoto(andres, ofertaPS5,    true));
        sparkVotoRepository.save(new SparkVoto(lucia,  ofertaPS5,    false)); // drip

        sparkVotoRepository.save(new SparkVoto(carlos, ofertaXiaomi, true));
        sparkVotoRepository.save(new SparkVoto(sofia,  ofertaXiaomi, true));

        sparkVotoRepository.save(new SparkVoto(pedro,  ofertaPSPlus, true));
        sparkVotoRepository.save(new SparkVoto(elena,  ofertaPSPlus, true));
        sparkVotoRepository.save(new SparkVoto(andres, ofertaPSPlus, true));

        sparkVotoRepository.save(new SparkVoto(maria,  ofertaSamsungQLED, true));
        sparkVotoRepository.save(new SparkVoto(carlos, ofertaRoombaIRobot, true));
        sparkVotoRepository.save(new SparkVoto(pedro,  ofertaRTX4060, true));
        sparkVotoRepository.save(new SparkVoto(elena,  ofertaRTX4060, true));
        sparkVotoRepository.save(new SparkVoto(sofia,  ofertaRTX4060, true));
        sparkVotoRepository.save(new SparkVoto(miguel, ofertaNetflixTrial, true));
        sparkVotoRepository.save(new SparkVoto(lucia,  ofertaNetflixTrial, true));
        sparkVotoRepository.save(new SparkVoto(andres, ofertaNetflixTrial, true));

        // ── 10. COMENTARIOS EN OFERTAS ────────────────────────────────────────
        comentarioRepository.save(new Comentario("¡Increíble precio! Lo compré y llegó en 24h perfectamente embalado.", ofertaAirpods, carlos));
        comentarioRepository.save(new Comentario("¿Funciona bien el ANC en zonas de mucho ruido? Estoy dudando entre estos y los Sony XM5.", ofertaAirpods, miguel));
        comentarioRepository.save(new Comentario("@miguel_motor Sí, el ANC es el mejor del mercado actualmente. Los Sony son muy buenos también pero estos tienen audio espacial. Para llamadas yo prefiero los AirPods.", ofertaAirpods, pedro));
        comentarioRepository.save(new Comentario("Ya no está disponible este precio, ha subido a 209€. :-/", ofertaAirpods, andres));

        comentarioRepository.save(new Comentario("Compré la key, activé en 30 segundos. 100% real y funcional.", ofertaWindows, sofia));
        comentarioRepository.save(new Comentario("¿Es activación permanente o tiene caducidad?", ofertaWindows, lucia));
        comentarioRepository.save(new Comentario("@lucia_moda Es OEM, ligada a un hardware concreto. Mientras no cambies la placa, es vitalicia.", ofertaWindows, carlos));
        comentarioRepository.save(new Comentario("Cuidado con las keys de Kinguin. Algunas son robadas. Mejor comprar en tiendas oficiales.", ofertaWindows, elena));

        comentarioRepository.save(new Comentario("La PS5 ya merece la pena a este precio, hay un catálogo brutal.", ofertaPS5, pedro));
        comentarioRepository.save(new Comentario("¿Sigue en stock? Entro y me dice agotado.", ofertaPS5, andres));

        comentarioRepository.save(new Comentario("El Xiaomi 13 Ultra también está a buen precio, ¿alguien lo compara con este?", ofertaXiaomi, sofia));
        comentarioRepository.save(new Comentario("El Redmi Note 13 Pro+ tiene mejor relación calidad/precio. La cámara de 200MP es una pasada para su rango.", ofertaXiaomi, pedro));

        comentarioRepository.save(new Comentario("RTX 4060 Ti para 1440p es una bestia. Muy buen precio aquí.", ofertaRTX4060, carlos));
        comentarioRepository.save(new Comentario("¿Merece la pena respecto a una 3080 de segunda mano?", ofertaRTX4060, maria));

        // ── 11. FAVORITOS ─────────────────────────────────────────────────────
        favoritoRepository.save(favoritoOferta(maria,    ofertaAirpods));
        favoritoRepository.save(favoritoOferta(pedro,    ofertaWindows));
        favoritoRepository.save(favoritoOferta(sofia,    ofertaRoombaIRobot));
        favoritoRepository.save(favoritoOferta(andres,   ofertaNetflixTrial));
        favoritoRepository.save(favoritoOferta(lucia,    ofertaNike));
        favoritoRepository.save(favoritoOferta(elena,    ofertaPS5));
        favoritoRepository.save(favoritoOferta(miguel,   ofertaRTX4060));

        favoritoRepository.save(favoritoProducto(maria,   iphone14));
        favoritoRepository.save(favoritoProducto(pedro,   macbookPro));
        favoritoRepository.save(favoritoProducto(sofia,   sonyWH));
        favoritoRepository.save(favoritoProducto(lucia,   nikeSneakers));
        favoritoRepository.save(favoritoProducto(andres,  ps5Console));
        favoritoRepository.save(favoritoProducto(elena,   bicicletaCarretera));
        favoritoRepository.save(favoritoProducto(carlos,  sonyA7IV));


        // ── 12. MENSAJES (chat legacy) ────────────────────────────────────────
        Mensaje msg1 = new Mensaje("Hola, ¿sigue disponible el iPhone 14 Pro?", maria, iphone14);
        mensajeRepository.save(msg1);
        Mensaje msg2 = new Mensaje("Sí, totalmente disponible. ¿Cuándo quieres verlo?", carlos, iphone14);
        mensajeRepository.save(msg2);
        Mensaje msg3 = new Mensaje("¿Aceptas 700€? Es que el mercado ha bajado bastante.", maria, iphone14);
        mensajeRepository.save(msg3);
        Mensaje msg4 = new Mensaje("Por 720€ lo dejo. Es el precio mínimo que acepto por el estado que tiene.", carlos, iphone14);
        mensajeRepository.save(msg4);

        Mensaje msg5 = new Mensaje("Buenas, ¿puedo ver la RTX 4070 Ti en persona?", elena, rtx4070);
        mensajeRepository.save(msg5);
        Mensaje msg6 = new Mensaje("Claro, estoy en Valencia. ¿Cuándo te viene bien?", pedro, rtx4070);
        mensajeRepository.save(msg6);

        // ── 13. CHAT MENSAJES (sistema nuevo con WebSocket) ───────────────────
        ChatMensaje chat1 = chatTexto(iphone14, maria, carlos,
            "Hola Carlos, ¿sigues teniendo el iPhone 14 Pro? Me interesa mucho.", -120);
        ChatMensaje chat2 = chatTexto(iphone14, carlos, maria,
            "Sí, aquí lo tengo. Está prácticamente nuevo. ¿Quieres que te mande más fotos?", -115);
        ChatMensaje chat3 = chatPropuesta(iphone14, maria, carlos, 700.0, -110);
        ChatMensaje chat4 = chatTexto(iphone14, carlos, maria,
            "La mínima que acepto es 720€. Tiene batería al 97% y sin ningún arañazo.", -105);
        ChatMensaje chat5 = chatTexto(iphone14, maria, carlos,
            "Ok, trato hecho. ¿Cómo lo hacemos, envío o en persona?", -100);
        ChatMensaje chat6 = chatTexto(iphone14, carlos, maria,
            "Prefiero por Wallapop pero como estamos en Nexus lo hacemos aquí. Te lo envío mañana.", -95);

        ChatMensaje chat7 = chatTexto(ps5Console, andres, carlos,
            "Buenas! ¿La PS5 tiene problemas de ventilación o de sobrecalentamiento?", -200);
        ChatMensaje chat8 = chatTexto(ps5Console, carlos, andres,
            "Ninguno. Siempre la he tenido en vertical con buena ventilación. Sin ningún problema.", -195);
        ChatMensaje chat9 = chatTexto(ps5Console, andres, carlos,
            "¿Los 3 juegos que incluyes son físicos o digitales?", -190);
        ChatMensaje chat10 = chatTexto(ps5Console, carlos, andres,
            "Spider-Man 2 y Returnal son físicos. Horizon FW es código digital.", -185);

        ChatMensaje chat11 = chatTexto(macbookPro, sofia, pedro,
            "Hola! ¿El MacBook tiene alguna mancha en la pantalla o blemish en el aluminio?", -50);
        ChatMensaje chat12 = chatTexto(macbookPro, pedro, sofia,
            "Ninguna. Perfecto. Siempre con funda de cuero desde el primer día.", -45);

        // ── 14. COMPRAS ───────────────────────────────────────────────────────
        // Compra 1: COMPLETADA (iPhone 14 Pro - maria compra a carlos)
        Compra compra1 = new Compra();
        compra1.setComprador(maria);
        compra1.setProducto(iphone14);
        compra1.setEstado(EstadoCompra.COMPLETADA);
        compra1.setStripePaymentIntentId("pi_3Qxyz1234COMPLETED001");
        compra1.setPrecioFinal(725.0);
        compra1.setPrecioEnvio(5.0);
        compra1.setMetodoEntrega(MetodoEntrega.ENVIO_PAQUETERIA);
        compra1.setDirNombre("María López García");
        compra1.setDirCalle("Carrer de Gràcia 15, 1º1ª");
        compra1.setDirCiudad("Barcelona");
        compra1.setDirCodigoPostal("08012");
        compra1.setDirPais("España");
        compra1.setDirTelefono("600333444");
        compra1.setFechaCompra(LocalDateTime.now().minusDays(30));
        compra1.setFechaPago(LocalDateTime.now().minusDays(30));
        compra1.setFechaEnvio(LocalDateTime.now().minusDays(29));
        compra1.setFechaEntrega(LocalDateTime.now().minusDays(27));
        compra1.setFechaCompletada(LocalDateTime.now().minusDays(27));
        iphone14.setEstadoProducto(EstadoProducto.VENDIDO);
        productoRepository.save(iphone14);
        compraRepository.save(compra1);

        // Compra 2: COMPLETADA (PS5 - andres compra a carlos)
        Compra compra2 = new Compra();
        compra2.setComprador(andres);
        compra2.setProducto(ps5Console);
        compra2.setEstado(EstadoCompra.COMPLETADA);
        compra2.setStripePaymentIntentId("pi_3Qxyz1234COMPLETED002");
        compra2.setPrecioFinal(420.0);
        compra2.setPrecioEnvio(0.0);
        compra2.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        compra2.setFechaCompra(LocalDateTime.now().minusDays(20));
        compra2.setFechaPago(LocalDateTime.now().minusDays(20));
        compra2.setFechaEntrega(LocalDateTime.now().minusDays(19));
        compra2.setFechaCompletada(LocalDateTime.now().minusDays(19));
        ps5Console.setEstadoProducto(EstadoProducto.VENDIDO);
        productoRepository.save(ps5Console);
        compraRepository.save(compra2);

        // Compra 3: ENVIADA (MacBook Pro - sofia compra a pedro)
        Compra compra3 = new Compra();
        compra3.setComprador(sofia);
        compra3.setProducto(macbookPro);
        compra3.setEstado(EstadoCompra.ENVIADO);
        compra3.setStripePaymentIntentId("pi_3Qxyz1234ENVIADO003");
        compra3.setPrecioFinal(1665.0);
        compra3.setPrecioEnvio(15.0);
        compra3.setMetodoEntrega(MetodoEntrega.ENVIO_PAQUETERIA);
        compra3.setDirNombre("Sofía Martínez Ruiz");
        compra3.setDirCalle("Calle Retiro 88, 4ºB");
        compra3.setDirCiudad("Madrid");
        compra3.setDirCodigoPostal("28009");
        compra3.setDirPais("España");
        compra3.setDirTelefono("600555666");
        compra3.setFechaCompra(LocalDateTime.now().minusDays(3));
        compra3.setFechaPago(LocalDateTime.now().minusDays(3));
        compra3.setFechaEnvio(LocalDateTime.now().minusDays(2));
        macbookPro.setEstadoProducto(EstadoProducto.RESERVADO);
        productoRepository.save(macbookPro);
        compraRepository.save(compra3);

        // Compra 4: PAGADA (Sony WH-1000XM5 - elena compra a sofia)
        Compra compra4 = new Compra();
        compra4.setComprador(elena);
        compra4.setProducto(sonyWH);
        compra4.setEstado(EstadoCompra.PAGADO);
        compra4.setStripePaymentIntentId("pi_3Qxyz1234PAGADO004");
        compra4.setPrecioFinal(226.0);
        compra4.setPrecioEnvio(6.0);
        compra4.setMetodoEntrega(MetodoEntrega.ENVIO_PAQUETERIA);
        compra4.setDirNombre("Elena Sánchez");
        compra4.setDirCalle("Paseo Independencia 45, 2ºA");
        compra4.setDirCiudad("Zaragoza");
        compra4.setDirCodigoPostal("50001");
        compra4.setDirPais("España");
        compra4.setDirTelefono("600777888");
        compra4.setFechaCompra(LocalDateTime.now().minusDays(1));
        compra4.setFechaPago(LocalDateTime.now().minusDays(1));
        sonyWH.setEstadoProducto(EstadoProducto.RESERVADO);
        productoRepository.save(sonyWH);
        compraRepository.save(compra4);

        // Compra 5: EN_DISPUTA (RTX 4070 Ti - lucia compra a pedro)
        Compra compra5 = new Compra();
        compra5.setComprador(lucia);
        compra5.setProducto(rtx4070);
        compra5.setEstado(EstadoCompra.EN_DISPUTA);
        compra5.setStripePaymentIntentId("pi_3Qxyz1234DISPUTA005");
        compra5.setPrecioFinal(650.0);
        compra5.setPrecioEnvio(0.0);
        compra5.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        compra5.setFechaCompra(LocalDateTime.now().minusDays(10));
        compra5.setFechaPago(LocalDateTime.now().minusDays(10));
        compra5.setFechaEnvio(LocalDateTime.now().minusDays(8));
        rtx4070.setEstadoProducto(EstadoProducto.RESERVADO);
        productoRepository.save(rtx4070);
        compraRepository.save(compra5);

        // Compra 6: CANCELADA (Bicicleta Trek - miguel iba a comprar a elena)
        Compra compra6 = new Compra();
        compra6.setComprador(miguel);
        compra6.setProducto(bicicletaCarretera);
        compra6.setEstado(EstadoCompra.CANCELADA);
        compra6.setStripePaymentIntentId("pi_3Qxyz1234CANCELADA006");
        compra6.setPrecioFinal(890.0);
        compra6.setPrecioEnvio(0.0);
        compra6.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        compra6.setFechaCompra(LocalDateTime.now().minusDays(15));
        compra6.setFechaCancelacion(LocalDateTime.now().minusDays(14));
        compraRepository.save(compra6);

        // Compra 7: REEMBOLSADA (iRobot Roomba - miguel compró a sofia)
        Compra compra7 = new Compra();
        compra7.setComprador(miguel);
        compra7.setProducto(roomba);
        compra7.setEstado(EstadoCompra.REEMBOLSADA);
        compra7.setStripePaymentIntentId("pi_3Qxyz1234REEMBOLSO007");
        compra7.setPrecioFinal(320.0);
        compra7.setPrecioEnvio(0.0);
        compra7.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        compra7.setFechaCompra(LocalDateTime.now().minusDays(25));
        compra7.setFechaPago(LocalDateTime.now().minusDays(25));
        compra7.setFechaCompletada(LocalDateTime.now().minusDays(22));
        compraRepository.save(compra7);

        // Compra 8: PENDIENTE (Nintendo Switch - carlos va a comprar a maria)
        Compra compra8 = new Compra();
        compra8.setComprador(carlos);
        compra8.setProducto(nintendoSwitch);
        compra8.setEstado(EstadoCompra.PENDIENTE);
        compra8.setPrecioFinal(298.0);
        compra8.setPrecioEnvio(8.0);
        compra8.setMetodoEntrega(MetodoEntrega.ENVIO_PAQUETERIA);
        compra8.setFechaCompra(LocalDateTime.now().minusHours(2));
        compraRepository.save(compra8);

        // Compra 9: ENTREGADO (LG OLED - pedro compró a carlos)
        Compra compra9 = new Compra();
        compra9.setComprador(pedro);
        compra9.setProducto(lgOled);
        compra9.setEstado(EstadoCompra.ENTREGADO);
        compra9.setStripePaymentIntentId("pi_3Qxyz1234ENTREGADO009");
        compra9.setPrecioFinal(820.0);
        compra9.setPrecioEnvio(0.0);
        compra9.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        compra9.setFechaCompra(LocalDateTime.now().minusDays(7));
        compra9.setFechaPago(LocalDateTime.now().minusDays(7));
        compra9.setFechaEntrega(LocalDateTime.now().minusDays(6));
        lgOled.setEstadoProducto(EstadoProducto.VENDIDO);
        productoRepository.save(lgOled);
        compraRepository.save(compra9);

        // ── 15. ENVÍOS ────────────────────────────────────────────────────────
        // Envío de compra1 (completada - iPhone 14 Pro)
        Envio envio1 = new Envio();
        envio1.setCompra(compra1);
        envio1.setEstado(EstadoEnvio.ENTREGADO);
        envio1.setMetodoEntrega(MetodoEntrega.ENVIO_PAQUETERIA);
        envio1.setNombreDestinatario("María López García");
        envio1.setDireccion("Carrer de Gràcia 15, 1º1ª");
        envio1.setCiudad("Barcelona");
        envio1.setCodigoPostal("08012");
        envio1.setPais("España");
        envio1.setTelefono("600333444");
        envio1.setTransportista("MRW");
        envio1.setNumeroSeguimiento("MRW2024001122334");
        envio1.setUrlSeguimiento("https://www.mrw.es/seguimiento_envios?ref=MRW2024001122334");
        envio1.setPrecioEnvio(5.0);
        envio1.setFechaEnvio(LocalDateTime.now().minusDays(29));
        envio1.setFechaEstimadaEntrega(LocalDateTime.now().minusDays(27));
        envio1.setFechaConfirmacionEntrega(LocalDateTime.now().minusDays(27));
        envio1.setValoracionVendedor(5);
        envio1.setComentarioValoracion("Envío rapidísimo, producto exactamente como se describía. ¡Vendedor 10!");
        envio1.setStripePaymentIntentId("pi_3Qxyz1234COMPLETED001");
        envioRepository.save(envio1);

        // Envío de compra2 (completada - PS5 en persona)
        Envio envio2 = new Envio();
        envio2.setCompra(compra2);
        envio2.setEstado(EstadoEnvio.ENTREGADO);
        envio2.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        envio2.setFechaConfirmacionEntrega(LocalDateTime.now().minusDays(19));
        envio2.setStripePaymentIntentId("pi_3Qxyz1234COMPLETED002");
        envioRepository.save(envio2);

        // Envío de compra3 (enviado - MacBook Pro)
        Envio envio3 = new Envio();
        envio3.setCompra(compra3);
        envio3.setEstado(EstadoEnvio.EN_TRANSITO);
        envio3.setMetodoEntrega(MetodoEntrega.ENVIO_PAQUETERIA);
        envio3.setNombreDestinatario("Sofía Martínez Ruiz");
        envio3.setDireccion("Calle Retiro 88, 4ºB");
        envio3.setCiudad("Madrid");
        envio3.setCodigoPostal("28009");
        envio3.setPais("España");
        envio3.setTelefono("600555666");
        envio3.setTransportista("SEUR");
        envio3.setNumeroSeguimiento("SEUR20241122334455");
        envio3.setUrlSeguimiento("https://www.seur.com/seguimiento/?ref=SEUR20241122334455");
        envio3.setPrecioEnvio(15.0);
        envio3.setFechaEnvio(LocalDateTime.now().minusDays(2));
        envio3.setFechaEstimadaEntrega(LocalDateTime.now().plusDays(1));
        envio3.setStripePaymentIntentId("pi_3Qxyz1234ENVIADO003");
        envioRepository.save(envio3);

        // Envío de compra4 (pendiente envío - Sony WH)
        Envio envio4 = new Envio();
        envio4.setCompra(compra4);
        envio4.setEstado(EstadoEnvio.PENDIENTE_ENVIO);
        envio4.setMetodoEntrega(MetodoEntrega.ENVIO_PAQUETERIA);
        envio4.setNombreDestinatario("Elena Sánchez");
        envio4.setDireccion("Paseo Independencia 45, 2ºA");
        envio4.setCiudad("Zaragoza");
        envio4.setCodigoPostal("50001");
        envio4.setPais("España");
        envio4.setTelefono("600777888");
        envio4.setPrecioEnvio(6.0);
        envio4.setStripePaymentIntentId("pi_3Qxyz1234PAGADO004");
        envioRepository.save(envio4);

        // Envío de compra5 (disputa - RTX 4070 Ti)
        Envio envio5 = new Envio();
        envio5.setCompra(compra5);
        envio5.setEstado(EstadoEnvio.INCIDENCIA);
        envio5.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        envio5.setStripePaymentIntentId("pi_3Qxyz1234DISPUTA005");
        envio5.setFechaEnvio(LocalDateTime.now().minusDays(8));
        envioRepository.save(envio5);

        // Envío de compra9 (entregado - LG OLED)
        Envio envio9 = new Envio();
        envio9.setCompra(compra9);
        envio9.setEstado(EstadoEnvio.ENTREGADO);
        envio9.setMetodoEntrega(MetodoEntrega.ENTREGA_EN_PERSONA);
        envio9.setFechaConfirmacionEntrega(LocalDateTime.now().minusDays(6));
        envio9.setStripePaymentIntentId("pi_3Qxyz1234ENTREGADO009");
        envioRepository.save(envio9);

        // ── 16. VALORACIONES ──────────────────────────────────────────────────
        // Valoración de compra1 (maria → carlos, 5 estrellas)
        Valoracion val1 = new Valoracion();
        val1.setComprador(maria);
        val1.setVendedor(carlos);
        val1.setCompra(compra1);
        val1.setPuntuacion(5);
        val1.setComentario("Producto exactamente como se describía. Envío rapidísimo y bien embalado. Carlos es un vendedor de 10, recomiendo 100%.");
        val1.setFecha(LocalDateTime.now().minusDays(26));
        val1.setRespuestaVendedor("¡Gracias María! Ha sido un placer. Espero que disfrutes del iPhone. ¡Hasta la próxima!");
        val1.setFechaRespuesta(LocalDateTime.now().minusDays(25));
        valoracionRepository.save(val1);

        // Valoración de compra2 (andres → carlos, 4 estrellas)
        Valoracion val2 = new Valoracion();
        val2.setComprador(andres);
        val2.setVendedor(carlos);
        val2.setCompra(compra2);
        val2.setPuntuacion(4);
        val2.setComentario("La PS5 estaba tal como se describía. La entrega en persona fue puntual. Un punto menos porque el mando tenía el gatillo un poco duro.");
        val2.setFecha(LocalDateTime.now().minusDays(18));
        valoracionRepository.save(val2);

        // Valoración de compra9 (pedro → carlos, 5 estrellas)
        Valoracion val3 = new Valoracion();
        val3.setComprador(pedro);
        val3.setVendedor(carlos);
        val3.setCompra(compra9);
        val3.setPuntuacion(5);
        val3.setComentario("El LG OLED impresionante. Carlos muy amable y puntual en la entrega. Sin duda volvería a comprarle.");
        val3.setFecha(LocalDateTime.now().minusDays(5));
        val3.setRespuestaVendedor("Muchas gracias Pedro! Espero que lo disfrutes para el gaming, es una bestia.");
        val3.setFechaRespuesta(LocalDateTime.now().minusDays(5).plusHours(3));
        valoracionRepository.save(val3);

        // Actualizar reputación de Carlos tras las valoraciones
        carlos.setReputacion(4.7);
        carlos.setTotalVentas(215);
        usuarioRepository.save(carlos);

        // ── 17. DEVOLUCIONES ──────────────────────────────────────────────────
        // Devolución compra5 (disputa RTX 4070 Ti)
        Devolucion dev1 = new Devolucion();
        dev1.setCompra(compra5);
        dev1.setEstado(EstadoDevolucion.SOLICITADA);
        dev1.setMotivo(MotivoDevolucion.PRODUCTO_NO_CORRESPONDE);
        dev1.setDescripcion("La tarjeta gráfica tiene arañazos en el backplate y el ventilador hace un ruido anómalo que no se menciona en el anuncio. Solicito devolución o reducción del precio.");
        dev1.setFechaSolicitud(LocalDateTime.now().minusDays(7));
        devolucionRepository.save(dev1);

        // Devolución compra7 (reembolsada - iRobot Roomba)
        Devolucion dev2 = new Devolucion();
        dev2.setCompra(compra7);
        dev2.setEstado(EstadoDevolucion.COMPLETADA);
        dev2.setMotivo(MotivoDevolucion.PRODUCTO_DEFECTUOSO);
        dev2.setDescripcion("El Roomba no conecta con el WiFi y la batería no carga correctamente. No funciona según lo descrito.");
        dev2.setNotaVendedor("Entendido, aceptamos la devolución. Envía el producto de vuelta con el embalaje original.");
        dev2.setTrackingDevolucion("CORREOS2024998877");
        dev2.setTransportistaDevolucion("Correos");
        dev2.setImporteDevolucion(320.0);
        dev2.setFechaSolicitud(LocalDateTime.now().minusDays(22));
        dev2.setFechaResolucion(LocalDateTime.now().minusDays(18));
        devolucionRepository.save(dev2);

        // ── 18. REPORTES ──────────────────────────────────────────────────────
        Reporte rep1 = new Reporte();
        rep1.setReportador(elena);
        rep1.setTipo(TipoReporte.OFERTA);
        rep1.setMotivo(MotivoReporte.SPAM);
        rep1.setDescripcion("Esta oferta de Windows 11 está duplicada. Ya existe la misma oferta publicada hace 2 horas por el mismo usuario.");
        rep1.setEstado(EstadoReporte.RESUELTO);
        rep1.setOfertaDenunciada(ofertaWindows);
        rep1.setFecha(LocalDateTime.now().minusDays(5));
        rep1.setResolucion("Verificado. La oferta es válida y el precio es real. No es spam.");
        rep1.setFechaResolucion(LocalDateTime.now().minusDays(4));
        reporteRepository.save(rep1);

        Reporte rep2 = new Reporte();
        rep2.setReportador(andres);
        rep2.setTipo(TipoReporte.PRODUCTO);
        rep2.setMotivo(MotivoReporte.FRAUDE);
        rep2.setDescripcion("El vendedor afirma vender un iPhone 14 Pro pero en las fotos parece un clone chino. La pantalla no tiene las esquinas del iPhone original.");
        rep2.setEstado(EstadoReporte.EN_REVISION);
        rep2.setProductoDenunciado(iphone14);
        rep2.setFecha(LocalDateTime.now().minusDays(2));
        reporteRepository.save(rep2);

        Reporte rep3 = new Reporte();
        rep3.setReportador(sofia);
        rep3.setTipo(TipoReporte.USUARIO);
        rep3.setMotivo(MotivoReporte.ACOSO);
        rep3.setDescripcion("Este usuario me ha enviado varios mensajes de acoso después de que no acepté su oferta de precio. Solicito que se tome medida.");
        rep3.setEstado(EstadoReporte.PENDIENTE);
        rep3.setActorDenunciado(miguel);
        rep3.setFecha(LocalDateTime.now().minusHours(6));
        reporteRepository.save(rep3);

        Reporte rep4 = new Reporte();
        rep4.setReportador(pedro);
        rep4.setTipo(TipoReporte.OFERTA);
        rep4.setMotivo(MotivoReporte.INFORMACION_FALSA);
        rep4.setDescripcion("Esta oferta dice que el precio es mínimo histórico pero he encontrado precios más bajos en otras tiendas hace 3 semanas.");
        rep4.setEstado(EstadoReporte.DESESTIMADO);
        rep4.setOfertaDenunciada(ofertaRoombaIRobot);
        rep4.setFecha(LocalDateTime.now().minusDays(8));
        rep4.setResolucion("Verificado el historial de precios. El precio publicado sí es mínimo histórico en Amazon según datos de CamelCamelCamel.");
        rep4.setFechaResolucion(LocalDateTime.now().minusDays(7));
        reporteRepository.save(rep4);

        Reporte rep5 = new Reporte();
        rep5.setReportador(lucia);
        rep5.setTipo(TipoReporte.VEHICULO);
        rep5.setMotivo(MotivoReporte.FRAUDE);
        rep5.setDescripcion("El vendedor asegura que el coche tiene 62.000 km pero he consultado el historial y aparece con 95.000 km hace 2 años. Posible manipulación del cuentakilómetros.");
        rep5.setEstado(EstadoReporte.PENDIENTE);
        rep5.setVehiculoDenunciado(bmw320d);
        rep5.setFecha(LocalDateTime.now().minusHours(18));
        reporteRepository.save(rep5);

        Reporte rep6 = new Reporte();
        rep6.setReportador(carlos);
        rep6.setTipo(TipoReporte.COMENTARIO);
        rep6.setMotivo(MotivoReporte.CONTENIDO_INAPROPIADO);
        rep6.setDescripcion("Este comentario contiene lenguaje ofensivo y difamatorio hacia el vendedor sin fundamento.");
        rep6.setEstado(EstadoReporte.PENDIENTE);
        rep6.setFecha(LocalDateTime.now().minusHours(1));
        reporteRepository.save(rep6);

        // ── 19. BLOQUEOS ──────────────────────────────────────────────────────
        Bloqueo bloqueo1 = new Bloqueo();
        bloqueo1.setBloqueador(sofia);
        bloqueo1.setBloqueado(miguel);
        bloqueo1.setMotivo("Acoso persistente después de declinar su oferta. Múltiples mensajes insistentes.");
        bloqueoRepository.save(bloqueo1);

        Bloqueo bloqueo2 = new Bloqueo();
        bloqueo2.setBloqueador(maria);
        bloqueo2.setBloqueado(andres);
        bloqueo2.setMotivo("Intentó hacer una compra falsa y canceló sin motivo justo cuando iba a enviarlo.");
        bloqueoRepository.save(bloqueo2);

        // ── 20. NEWSLETTER SUSCRIPCIONES ──────────────────────────────────────
        // Suscripción activa - carlos
        NewsletterSuscripcion news1 = new NewsletterSuscripcion();
        news1.setEmail(carlos.getEmail());
        news1.setNombre("Carlos García");
        news1.setEstado(EstadoSuscripcion.ACTIVO);
        news1.setTokenBaja(UUID.randomUUID().toString());
        news1.setFechaConfirmacion(LocalDateTime.now().minusMonths(3));
        news1.setRecibirOfertas(true);
        news1.setRecibirNoticias(true);
        news1.setRecibirTrending(true);
        news1.setFrecuencia("SEMANAL");
        news1.setFechaConsentimiento(LocalDateTime.now().minusMonths(3));
        news1.setIpConsentimiento("192.168.1.10");
        news1.setVersionPolitica("1.0");
        newsletterRepository.save(news1);

        // Suscripción activa - maria
        NewsletterSuscripcion news2 = new NewsletterSuscripcion();
        news2.setEmail(maria.getEmail());
        news2.setNombre("María López");
        news2.setEstado(EstadoSuscripcion.ACTIVO);
        news2.setTokenBaja(UUID.randomUUID().toString());
        news2.setFechaConfirmacion(LocalDateTime.now().minusMonths(2));
        news2.setRecibirOfertas(true);
        news2.setRecibirNoticias(false);
        news2.setRecibirTrending(true);
        news2.setFrecuencia("DIARIO");
        news2.setFechaConsentimiento(LocalDateTime.now().minusMonths(2));
        news2.setIpConsentimiento("77.231.45.100");
        news2.setVersionPolitica("1.0");
        newsletterRepository.save(news2);

        // Suscripción activa - pedro
        NewsletterSuscripcion news3 = new NewsletterSuscripcion();
        news3.setEmail(pedro.getEmail());
        news3.setNombre("Pedro Ruiz");
        news3.setEstado(EstadoSuscripcion.ACTIVO);
        news3.setTokenBaja(UUID.randomUUID().toString());
        news3.setFechaConfirmacion(LocalDateTime.now().minusWeeks(6));
        news3.setRecibirOfertas(true);
        news3.setRecibirNoticias(true);
        news3.setRecibirTrending(true);
        news3.setFrecuencia("SEMANAL");
        news3.setFechaConsentimiento(LocalDateTime.now().minusWeeks(6));
        news3.setIpConsentimiento("195.55.100.22");
        news3.setVersionPolitica("1.0");
        newsletterRepository.save(news3);

        // Suscripción pendiente (double opt-in no completado) - lucia
        NewsletterSuscripcion news4 = new NewsletterSuscripcion();
        news4.setEmail(lucia.getEmail());
        news4.setNombre("Lucía Fernández");
        news4.setEstado(EstadoSuscripcion.PENDIENTE);
        news4.setTokenConfirmacion(UUID.randomUUID().toString());
        news4.setTokenBaja(UUID.randomUUID().toString());
        news4.setFechaEnvioConfirmacion(LocalDateTime.now().minusHours(3));
        news4.setRecibirOfertas(true);
        news4.setRecibirNoticias(false);
        news4.setRecibirTrending(true);
        news4.setFrecuencia("QUINCENAL");
        news4.setFechaConsentimiento(LocalDateTime.now().minusHours(3));
        news4.setIpConsentimiento("89.128.44.55");
        news4.setVersionPolitica("1.0");
        newsletterRepository.save(news4);

        // Suscripción de baja - elena
        NewsletterSuscripcion news5 = new NewsletterSuscripcion();
        news5.setEmail(elena.getEmail());
        news5.setNombre("Elena Sánchez");
        news5.setEstado(EstadoSuscripcion.BAJA);
        news5.setTokenBaja(UUID.randomUUID().toString());
        news5.setFechaConfirmacion(LocalDateTime.now().minusMonths(4));
        news5.setFechaBaja(LocalDateTime.now().minusWeeks(2));
        news5.setMotivoBaja("Recibo demasiados emails y no tengo tiempo de leerlos.");
        news5.setRecibirOfertas(false);
        news5.setRecibirNoticias(false);
        news5.setRecibirTrending(false);
        news5.setFrecuencia("SEMANAL");
        news5.setFechaConsentimiento(LocalDateTime.now().minusMonths(4));
        news5.setVersionPolitica("1.0");
        newsletterRepository.save(news5);

        // Suscripción activa - sofia
        NewsletterSuscripcion news6 = new NewsletterSuscripcion();
        news6.setEmail(sofia.getEmail());
        news6.setNombre("Sofía Martínez");
        news6.setEstado(EstadoSuscripcion.ACTIVO);
        news6.setTokenBaja(UUID.randomUUID().toString());
        news6.setFechaConfirmacion(LocalDateTime.now().minusMonths(1));
        news6.setRecibirOfertas(true);
        news6.setRecibirNoticias(true);
        news6.setRecibirTrending(false);
        news6.setFrecuencia("MENSUAL");
        news6.setFechaConsentimiento(LocalDateTime.now().minusMonths(1));
        news6.setIpConsentimiento("212.100.77.33");
        news6.setVersionPolitica("1.0");
        newsletterRepository.save(news6);

        // ── 21. NOTIFICACIONES IN-APP ─────────────────────────────────────────
        // Para maria (compradora activa)
        notif(maria, TipoNotificacion.COMPRA_CONFIRMADA, "Compra confirmada",
              "Tu compra del iPhone 14 Pro ha sido confirmada. El vendedor preparará el envío en breve.", "/compras", true, -30);
        notif(maria, TipoNotificacion.ENVIO_ACTUALIZADO, "¡Tu pedido está en camino!",
              "Carlos ha enviado el iPhone 14 Pro. Nº seguimiento: MRW2024001122334 (MRW)", "/compras", true, -29);
        notif(maria, TipoNotificacion.NUEVA_VALORACION, "Valoración recibida",
              "Carlos ha respondido a tu valoración. Ve a verla en tu perfil.", "/perfil", true, -25);
        notif(maria, TipoNotificacion.NUEVO_MENSAJE, "Nuevo mensaje de Carlos",
              "Carlos: 'Muchas gracias María! La batería está al 97%, ya verás qué pasada'", "/chat", false, -1);
        notif(maria, TipoNotificacion.SPARK_EN_OFERTA, "Tu oferta de Xiaomi está en tendencia",
              "La oferta 'Xiaomi Redmi Note 13 Pro+' ha recibido 67 Sparks en las últimas 2 horas.", "/ofertas", false, -2);

        // Para carlos (vendedor activo)
        notif(carlos, TipoNotificacion.NUEVA_COMPRA, "¡Nueva venta! iPhone 14 Pro",
              "María ha comprado tu iPhone 14 Pro por 725€. Prepara el envío lo antes posible.", "/ventas", true, -30);
        notif(carlos, TipoNotificacion.NUEVA_VALORACION, "Nueva valoración de 5 estrellas ⭐",
              "María te ha valorado con 5 estrellas: 'Producto exactamente como se describía...'", "/perfil", true, -26);
        notif(carlos, TipoNotificacion.NUEVA_COMPRA, "¡Nueva venta! LG OLED 55\"",
              "Pedro ha comprado tu LG OLED 55\" por 820€. Confirma la entrega en persona.", "/ventas", true, -7);
        notif(carlos, TipoNotificacion.SISTEMA, "Verifica tu identidad",
              "Para incrementar la confianza, verifica tu identidad subiendo tu DNI. Mejora tu reputación.", "/ajustes", false, -5);

        // Para pedro (vendedor y comprador)
        notif(pedro, TipoNotificacion.DEVOLUCION, "Solicitud de devolución de Lucía",
              "Lucía ha abierto una disputa sobre la RTX 4070 Ti. Tienes 48h para responder.", "/ventas", false, -7);
        notif(pedro, TipoNotificacion.NUEVO_MENSAJE, "Nuevo mensaje de Sofía sobre el MacBook",
              "Sofía: '¿El MacBook tiene alguna mancha en la pantalla?'", "/chat", true, -50);
        notif(pedro, TipoNotificacion.ENVIO_ACTUALIZADO, "Pedido en tránsito hacia Madrid",
              "El MacBook Pro está en camino a Sofía. Número de seguimiento: SEUR20241122334455", "/ventas", false, -2);

        // Para sofia (compradora)
        notif(sofia, TipoNotificacion.COMPRA_CONFIRMADA, "Pago procesado - MacBook Pro M2",
              "Tu compra del MacBook Pro M2 ha sido confirmada. Pedro está preparando el envío.", "/compras", false, -3);
        notif(sofia, TipoNotificacion.ENVIO_ACTUALIZADO, "El MacBook está en camino",
              "Pedro ha enviado tu MacBook Pro. Seguimiento SEUR: SEUR20241122334455. Llegará mañana.", "/compras", false, -2);

        // Para elena (disputas)
        notif(elena, TipoNotificacion.SISTEMA, "Disputa abierta por Lucía",
              "Se ha abierto una disputa en la compra de la RTX 4070 Ti. El equipo de Nexus está revisando el caso.", "/ventas", false, -7);

        // Para andres (mensaje sin leer)
        notif(andres, TipoNotificacion.NUEVO_MENSAJE, "Mensaje de Carlos sobre la PS5",
              "Carlos: 'Spider-Man 2 y Returnal son físicos. Horizon FW es código digital.'", "/chat", false, -185);
        notif(andres, TipoNotificacion.NUEVA_VALORACION, "Has recibido una valoración",
              "Revisa la valoración que has recibido de Carlos en tu perfil de vendedor.", "/perfil", true, -18);

        System.out.println("=== PopulateDB completado con éxito ===");
        System.out.println("  - Categorías:    " + categoriaRepository.count());
        System.out.println("  - Actores:       " + actorRepository.count()
                + " (2 Admins, 2 Empresas, 8 Usuarios)");
        System.out.println("  - Productos:     " + productoRepository.count());
        System.out.println("  - Vehículos:     " + vehiculoRepository.count());
        System.out.println("  - Ofertas:       " + ofertaRepository.count());
        System.out.println("  - SparkVotos:    " + sparkVotoRepository.count());
        System.out.println("  - Comentarios:   " + comentarioRepository.count());
        System.out.println("  - Favoritos:     " + favoritoRepository.count());
        System.out.println("  - Mensajes:      " + mensajeRepository.count());
        System.out.println("  - ChatMensajes:  " + chatMensajeRepository.count());
        System.out.println("  - Compras:       " + compraRepository.count());
        System.out.println("  - Envíos:        " + envioRepository.count());
        System.out.println("  - Valoraciones:  " + valoracionRepository.count());
        System.out.println("  - Devoluciones:  " + devolucionRepository.count());
        System.out.println("  - Reportes:      " + reporteRepository.count());
        System.out.println("  - Bloqueos:      " + bloqueoRepository.count());
        System.out.println("  - Contratos:     " + contratoRepository.count());
        System.out.println("  - Newsletter:    " + newsletterRepository.count());
        System.out.println("  - Notificaciones:" + notificacionRepository.count());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /** Categoría idempotente por slug. */
    private Categoria cat(String nombre, String slug, String icono, String color,
                           Categoria parent, int orden) {
        return categoriaRepository.findBySlug(slug).orElseGet(() -> {
            Categoria c = new Categoria(nombre, slug, icono);
            c.setColor(color);
            c.setOrden(orden);
            c.setActiva(true);
            if (parent != null) c.setParent(parent);
            return categoriaRepository.save(c);
        });
    }

    /** Crea y persiste un Usuario completo. */
    private Usuario usuario(String user, String email, String ubicacion,
                             String bio, double reputacion, int totalVentas, boolean verificado) {
        Usuario u = new Usuario();
        u.setUser(user);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("Password123!"));
        u.setCuentaVerificada(true);
        u.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user);
        u.setBiografia(bio);
        u.setUbicacion(ubicacion);
        u.setReputacion(reputacion);
        u.setTotalVentas(totalVentas);
        u.setEsVerificado(verificado);
        u.setPerfilPublico(true);
        u.setMostrarUbicacion(true);
        u.setMostrarTelefono(false);
        return usuarioRepository.save(u);
    }

    /** Crea y persiste un Producto. */
    private Producto producto(String titulo, String descripcion, Double precio,
                               TipoOferta tipo, Actor vendedor, Categoria categoria,
                               String marca, String modelo, CondicionProducto condicion,
                               boolean admiteEnvio, Double precioEnvio,
                               boolean negociable, String ubicacion, String imagen) {
        Producto p = new Producto(titulo, descripcion, precio, tipo, vendedor, imagen);
        p.setCategoria(categoria);
        p.setMarca(marca);
        p.setModelo(modelo);
        p.setCondicion(condicion);
        p.setAdmiteEnvio(admiteEnvio);
        p.setPrecioEnvio(precioEnvio);
        p.setPrecioNegociable(negociable);
        p.setUbicacion(ubicacion);
        p.setEstadoProducto(EstadoProducto.DISPONIBLE);
        return productoRepository.save(p);
    }

    /** Crea y persiste un Vehículo. */
    private Vehiculo vehiculo(String titulo, String descripcion, Double precio,
                               TipoVehiculo tipoVehiculo, Actor publicador, Categoria categoria,
                               String marca, String modelo, Integer anio, Integer km,
                               String combustible, String cambio, Integer potencia,
                               Integer cilindrada, String color, Integer numPuertas,
                               Integer plazas, String matricula, Boolean itv, Boolean garantia,
                               String ubicacion, String imagen) {
        Vehiculo v = new Vehiculo();
        v.setTitulo(titulo);
        v.setDescripcion(descripcion);
        v.setPrecio(precio);
        v.setTipoVehiculo(tipoVehiculo);
        v.setEstadoVehiculo(EstadoVehiculo.DISPONIBLE);
        v.setTipoOferta(TipoOferta.VENTA);
        v.setPublicador(publicador);
        v.setCategoria(categoria);
        v.setMarca(marca);
        v.setModelo(modelo);
        v.setAnio(anio);
        v.setKilometros(km);
        v.setCombustible(combustible);
        v.setCambio(cambio);
        v.setPotencia(potencia);
        v.setCilindrada(cilindrada);
        v.setColor(color);
        v.setNumeroPuertas(numPuertas);
        v.setPlazas(plazas);
        v.setMatricula(matricula);
        v.setItv(itv);
        v.setGarantia(garantia);
        v.setUbicacion(ubicacion);
        v.setImagenPrincipal(imagen);
        v.setCondicion(CondicionProducto.MUY_BUEN_ESTADO);
        return vehiculoRepository.save(v);
    }

    /**
     * Crea y persiste una Oferta (chollometro).
     * Sobrecarga para fecha de expiración LocalDateTime directa.
     */
    private Oferta oferta(String titulo, String descripcion, Double precio, Double original,
                           String tienda, String url, Categoria cat, Actor actor,
                           BadgeOferta badge, LocalDateTime fechaExpiracion, String imagen,
                           int sparks, int drips, int vistas, int compartidos) {
        Oferta o = new Oferta();
        o.setTitulo(titulo);
        o.setDescripcion(descripcion);
        o.setPrecioOferta(precio);
        o.setPrecioOriginal(original);
        o.setTienda(tienda);
        o.setUrlOferta(url);
        o.setCategoria(cat);
        o.setActor(actor);
        o.setImagenPrincipal(imagen);
        o.setSparkCount(sparks);
        o.setDripCount(drips);
        o.setNumeroVistas(vistas);
        o.setNumeroCompartidos(compartidos);
        o.setEsActiva(true);
        o.setBadge(badge);
        o.setFechaPublicacion(LocalDateTime.now().minusHours(Math.abs(sparks % 72) + 1));
        if (fechaExpiracion != null) o.setFechaExpiracion(fechaExpiracion);
        return ofertaRepository.save(o);
    }

    /**
     * Sobrecarga con horasAtras negativas para fecha de publicación.
     * Si horasAtras > 0 es minusHours, si == COLLAZO_ALIAS (especial) usa el badge dado.
     */
    private Oferta oferta(String titulo, String descripcion, Double precio, Double original,
                           String tienda, String url, Categoria cat, Actor actor,
                           BadgeOferta badge, long horasAtrasPublicacion, String imagen,
                           int sparks, int drips, int vistas, int compartidos) {
        Oferta o = new Oferta();
        o.setTitulo(titulo);
        o.setDescripcion(descripcion);
        o.setPrecioOferta(precio);
        o.setPrecioOriginal(original);
        o.setTienda(tienda);
        o.setUrlOferta(url);
        o.setCategoria(cat);
        o.setActor(actor);
        o.setImagenPrincipal(imagen);
        o.setSparkCount(sparks);
        o.setDripCount(drips);
        o.setNumeroVistas(vistas);
        o.setNumeroCompartidos(compartidos);
        o.setEsActiva(true);
        o.setBadge(badge);
        long horas = Math.abs(horasAtrasPublicacion);
        o.setFechaPublicacion(LocalDateTime.now().minusHours(horas > 0 ? horas : 1));
        return ofertaRepository.save(o);
    }

    /** ChatMensaje de tipo TEXTO. */
    private ChatMensaje chatTexto(Producto producto, Usuario remitente, Usuario receptor,
                                   String texto, long minutosAtras) {
        ChatMensaje m = new ChatMensaje();
        m.setProducto(producto);
        m.setRemitente(remitente);
        m.setReceptor(receptor);
        m.setTexto(texto);
        m.setTipo(TipoMensaje.TEXTO);
        m.setLeido(minutosAtras < -60);
        m.setFechaEnvio(LocalDateTime.now().plusMinutes(minutosAtras));
        return chatMensajeRepository.save(m);
    }

    /** ChatMensaje de tipo OFERTA_PRECIO (propuesta). */
    private ChatMensaje chatPropuesta(Producto producto, Usuario remitente, Usuario receptor,
                                       Double precio, long minutosAtras) {
        ChatMensaje m = new ChatMensaje();
        m.setProducto(producto);
        m.setRemitente(remitente);
        m.setReceptor(receptor);
        m.setTexto("💰 Propuesta de precio: " + precio + "€");
        m.setTipo(TipoMensaje.OFERTA_PRECIO);
        m.setPrecioPropuesto(precio);
        m.setEstadoPropuesta("PENDIENTE");
        m.setLeido(minutosAtras < -60);
        m.setFechaEnvio(LocalDateTime.now().plusMinutes(minutosAtras));
        return chatMensajeRepository.save(m);
    }

    /** Favorito de oferta. */
    private Favorito favoritoOferta(Usuario usuario, Oferta oferta) {
        Favorito f = new Favorito();
        f.setUsuario(usuario);
        f.setOferta(oferta);
        return f;
    }

    /** Favorito de producto. */
    private Favorito favoritoProducto(Usuario usuario, Producto producto) {
        Favorito f = new Favorito();
        f.setUsuario(usuario);
        f.setProducto(producto);
        return f;
    }

    /** Notificación in-app. */
    private void notif(Usuario actor, TipoNotificacion tipo, String titulo, String mensaje,
                        String url, boolean leida, long horasAtras) {
        NotificacionInApp n = new NotificacionInApp();
        n.setActor(actor);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setUrl(url);
        n.setLeida(leida);
        
        // Calculamos la fecha en base al offset de horas que pasamos por parámetro
        // (Como le pasas valores negativos en la invocación como -30, -29, etc., plusHours restará ese tiempo)
        n.setFecha(LocalDateTime.now().plusHours(horasAtras));
        
        notificacionRepository.save(n);
    }

}