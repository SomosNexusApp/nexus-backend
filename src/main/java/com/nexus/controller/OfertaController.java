package com.nexus.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.Actor;
import com.nexus.entity.Oferta;
import com.nexus.repository.ActorRepository;
import com.nexus.service.ModerationService;
import com.nexus.service.OfertaService;
import com.nexus.service.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/oferta")
@Tag(name = "Ofertas", description = "Sistema de chollos con Spark")
public class OfertaController {

    @Autowired 
    private ModerationService moderationService;
    @Autowired private OfertaService ofertaService;
    @Autowired private ActorRepository actorRepository;
    @Autowired private StorageService storageService;

    // --- LISTAR TODAS ---
    @GetMapping
    @Operation(summary = "Listar todas las ofertas activas")
    public ResponseEntity<List<Oferta>> listar() {
        return ResponseEntity.ok(ofertaService.findAll());
    }
    
    // --- VER DETALLE (Incrementa vistas) ---
    @GetMapping("/{id}")
    @Operation(summary = "Ver detalle de oferta")
    public ResponseEntity<Oferta> verOferta(@PathVariable Integer id) {
        Optional<Oferta> oferta = ofertaService.findById(id);
        if (oferta.isPresent()) {
            ofertaService.incrementarVistas(id);
            return ResponseEntity.ok(oferta.get());
        }
        return ResponseEntity.notFound().build();
    }
    
