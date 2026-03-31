package com.nexus.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.nexus.repository.AdminConfigRepository;

@SpringBootTest
public class ModerationServiceTest {

    @Autowired
    private ModerationService moderationService;

    @MockitoBean
    private AdminConfigRepository configRepository;

    @Test
    void testEsContenidoApropiado() {
        assertTrue(moderationService.esContenidoApropiado("Este es un titulo normal"));
        assertFalse(moderationService.esContenidoApropiado("vete a follar a otra parte"));
    }

    @Test
    void testValidarYBloquear() {
        // Test clean content (should not throw)
        assertDoesNotThrow(() -> moderationService.validarYBloquear("Titulo limpio", "producto", "el título"));

        // Test inappropriate content (should throw with specific message)
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            moderationService.validarYBloquear("Esto es una mierda", "producto", "el título");
        });

        assertEquals("No podemos incluir este tipo de palabras en el título al publicar un producto", exception.getMessage());
    }

    @Test
    void testValidarYBloquearVehiculo() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            moderationService.validarYBloquear("Venta de cabron en coche", "vehículo", "la descripción");
        });

        assertEquals("No podemos incluir este tipo de palabras en la descripción al publicar un vehículo", exception.getMessage());
    }

    @Test
    void testMaricon() {
        assertFalse(moderationService.esContenidoApropiado("maricon"), "Should block 'maricon'");
        assertFalse(moderationService.esContenidoApropiado("Este es un maricon"), "Should block 'maricon' in sentence");
        assertFalse(moderationService.esContenidoApropiado("maricón"), "Should block 'maricón' with tilde");
        assertFalse(moderationService.esContenidoApropiado("mariconnnn"), "Should block 'mariconnnn' with repeated last char");
    }
}
