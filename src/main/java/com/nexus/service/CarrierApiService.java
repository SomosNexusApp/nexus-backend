package com.nexus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.nexus.entity.EstadoEnvio;

/**
 * Servicio de integración con APIs reales de transportistas.
 *
 * Cada método intenta obtener el precio real via API. Si las credenciales
 * no están configuradas (vacías) o la llamada falla, cae al precio
 * de la tabla de ShippingPriceService + margen de 0,30 €.
 *
 * ──────────────────────────────────────────────────────
 * Correos: API B2B REST/SOAP — Requiere contrato comercial y acceso al portal
 * Mi Oficina
 * ──────────────────────────────────────────────────────
 */
@Service
public class CarrierApiService {

    private static final Logger log = LoggerFactory.getLogger(CarrierApiService.class);

    /**
     * Margen mínimo sobre coste real del transportista (cubre gastos operativos).
     * Si un día la API devuelve 4,00 €, al comprador se le cobran 4,30 €.
     */
    private static final double MARGEN = 0.30;

    @Value("${correos.client-id:}")
    private String correosClientId;

    @Value("${correos.client-secret:}")
    private String correosClientSecret;

    @Value("${nexus.shipping.tracking-provider-url:}")
    private String trackingProviderUrl;

    @Value("${nexus.shipping.tracking-provider-token:}")
    private String trackingProviderToken;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── Precio público ─────────────────────────────────────────────────────────

    public java.util.List<java.util.Map<String, Object>> getAvailableCarriers(double pesoKg, boolean esRecogida, double precioBaseSugerido) {
        java.util.List<java.util.Map<String, Object>> options = new java.util.ArrayList<>();

        // Solo Correos - La opción más barata
        java.util.Map<String, Object> correos = new java.util.HashMap<>();
        correos.put("id", "CORREOS");
        correos.put("nombre", "Correos (Paq Estándar)");
        
        // Precio fijo económico: 4.00€ para recogida, 4.50€ para domicilio
        double precio = esRecogida ? 4.00 : 4.50;
        
        // Si el precio sugerido es menor (p.ej. por una oferta), usamos el sugerido para no cobrar de más
        if (precioBaseSugerido > 0 && precioBaseSugerido < precio) {
            precio = precioBaseSugerido;
        }

        correos.put("precio", precio);
        correos.put("tiempoEstimado", "2-4 días hábiles");
        correos.put("logo", "assets/logos/correos.png");
        options.add(correos);

        return options;
    }

    /**
     * Obtiene el mejor precio disponible entre los tres transportistas
     * para un paquete dado. Si ninguna API está configurada devuelve null
     * y el caller debe usar la tabla de ShippingPriceService.
     *
     * @param pesoKg     peso en kg
     * @param esRecogida ¿punto de recogida?
     * @return precio en €, o null si no hay APIs configuradas
     */
    public Double getBestPrice(double pesoKg, boolean esRecogida) {
        return getPriceFromCorreos(pesoKg, esRecogida);
    }

    public Double getPriceForCarrier(String carrierId, double pesoKg, boolean esRecogida, double precioBaseSugerido) {
        Double correos = getPriceFromCorreos(pesoKg, esRecogida);
        return correos != null ? correos : precioBaseSugerido;
    }