    // --- BUSCAR CON FILTROS (SIN DTO) ---
    @GetMapping("/filtrar")
    @Operation(summary = "Búsqueda avanzada con filtros")
    public ResponseEntity<Map<String, Object>> filtrar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String tienda,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false, defaultValue = "true") Boolean soloActivas,
            @RequestParam(required = false, defaultValue = "fecha") String ordenarPor,
            @RequestParam(required = false, defaultValue = "desc") String direccion,
            @RequestParam(required = false, defaultValue = "0") Integer pagina,
            @RequestParam(required = false, defaultValue = "20") Integer tamañoPagina) {
        
        try {
            Pageable pageable = PageRequest.of(pagina, tamañoPagina);
            
            Page<Oferta> paginaOfertas = ofertaService.buscarConFiltros(
                categoria, tienda, precioMin, precioMax, busqueda, 
                soloActivas, ordenarPor, direccion, pageable
            );
            
            return ResponseEntity.ok(Map.of(
                "ofertas", paginaOfertas.getContent(),
                "paginaActual", paginaOfertas.getNumber(),
                "totalPaginas", paginaOfertas.getTotalPages(),
                "totalElementos", paginaOfertas.getTotalElements()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // --- OFERTAS DESTACADAS ---
    @GetMapping("/destacadas")
    @Operation(summary = "Ofertas destacadas (Spark + Descuento + Reciente)")
    public ResponseEntity<List<Oferta>> destacadas() {
        return ResponseEntity.ok(ofertaService.obtenerDestacadas());
    }
    
    // --- TRENDING ---
    @GetMapping("/trending")
    @Operation(summary = "Trending en las últimas 24 horas")
    public ResponseEntity<List<Oferta>> trending() {
        return ResponseEntity.ok(ofertaService.obtenerTrending());
    }
    
    // --- TOP SPARK ---
    @GetMapping("/top-spark")
    @Operation(summary = "Ofertas con mejor Spark Score")
    public ResponseEntity<List<Oferta>> topSpark() {
        return ResponseEntity.ok(ofertaService.obtenerTopSpark());
    }
    
    // --- EXPIRAN PRONTO ---
    @GetMapping("/expiran-pronto")
    @Operation(summary = "Ofertas que expiran en 24 horas")
    public ResponseEntity<List<Oferta>> expiranProx() {
        return ResponseEntity.ok(ofertaService.obtenerProximasExpirar());
    }
    
    // --- ⚡ VOTAR (SPARK / DRIP) ---
    @PostMapping("/{id}/votar")
    @Operation(summary = "Dar Spark (⚡) o Drip (💧) a una oferta")
    public ResponseEntity<?> votar(
            @PathVariable Integer id, 
            @RequestParam Integer usuarioId,
            @RequestParam Boolean esSpark) {
        
        try {
            ofertaService.votarOferta(id, usuarioId, esSpark);
            
            Optional<Oferta> oferta = ofertaService.findById(id);
            if (oferta.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "mensaje", esSpark ? "⚡ Spark dado" : "💧 Drip dado",
                    "sparkScore", oferta.get().getSparkScore(),
                    "badge", oferta.get().getBadge()
                ));
            }
            return ResponseEntity.ok(Map.of("mensaje", "Voto registrado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // --- COMPARTIR (Incrementa contador) ---
    @PostMapping("/{id}/compartir")
    @Operation(summary = "Registrar que se compartió la oferta")
    public ResponseEntity<?> compartir(@PathVariable Integer id) {
        ofertaService.incrementarCompartidos(id);
        return ResponseEntity.ok(Map.of("mensaje", "Compartido registrado"));
    }
    
    // --- CREAR OFERTA ---
    @PostMapping(value = "/{actorId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publicar oferta con imágenes")
    public ResponseEntity<?> crear(
            @PathVariable Integer actorId,
            @RequestPart("oferta") Oferta oferta,
            @RequestPart("imagenPrincipal") MultipartFile imagenPrincipal,
            @RequestPart(value = "galeria", required = false) List<MultipartFile> galeria) {
        
        try {
            Optional<Actor> actor = actorRepository.findById(actorId);
            if (actor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Actor no encontrado"));
            }
            
            // --- VALIDACIÓN DE MODERACIÓN (CREAR) ---
            String tituloAValidar = oferta.getTitulo() != null ? oferta.getTitulo() : "";
            String descAValidar = oferta.getDescripcion() != null ? oferta.getDescripcion() : "";
            String textoCompleto = tituloAValidar + " " + descAValidar;

            if (!moderationService.esContenidoApropiado(textoCompleto)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El título o descripción contiene lenguaje inapropiado y no cumple las normas de la comunidad."));
            }
            // ----------------------------------------
            
            oferta.setActor(actor.get());
            
            // Subir imagen principal
            String urlPrincipal = storageService.subirImagen(imagenPrincipal);
            if (urlPrincipal == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir imagen principal"));
            }
            oferta.setImagenPrincipal(urlPrincipal);
            
            // Subir galería (máx 5)
            if (galeria != null && !galeria.isEmpty()) {
                for (int i = 0; i < Math.min(galeria.size(), 5); i++) {
                    String urlGaleria = storageService.subirImagen(galeria.get(i));
                    if (urlGaleria != null) {
                        oferta.addImagenGaleria(urlGaleria);
                    }
                }
            }
            
            Oferta guardada = ofertaService.save(oferta);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // --- ACTUALIZAR ---
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Actualizar oferta")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer id,
            @RequestPart("oferta") Oferta nuevosDatos,
            @RequestPart(value = "imagenPrincipal", required = false) MultipartFile imagenPrincipal,
            @RequestPart(value = "galeria", required = false) List<MultipartFile> galeria) {
        
        try {
            // --- VALIDACIÓN DE MODERACIÓN (ACTUALIZAR) ---
            String tituloAValidar = nuevosDatos.getTitulo() != null ? nuevosDatos.getTitulo() : "";
            String descAValidar = nuevosDatos.getDescripcion() != null ? nuevosDatos.getDescripcion() : "";
            String textoCompleto = tituloAValidar + " " + descAValidar;

            // Si está enviando título o descripción, los validamos
            if (!textoCompleto.trim().isEmpty() && !moderationService.esContenidoApropiado(textoCompleto)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El título o descripción contiene lenguaje inapropiado y no cumple las normas de la comunidad."));
            }
            // ---------------------------------------------
            
            return ofertaService.findById(id).map(oferta -> {
                if (nuevosDatos.getTitulo() != null) oferta.setTitulo(nuevosDatos.getTitulo());
                if (nuevosDatos.getDescripcion() != null) oferta.setDescripcion(nuevosDatos.getDescripcion());
                if (nuevosDatos.getTienda() != null) oferta.setTienda(nuevosDatos.getTienda());
                if (nuevosDatos.getPrecioOriginal() != null) oferta.setPrecioOriginal(nuevosDatos.getPrecioOriginal());
                if (nuevosDatos.getPrecioOferta() != null) oferta.setPrecioOferta(nuevosDatos.getPrecioOferta());
                if (nuevosDatos.getUrlOferta() != null) oferta.setUrlOferta(nuevosDatos.getUrlOferta());
                if (nuevosDatos.getFechaExpiracion() != null) oferta.setFechaExpiracion(nuevosDatos.getFechaExpiracion());
                if (nuevosDatos.getCategoria() != null) oferta.setCategoria(nuevosDatos.getCategoria());
                
                // --- NUEVOS CAMPOS ---
                if (nuevosDatos.getCodigoDescuento() != null) oferta.setCodigoDescuento(nuevosDatos.getCodigoDescuento());
                if (nuevosDatos.getEsOnline() != null) oferta.setEsOnline(nuevosDatos.getEsOnline());
                if (nuevosDatos.getCiudadOferta() != null) oferta.setCiudadOferta(nuevosDatos.getCiudadOferta());
                if (nuevosDatos.getGastosEnvio() != null) oferta.setGastosEnvio(nuevosDatos.getGastosEnvio());
                
                if (imagenPrincipal != null && !imagenPrincipal.isEmpty()) {
                    String urlNueva = storageService.subirImagen(imagenPrincipal);
                    if (urlNueva != null) {
                        storageService.eliminarImagen(oferta.getImagenPrincipal());
                        oferta.setImagenPrincipal(urlNueva);
                    }
                }
                
                if (galeria != null && !galeria.isEmpty()) {
                    for (MultipartFile file : galeria) {
                        // Límite a 5 imágenes
                        if (oferta.getGaleriaImagenes().size() < 5) {
                            String urlGaleria = storageService.subirImagen(file);
                            if (urlGaleria != null) {
                                oferta.addImagenGaleria(urlGaleria);
                            }
                        }
                    }
                }
                
                return ResponseEntity.ok(ofertaService.save(oferta));
            }).orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // --- ELIMINAR ---
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar oferta")
    public ResponseEntity<?> borrar(@PathVariable Integer id) {
        Optional<Oferta> oferta = ofertaService.findById(id);
        if (oferta.isPresent()) {
            Oferta o = oferta.get();
            
            storageService.eliminarImagen(o.getImagenPrincipal());
            for (String url : o.getGaleriaImagenes()) {
                storageService.eliminarImagen(url);
            }
            
            ofertaService.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Oferta eliminada"));
        }
        return ResponseEntity.notFound().build();
    }
}