package com.nexus.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.nexus.dto.PuntoRecogidaDTO;

/**
 * Puntos de entrega / oficinas de transportistas por ciudad (datos orientativos, España).
 * Sin API externa: evita claves y costes; el vendedor confirma siempre en la paquetería.
 */
@Service
public class PuntoRecogidaService {

    private static final Map<String, List<PuntoRecogidaDTO>> POR_CIUDAD = Map.ofEntries(
            Map.entry("madrid", List.of(
                    new PuntoRecogidaDTO("Correos — Cibeles", "Plaza Cibeles 1", "Madrid",
                            "Lun–Vie 9:00–20:30", "CORREOS"),
                    new PuntoRecogidaDTO("SEUR — Plaza Castilla", "Paseo Castellana 189", "Madrid",
                            "Lun–Vie 10:00–19:00", "SEUR"),
                    new PuntoRecogidaDTO("MRW — Usera", "Av. Rafaela Ybarra 64", "Madrid",
                            "Lun–Vie 9:00–18:00", "MRW"))),
            Map.entry("barcelona", List.of(
                    new PuntoRecogidaDTO("Correos — Universitat", "Ronda Universitat 31", "Barcelona",
                            "Lun–Vie 8:30–20:30", "CORREOS"),
                    new PuntoRecogidaDTO("SEUR — Eixample", "Carrer Mallorca 272", "Barcelona",
                            "Lun–Vie 9:00–19:00", "SEUR"))),
            Map.entry("valencia", List.of(
                    new PuntoRecogidaDTO("Correos — Ayuntamiento", "Plaza Ayuntamiento 17", "Valencia",
                            "Lun–Vie 9:00–20:00", "CORREOS"),
                    new PuntoRecogidaDTO("MRW — Campanar", "Carrer de Trafalgar 38", "Valencia",
                            "Lun–Vie 9:00–18:00", "MRW"))),
            Map.entry("sevilla", List.of(
                    new PuntoRecogidaDTO("Correos — Alfonso XII", "Av. República Argentina 16", "Sevilla",
                            "Lun–Vie 9:00–20:00", "CORREOS"))),
            Map.entry("bilbao", List.of(
                    new PuntoRecogidaDTO("Correos — Abando", "Gran Vía 25", "Bilbao",
                            "Lun–Vie 9:00–20:00", "CORREOS"))),
            Map.entry("malaga", List.of(
                    new PuntoRecogidaDTO("Correos — Alameda", "Alameda Principal 23", "Málaga",
                            "Lun–Vie 9:00–20:00", "CORREOS"))),
            Map.entry("zaragoza", List.of(
                    new PuntoRecogidaDTO("Correos — Paseo Independencia", "Paseo Independencia 24", "Zaragoza",
                            "Lun–Vie 9:00–20:00", "CORREOS"))));

    public List<PuntoRecogidaDTO> buscarPorCiudadOCp(String ciudadOCp) {
        if (ciudadOCp == null || ciudadOCp.isBlank()) {
            return List.of(porDefecto());
        }
        String key = ciudadOCp.toLowerCase(Locale.ROOT).trim();
        for (String k : POR_CIUDAD.keySet()) {
            if (key.contains(k)) {
                return new ArrayList<>(POR_CIUDAD.get(k));
            }
        }
        return List.of(porDefecto());
    }

    private PuntoRecogidaDTO porDefecto() {
        return new PuntoRecogidaDTO("Oficina Correos más cercana",
                "Busca en correos.es tu código postal", "España",
                "Consulta horario en la web del operador", "CORREOS");
    }
}
