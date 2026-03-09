package com.nexus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 * SEUR: API REST — Requiere contrato comercial (https://www.seur.com)
 * MRW: SOAP/REST — Requiere contrato comercial (https://www.mrw.es)
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

    // ── SEUR ──────────────────────────────────────────────────────────────────

    @Value("${seur.user:}")
    private String seurUser;

    @Value("${seur.password:}")
    private String seurPassword;

    @Value("${seur.cif:}")
    private String seurCif;

    // ── MRW ───────────────────────────────────────────────────────────────────

    @Value("${mrw.codigo-abonado:}")
    private String mrwCodigoAbonado;

    @Value("${mrw.codigo-departamento:}")
    private String mrwCodigoDepartamento;

    // ── Precio público ─────────────────────────────────────────────────────────

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
        Double correos = getPriceFromCorreos(pesoKg, esRecogida);
        Double seur = getPriceFromSeur(pesoKg, esRecogida);
        Double mrw = getPriceFromMrw(pesoKg, esRecogida);

        Double mejor = null;
        if (correos != null)
            mejor = correos;
        if (seur != null && (mejor == null || seur < mejor))
            mejor = seur;
        if (mrw != null && (mejor == null || mrw < mejor))
            mejor = mrw;

        return mejor;
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

    // ── SEUR ───────────────────────────────────────────────────────────────────

    /**
     * Precio real de SEUR.
     * Requiere: seur.user, seur.password, seur.cif en application.properties
     *
     * Endpoint real: GET https://api.seur.com/private/GetPrice
     * Autenticación: Basic Auth (user:password)
     * Parámetros: CIF, peso, servicio (ESTANDAR / PUNTO_RED)
     *
     * @return precio con margen o null si no configurado
     */
    public Double getPriceFromSeur(double pesoKg, boolean esRecogida) {
        if (seurUser == null || seurUser.isBlank()) {
            return null;
        }

        try {
            // TODO: Implementar cuando tengas credenciales SEUR
            // GET https://api.seur.com/private/GetPrice
            // ?CIF={seurCif}&PESO={pesoGr}&TIPO_SERVICIO={esRecogida?"PUNTO_RED":"ESTANDAR"}
            // Authorization: Basic Base64(user:password)
            // → XML/JSON con PVP
            // return pvp + MARGEN;
            log.info("[SEUR] Credenciales configuradas pero integración pendiente — usando tabla");
            return null;
        } catch (Exception e) {
            log.warn("[SEUR] Error llamando a la API: {} — usando tabla", e.getMessage());
            return null;
        }
    }

    // ── MRW ────────────────────────────────────────────────────────────────────

    /**
     * Precio real de MRW.
     * Requiere: mrw.codigo-abonado, mrw.codigo-departamento en
     * application.properties
     *
     * WSDL: https://www.mrw.es/Franquicia/Servicios/ServicioMRW.svc?wsdl
     * Método SOAP: GetEnvioFromCGM o CalculatePriceEnvio
     * Autenticación: CodigoAbonado + CodigoDepartamento en header SOAP
     *
     * @return precio con margen o null si no configurado
     */
    public Double getPriceFromMrw(double pesoKg, boolean esRecogida) {
        if (mrwCodigoAbonado == null || mrwCodigoAbonado.isBlank()) {
            return null;
        }

        try {
            // TODO: Implementar cuando tengas credenciales MRW
            // Requiere cliente SOAP (javax.xml.ws / cxf)
            // <CalculatePriceEnvio>
            // <CodigoAbonado>mrwCodigoAbonado</CodigoAbonado>
            // <PesoEnvio>pesoGr</PesoEnvio>
            // <TipoEnvio>DOMICILIO o AGENCIA</TipoEnvio>
            // </CalculatePriceEnvio>
            // → <ImporteEnvio>5.20</ImporteEnvio>
            // return importeEnvio + MARGEN;
            log.info("[MRW] Credenciales configuradas pero integración pendiente — usando tabla");
            return null;
        } catch (Exception e) {
            log.warn("[MRW] Error llamando a la API: {} — usando tabla", e.getMessage());
            return null;
        }
    }
}