    public TrackingResult consultarTracking(String carrierId, String numeroSeguimiento, String urlSeguimiento) {
        if (numeroSeguimiento == null || numeroSeguimiento.isBlank()) {
            return TrackingResult.sinCambios();
        }

        TrackingResult porProveedor = consultarProveedorExterno(carrierId, numeroSeguimiento);
        if (porProveedor.estado != null) {
            return porProveedor;
        }

        if (urlSeguimiento == null || urlSeguimiento.isBlank()) {
            return TrackingResult.sinCambios();
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlSeguimiento, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return TrackingResult.sinCambios();
            }

            String body = response.getBody().toLowerCase();
            if (body.contains("entregado") || body.contains("delivered")) {
                return new TrackingResult(EstadoEnvio.ENTREGADO, "Entrega confirmada por transportista");
            }
            if (body.contains("en reparto") || body.contains("out for delivery")) {
                return new TrackingResult(EstadoEnvio.EN_REPARTO, "Paquete en reparto");
            }
            if (body.contains("en tránsito") || body.contains("en transito") || body.contains("in transit")) {
                return new TrackingResult(EstadoEnvio.EN_TRANSITO, "Paquete en tránsito");
            }
            if (body.contains("admitido") || body.contains("accepted") || body.contains("clasificación")) {
                return new TrackingResult(EstadoEnvio.ENVIADO, "Paquete admitido por transportista");
            }
            return TrackingResult.sinCambios();
        } catch (Exception e) {
            log.debug("[Tracking] No se pudo leer URL de seguimiento {}: {}", urlSeguimiento, e.getMessage());
            return TrackingResult.sinCambios();
        }
    }

    private TrackingResult consultarProveedorExterno(String carrierId, String numeroSeguimiento) {
        if (trackingProviderUrl == null || trackingProviderUrl.isBlank()) {
            return TrackingResult.sinCambios();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (trackingProviderToken != null && !trackingProviderToken.isBlank()) {
                headers.setBearerAuth(trackingProviderToken);
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = String.format("%s?carrier=%s&tracking=%s",
                    trackingProviderUrl, carrierId != null ? carrierId : "CORREOS", numeroSeguimiento);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return TrackingResult.sinCambios();
            }
            String body = response.getBody().toLowerCase();
            if (body.contains("\"status\":\"delivered\"")) {
                return new TrackingResult(EstadoEnvio.ENTREGADO, "Entrega confirmada por API de tracking");
            }
            if (body.contains("\"status\":\"out_for_delivery\"")) {
                return new TrackingResult(EstadoEnvio.EN_REPARTO, "Paquete en reparto");
            }
            if (body.contains("\"status\":\"in_transit\"")) {
                return new TrackingResult(EstadoEnvio.EN_TRANSITO, "Paquete en tránsito");
            }
            if (body.contains("\"status\":\"accepted\"")) {
                return new TrackingResult(EstadoEnvio.ENVIADO, "Paquete admitido por transportista");
            }
            return TrackingResult.sinCambios();
        } catch (Exception e) {
            log.debug("[Tracking] Provider externo no disponible: {}", e.getMessage());
            return TrackingResult.sinCambios();
        }
    }

    public static class TrackingResult {
        private final EstadoEnvio estado;
        private final String descripcion;

        public TrackingResult(EstadoEnvio estado, String descripcion) {
            this.estado = estado;
            this.descripcion = descripcion;
        }

        public static TrackingResult sinCambios() {
            return new TrackingResult(null, null);
        }

        public EstadoEnvio getEstado() {
            return estado;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    // ── Correos ────────────────────────────────────────────────────────────────

    /**
     * Precio real de Correos (Paq Estándar / Paq Mini).
     * Requiere: correos.client-id y correos.client-secret en application.properties
     *
     * Nota: La API de Correos para empresas requiere firma de contrato.
     * Te proporcionarán credenciales B2B y la URL de desarrollo (pre-producción)
     * específica.
     * Producto recomendado paquetes < 5 kg: PAQMINI / PAQESTANDAR
     * Autenticación habitual: OAuth2 client_credentials o Basic Auth según el
     * servicio
     *
     * @return precio con margen o null si no configurado
     */
    public Double getPriceFromCorreos(double pesoKg, boolean esRecogida) {
        if (correosClientId == null || correosClientId.isBlank()) {
            return null; // Sin credenciales → fallback a tabla
        }

        try {
            // TODO: Implementar cuando tengas credenciales de Correos Click&Send
            // 1. POST https://apisandbox.correos.es/oauth/token
            // (grant_type=client_credentials)
            // → bearer token
            // 2. POST https://apisandbox.correos.es/restcita/precio/v1
            // body: { "modalidad": esRecogida ? "PAQMINI" : "PAQESTANDAR",
            // "remitente": {...}, "destinatario": {...},
            // "pesos": [{ "codigo": "P", "valor": pesoKg }] }
            // → { "importeTotal": 4.10 }
            // 3. return response.importeTotal + MARGEN;
            log.info("[Correos] Credenciales configuradas pero integración pendiente — usando tabla");
            return null;
        } catch (Exception e) {
            log.warn("[Correos] Error llamando a la API: {} — usando tabla", e.getMessage());
            return null;
        }
    }

}
