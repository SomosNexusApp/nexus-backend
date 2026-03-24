package com.nexus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.*;
import com.nexus.repository.*;

@Service
public class VehiculoService {

    @Autowired private VehiculoRepository  vehiculoRepository;
    @Autowired private ActorRepository     actorRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private StorageService      storageService;

    // ── CRUD básico ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Vehiculo> findAll() {
        return vehiculoRepository.findAll();
    }
    @Transactional(readOnly = true)
    public Optional<Vehiculo> findById(Integer id) {
        return vehiculoRepository.findById(id);
    }


    
    public List<Vehiculo> findDisponibles() {
        return vehiculoRepository.findByEstadoVehiculo(EstadoVehiculo.DISPONIBLE);
    }

    public List<Vehiculo> findByTipo(TipoVehiculo tipo) {
        return vehiculoRepository.findByTipoVehiculoAndEstadoVehiculo(tipo, EstadoVehiculo.DISPONIBLE);
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> findByPublicador(Integer publicadorId) {
        return vehiculoRepository.findByPublicadorId(publicadorId);
    }
    
    public List<String> getMarcasDisponibles() {
        return vehiculoRepository.findMarcasDistintas();
    }

    public List<Vehiculo> getVehiculosDeUsuario(Integer publicadorId) {
        return vehiculoRepository.findByPublicadorIdOrderByFechaPublicacionDesc(publicadorId);
    }

    // ── Búsqueda paginada COMPLETA (usada por VehiculoController.filtrar) ────

    /**
     * Delega directamente en el método JPQL completo del repositorio.
     * El antiguo "buscarConFiltros" de la firma corta ya NO existe aquí
     * para evitar la ambigüedad que causaba el error de compilación.
     */
    @Transactional(readOnly = true)
    public Page<Vehiculo> buscarPaginado(
            TipoVehiculo tipo, String marca, String modelo,
            Double precioMin, Double precioMax,
            Integer anioMin, Integer anioMax, Integer kmMax,
            TipoCombustible combustible, String cambio, String busqueda,
            Integer potenciaMin, Integer cilindradaMin, String color,
            Integer numeroPuertas, Integer plazas, Boolean garantia, Boolean itv,
            Pageable pageable) {

        return vehiculoRepository.buscarPaginadoGeografico(
            tipo, marca, modelo, precioMin, precioMax,
            anioMin, anioMax, kmMax, combustible, cambio,
            busqueda, potenciaMin, cilindradaMin, color,
            numeroPuertas, plazas, garantia, itv, 
            null, null, null, null, pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Vehiculo> buscarPaginadoGeografico(
            TipoVehiculo tipo, String marca, String modelo,
            Double precioMin, Double precioMax,
            Integer anioMin, Integer anioMax, Integer kmMax,
            TipoCombustible combustible, String cambio, String busqueda,
            Integer potenciaMin, Integer cilindradaMin, String color,
            Integer numeroPuertas, Integer plazas, Boolean garantia, Boolean itv,
            Double minLat, Double maxLat, Double minLng, Double maxLng,
            Pageable pageable) {

        return vehiculoRepository.buscarPaginadoGeografico(
            tipo, marca, modelo, precioMin, precioMax,
            anioMin, anioMax, kmMax, combustible, cambio,
            busqueda, potenciaMin, cilindradaMin, color,
            numeroPuertas, plazas, garantia, itv, 
            minLat, maxLat, minLng, maxLng, pageable
        );
    }

    // ── Publicar / Crear ─────────────────────────────────────────────────────

    @Transactional
    public Vehiculo publicar(Vehiculo vehiculo, Integer publicadorId) {
        Actor publicador = actorRepository.findById(publicadorId)
            .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado: " + publicadorId));
        vehiculo.setPublicador(publicador);
        vehiculo.setFechaPublicacion(LocalDateTime.now());
        if (vehiculo.getEstadoVehiculo() == null) vehiculo.setEstadoVehiculo(EstadoVehiculo.DISPONIBLE);
        asegurarCategoria(vehiculo);
        return vehiculoRepository.save(vehiculo);
    }

    @Transactional
    public Vehiculo crear(Vehiculo vehiculo, Integer publicadorId, List<MultipartFile> imagenes) {
        Actor publicador = actorRepository.findById(publicadorId)
            .orElseThrow(() -> new IllegalArgumentException("Actor no encontrado: " + publicadorId));
        vehiculo.setPublicador(publicador);
        vehiculo.setFechaPublicacion(LocalDateTime.now());
        vehiculo.setEstadoVehiculo(EstadoVehiculo.DISPONIBLE);
        asegurarCategoria(vehiculo);

        if (imagenes != null) {
            for (MultipartFile img : imagenes) {
                String url = storageService.subirImagen(img);
                if (url != null) {
                    if (vehiculo.getImagenPrincipal() == null) vehiculo.setImagenPrincipal(url);
                    else vehiculo.addImagenGaleria(url);
                }
            }
        }
        return vehiculoRepository.save(vehiculo);
    }

    // ── Actualizar ───────────────────────────────────────────────────────────

    @Transactional
    public Vehiculo update(Integer id, Vehiculo datos) {
        Vehiculo v = vehiculoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Vehiculo no encontrado: " + id));

        if (datos.getTitulo()         != null) v.setTitulo(datos.getTitulo());
        if (datos.getDescripcion()    != null) v.setDescripcion(datos.getDescripcion());
        if (datos.getPrecio()         != null) v.setPrecio(datos.getPrecio());
        if (datos.getMarca()          != null) v.setMarca(datos.getMarca());
        if (datos.getModelo()         != null) v.setModelo(datos.getModelo());
        if (datos.getAnio()           != null) v.setAnio(datos.getAnio());
        if (datos.getKilometros()     != null) v.setKilometros(datos.getKilometros());
        if (datos.getCombustible()    != null) v.setCombustible(datos.getCombustible());
        if (datos.getCambio()         != null) v.setCambio(datos.getCambio());
        if (datos.getPotencia()       != null) v.setPotencia(datos.getPotencia());
        if (datos.getCilindrada()     != null) v.setCilindrada(datos.getCilindrada());
        if (datos.getColor()          != null) v.setColor(datos.getColor());
        if (datos.getNumeroPuertas()  != null) v.setNumeroPuertas(datos.getNumeroPuertas());
        if (datos.getPlazas()         != null) v.setPlazas(datos.getPlazas());
        if (datos.getUbicacion()      != null) v.setUbicacion(datos.getUbicacion());
        if (datos.getTipoVehiculo()   != null) v.setTipoVehiculo(datos.getTipoVehiculo());
        if (datos.getEstadoVehiculo() != null) v.setEstadoVehiculo(datos.getEstadoVehiculo());
        if (datos.getTipoOferta()     != null) v.setTipoOferta(datos.getTipoOferta());
        if (datos.getCondicion()      != null) v.setCondicion(datos.getCondicion());
        if (datos.getItv()            != null) v.setItv(datos.getItv());
        if (datos.getGarantia()       != null) v.setGarantia(datos.getGarantia());
        if (datos.getImagenPrincipal()!= null) v.setImagenPrincipal(datos.getImagenPrincipal());

        return vehiculoRepository.save(v);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Integer id) {
        vehiculoRepository.findById(id).ifPresent(v -> {
            v.setEstadoVehiculo(EstadoVehiculo.ELIMINADO);
            vehiculoRepository.save(v);
        });
    }

    @Transactional
    public void deleteById(Integer id) { delete(id); }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void asegurarCategoria(Vehiculo v) {
        if (v.getCategoria() != null) return;
        Categoria cat = categoriaRepository.findBySlug("vehiculos").orElseGet(() -> {
            Categoria nueva = new Categoria("Vehiculos", "vehiculos", "directions_car");
            nueva.setColor("#1976D2");
            nueva.setOrden(5);
            nueva.setActiva(true);
            return categoriaRepository.save(nueva);
        });
        v.setCategoria(cat);
    }
}