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

// servicio que gestiona la caducidad de anuncios (productos, vehiculos y ofertas)
// lo ejecuta el scheduler cada dia a las 8:00
@Service
public class AnuncioCaducidadService {

    // los hitos en dias en los que mandamos aviso al usuario antes de que caduque
    // enviamos notificacion cuando quedan 30, 14, 7 y 1 dia(s)
    private static final int[] HITOS_DIAS = { 30, 14, 7, 1 };

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private com.nexus.repository.OfertaRepository ofertaRepository;
    @Autowired
    private NotificacionService notificacionService;

    // la vida de un anuncio en dias, configurable desde application.properties
    // por defecto son 180 dias (6 meses aprox)
    @Value("${nexus.anuncio.vida-dias:180}")
    private int vidaDias;

    // punto de entrada: el scheduler llama a este metodo cada dia
    @Transactional
    public void ejecutarDiario() {
        procesarProductos();      // avisos y caducidad de productos
        procesarVehiculos();      // avisos y caducidad de vehiculos
        limpiarProductosVendidos(); // borra logicamente los productos vendidos hace mas de 14 dias
        limpiarOfertasAgotadas(); // igual pero para ofertas
        limpiarExpirados();       // borra lo que lleva expirado mas de 14 dias sin reactivarse
    }

    private void procesarProductos() {
        // solo miramos los que estan disponibles o pausados (los demas no nos interesan)
        List<Producto> lista = productoRepository.findByEstadoIn(
                List.of(EstadoProducto.DISPONIBLE, EstadoProducto.PAUSADO));
        for (Producto p : lista) {
            if (p.getFechaPublicacion() == null) continue; // por si acaso hay alguno sin fecha
            if (p.getFechaCaducidad() == null) {
                // si no tiene fecha de caducidad la calculamos a partir de la publicacion
                p.setFechaCaducidad(p.getFechaPublicacion().plusDays(vidaDias));
                productoRepository.save(p);
                continue;
            }
            LocalDate hoy = LocalDate.now();
            long days = ChronoUnit.DAYS.between(hoy, p.getFechaCaducidad().toLocalDate());
            if (days < 0) {
                // ya caduco: cambiamos el estado a EXPIRADO
                p.setEstado(EstadoProducto.EXPIRADO);
                productoRepository.save(p);
                continue;
            }
            Integer u = p.getUltimoAvisoCaducidadDias();
            for (int m : HITOS_DIAS) {
                // si los dias restantes son menores o iguales al hito y aun no hemos avisado en ese hito
                if (days <= m && (u == null || u > m)) {
                    String msg = days == 0
                            ? "Tu anuncio «" + p.getTitulo() + "» caduca hoy. Reactívalo desde Mis artículos."
                            : "Tu anuncio «" + p.getTitulo() + "» caduca en " + Math.max(days, 1)
                                    + " día(s). Evita que se oculte: reactívalo desde el perfil.";
                    notificacionService.notificarCaducidadAnuncio(p.getVendedor().getId(), p.getTitulo(), msg,
                            "/perfil?tab=productos");
                    p.setUltimoAvisoCaducidadDias(m); // guardamos este hito para no repetir el aviso
                    productoRepository.save(p);
                    break; // solo un aviso por ejecucion aunque esten varios hitos pendientes
                }
            }
        }
    }

    // igual que procesarProductos pero para vehiculos
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

    // limpia productos que llevan mas de 14 dias en estado VENDIDO
    // es una eliminacion logica (soft delete): cambia el estado a ELIMINADO
    // el usuario no los ve pero los datos siguen en la bbdd por si hacen falta
    private void limpiarProductosVendidos() {
        List<Producto> vendidos = productoRepository.findByEstado(EstadoProducto.VENDIDO);
        java.time.LocalDateTime limite = java.time.LocalDateTime.now().minusDays(14);
        
        for (Producto p : vendidos) {
            if (p.getFechaVenta() != null && p.getFechaVenta().isBefore(limite)) {
                p.setEstado(EstadoProducto.ELIMINADO);
                productoRepository.save(p);
            }
        }
    }

    // igual que limpiarProductosVendidos pero para ofertas en estado AGOTADA
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

    // limpia lo que lleva expirado mas de 14 dias sin que el usuario lo reactive
    // el flujo es: DISPONIBLE -> EXPIRADO (dia 0) -> ELIMINADO (dia +14 si no se reactivo)
    private void limpiarExpirados() {
        java.time.LocalDateTime limite = java.time.LocalDateTime.now().minusDays(14);

        // productos expirados
        List<Producto> productosExpirados = productoRepository.findByEstado(EstadoProducto.EXPIRADO);
        for (Producto p : productosExpirados) {
            if (p.getFechaCaducidad() != null && p.getFechaCaducidad().isBefore(limite)) {
                p.setEstado(EstadoProducto.ELIMINADO);
                productoRepository.save(p);
            }
        }

        // vehiculos expirados (misma logica)
        List<Vehiculo> vehiculosExpirados = vehiculoRepository.findByEstadoVehiculo(EstadoVehiculo.EXPIRADO);
        for (Vehiculo v : vehiculosExpirados) {
            if (v.getFechaCaducidad() != null && v.getFechaCaducidad().isBefore(limite)) {
                v.setEstadoVehiculo(EstadoVehiculo.ELIMINADO);
                vehiculoRepository.save(v);
            }
        }
    }
}
