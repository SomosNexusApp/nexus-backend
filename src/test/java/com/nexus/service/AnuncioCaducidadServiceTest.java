package com.nexus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.EstadoVehiculo;
import com.nexus.entity.Producto;
import com.nexus.entity.Vehiculo;
import com.nexus.repository.ProductoRepository;
import com.nexus.repository.VehiculoRepository;

@ExtendWith(MockitoExtension.class)
class AnuncioCaducidadServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private AnuncioCaducidadService anuncioCaducidadService;

    @BeforeEach
    void setVida() {
        ReflectionTestUtils.setField(anuncioCaducidadService, "vidaDias", 180);
    }

    @Test
    void ejecutarDiario_marcaProductoComoExpiradoSiCaducidadPasada() {
        Producto p = new Producto();
        p.setTitulo("Test");
        p.setEstado(EstadoProducto.DISPONIBLE);
        p.setFechaPublicacion(LocalDateTime.now().minusDays(200));
        p.setFechaCaducidad(LocalDateTime.now().minusDays(2));

        when(productoRepository.findByEstadoIn(any())).thenReturn(List.of(p));
        when(vehiculoRepository.findByEstadoVehiculoIn(any())).thenReturn(List.of());

        anuncioCaducidadService.ejecutarDiario();

        verify(productoRepository).save(argThat(pr -> pr.getEstado() == EstadoProducto.EXPIRADO));
    }

    @Test
    void ejecutarDiario_marcaVehiculoComoExpiradoSiCaducidadPasada() {
        Vehiculo v = new Vehiculo();
        v.setTitulo("Coche");
        v.setEstadoVehiculo(EstadoVehiculo.DISPONIBLE);
        v.setFechaPublicacion(LocalDateTime.now().minusDays(200));
        v.setFechaCaducidad(LocalDateTime.now().minusDays(1));

        when(productoRepository.findByEstadoIn(any())).thenReturn(List.of());
        when(vehiculoRepository.findByEstadoVehiculoIn(any())).thenReturn(List.of(v));

        anuncioCaducidadService.ejecutarDiario();

        verify(vehiculoRepository).save(argThat(vh -> vh.getEstadoVehiculo() == EstadoVehiculo.EXPIRADO));
    }

    @Test
    void ejecutarDiario_inicializaFechaCaducidadSiNull() {
        Producto p = new Producto();
        p.setTitulo("Sin fecha");
        p.setEstado(EstadoProducto.DISPONIBLE);
        LocalDateTime pub = LocalDateTime.now().minusDays(10);
        p.setFechaPublicacion(pub);
        p.setFechaCaducidad(null);

        when(productoRepository.findByEstadoIn(any())).thenReturn(List.of(p));
        when(vehiculoRepository.findByEstadoVehiculoIn(any())).thenReturn(List.of());

        anuncioCaducidadService.ejecutarDiario();

        verify(productoRepository).save(argThat(pr -> pr.getFechaCaducidad() != null
                && pr.getFechaCaducidad().equals(pub.plusDays(180))));
        assertEquals(EstadoProducto.DISPONIBLE, p.getEstado());
    }
}
