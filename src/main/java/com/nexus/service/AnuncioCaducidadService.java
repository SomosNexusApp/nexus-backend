package com.nexus.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.EstadoOferta;
import com.nexus.entity.EstadoVehiculo;
import com.nexus.entity.Producto;
import com.nexus.entity.Oferta;
import com.nexus.entity.Vehiculo;
import com.nexus.repository.ProductoRepository;
import com.nexus.repository.VehiculoRepository;

@Service
public class AnuncioCaducidadService {

    private static final int[] HITOS_DIAS = { 30, 14, 7, 1 };

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private com.nexus.repository.OfertaRepository ofertaRepository;
    @Autowired
    private NotificacionService notificacionService;

    @Value("${nexus.anuncio.vida-dias:180}")
    private int vidaDias;

    @Transactional
    public void ejecutarDiario() {
        procesarProductos();
        procesarVehiculos();
        limpiarProductosVendidos();
        limpiarOfertasAgotadas();
        limpiarExpirados();
    }

    private void procesarProductos() {
        List<Producto> lista = productoRepository.findByEstadoIn(
                List.of(EstadoProducto.DISPONIBLE, EstadoProducto.PAUSADO));
        for (Producto p : lista) {
            if (p.getFechaPublicacion() == null) continue;
            if (p.getFechaCaducidad() == null) {
                p.setFechaCaducidad(p.getFechaPublicacion().plusDays(vidaDias));
                productoRepository.save(p);
                continue;
            }
            LocalDate hoy = LocalDate.now();
            long days = ChronoUnit.DAYS.between(hoy, p.getFechaCaducidad().toLocalDate());
            if (days < 0) {
                p.setEstado(EstadoProducto.EXPIRADO);
                productoRepository.save(p);
                continue;
            }
            Integer u = p.getUltimoAvisoCaducidadDias();
            for (int m : HITOS_DIAS) {
                if (days <= m && (u == null || u > m)) {
                    String msg = days == 0
                            ? "Tu anuncio «" + p.getTitulo() + "» caduca hoy. Reactívalo desde Mis artículos."
                            : "Tu anuncio «" + p.getTitulo() + "» caduca en " + Math.max(days, 1)
                                    + " día(s). Evita que se oculte: reactívalo desde el perfil.";
                    notificacionService.notificarCaducidadAnuncio(p.getVendedor().getId(), p.getTitulo(), msg,
                            "/perfil?tab=productos");
                    p.setUltimoAvisoCaducidadDias(m);
                    productoRepository.save(p);
                    break;
                }
            }
        }
    }

    private void procesarVehiculos() {
        List<Vehiculo> lista = vehiculoRepository.findByEstadoVehiculoIn(
                List.of(EstadoVehiculo.DISPONIBLE, EstadoVehiculo.PAUSADO));
        for (Vehiculo v : lista) {
            if (v.getFechaPublicacion() == null) continue;
            if (v.getFechaCaducidad() == null) {
                v.setFechaCaducidad(v.getFechaPublicacion().plusDays(vidaDias));
                vehiculoRepository.save(v);
                continue;
            }
            LocalDate hoy = LocalDate.now();
            long days = ChronoUnit.DAYS.between(hoy, v.getFechaCaducidad().toLocalDate());
            if (days < 0) {
                v.setEstadoVehiculo(EstadoVehiculo.EXPIRADO);
                vehiculoRepository.save(v);
                continue;
            }
            Integer u = v.getUltimoAvisoCaducidadDias();
            for (int m : HITOS_DIAS) {
                if (days <= m && (u == null || u > m)) {
                    String msg = days == 0
                            ? "Tu anuncio de vehículo «" + v.getTitulo() + "» caduca hoy."
                            : "Tu vehículo «" + v.getTitulo() + "» caduca en " + Math.max(days, 1)
                                    + " día(s). Reactívalo desde Mis artículos.";
                    notificacionService.notificarCaducidadAnuncio(v.getPublicador().getId(), v.getTitulo(), msg,
                            "/perfil?tab=vehiculos");
                    v.setUltimoAvisoCaducidadDias(m);
                    vehiculoRepository.save(v);
                    break;
                }
            }
        }
    }

    private void limpiarProductosVendidos() {
        // Obtenemos productos vendidos. 
        // Nota: asumo que productoRepository tiene findByEstado(EstadoProducto)
        List<Producto> vendidos = productoRepository.findByEstado(EstadoProducto.VENDIDO);
        java.time.LocalDateTime limite = java.time.LocalDateTime.now().minusDays(14);
        
        for (Producto p : vendidos) {
            if (p.getFechaVenta() != null && p.getFechaVenta().isBefore(limite)) {
                p.setEstado(EstadoProducto.ELIMINADO);
                productoRepository.save(p);
            }
        }
    }

    private void limpiarOfertasAgotadas() {
        List<Oferta> agotadas = ofertaRepository.findByEstado(EstadoOferta.AGOTADA);
        java.time.LocalDateTime limite = java.time.LocalDateTime.now().minusDays(14);
        
        for (Oferta o : agotadas) {
            if (o.getFechaFinalizado() != null && o.getFechaFinalizado().isBefore(limite)) {
                o.setEstado(EstadoOferta.ELIMINADA);
                ofertaRepository.save(o);
            }
        }
    }

    private void limpiarExpirados() {
        java.time.LocalDateTime limite = java.time.LocalDateTime.now().minusDays(14);

        // Productos
        List<Producto> productosExpirados = productoRepository.findByEstado(EstadoProducto.EXPIRADO);
        for (Producto p : productosExpirados) {
            if (p.getFechaCaducidad() != null && p.getFechaCaducidad().isBefore(limite)) {
                p.setEstado(EstadoProducto.ELIMINADO);
                productoRepository.save(p);
            }
        }

        // Vehículos
        List<Vehiculo> vehiculosExpirados = vehiculoRepository.findByEstadoVehiculo(EstadoVehiculo.EXPIRADO);
        for (Vehiculo v : vehiculosExpirados) {
            if (v.getFechaCaducidad() != null && v.getFechaCaducidad().isBefore(limite)) {
                v.setEstadoVehiculo(EstadoVehiculo.ELIMINADO);
                vehiculoRepository.save(v);
            }
        }
    }
}
