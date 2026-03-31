package com.nexus.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * IA opcional (Google Gemini) — clave en {@code nexus.soporte.gemini-api-key}.
 */
@Service
public class SoporteAiService {

    private static final Logger log = LoggerFactory.getLogger(SoporteAiService.class);

    private static final String SYSTEM = """
            Eres el asistente virtual de Nexus, marketplace de compra-venta entre particulares en España.
            Responde SIEMPRE en español, con tono cercano y profesional. Sé breve (máximo 6 frases).
            Ayuda con: envíos, pagos seguros, devoluciones, cómo publicar, seguridad.
            No inventes políticas legales concretas; remite a la ayuda de la app.
            No uses emojis ni símbolos decorativos.
            """;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${nexus.soporte.gemini-api-key:}")
    private String geminiApiKey;

    public static class SoporteAiResponse {
        private String contenido;
        private String tipoContenido;
        private Integer referenciaId;

        public SoporteAiResponse(String contenido) { this.contenido = contenido; }
        public String getContenido() { return contenido; }
        public void setContenido(String contenido) { this.contenido = contenido; }
        public String getTipoContenido() { return tipoContenido; }
        public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }
        public Integer getReferenciaId() { return referenciaId; }
        public void setReferenciaId(Integer referenciaId) { this.referenciaId = referenciaId; }
    }

    public SoporteAiResponse responder(String ultimoUsuario, List<String> historialUltimos) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return new SoporteAiResponse(respuestaSinApi(ultimoUsuario));
        }
        try {
            String responseText = callGemini(ultimoUsuario, historialUltimos);
            SoporteAiResponse resObj = new SoporteAiResponse(responseText);
            
            // Simple heuristic to attach a card if the AI mentions a specific product/offer ID
            // In a real scenario, we'd use function calling or a structured prompt.
            if (responseText.toLowerCase().contains("producto #")) {
                 parseAndSet(resObj, "PRODUCTO", responseText, "producto #");
            } else if (responseText.toLowerCase().contains("oferta #")) {
                 parseAndSet(resObj, "OFERTA", responseText, "oferta #");
            } else if (responseText.toLowerCase().contains("vehículo #")) {
                 parseAndSet(resObj, "VEHICULO", responseText, "vehículo #");
            }

            return resObj;
        } catch (Exception e) {
            log.warn("Gemini: {}", e.getMessage());
            return new SoporteAiResponse(respuestaSinApi(ultimoUsuario));
        }
    }

    private void parseAndSet(SoporteAiResponse res, String tipo, String text, String marker) {
        try {
            int idx = text.toLowerCase().indexOf(marker) + marker.length();
            String sub = text.substring(idx).split("[^0-9]")[0];
            if (!sub.isBlank()) {
                res.setTipoContenido(tipo);
                res.setReferenciaId(Integer.parseInt(sub));
            }
        } catch (Exception ignored) {}
    }

    private String callGemini(String ultimoUsuario, List<String> historialUltimos) throws Exception {
        // Usamos gemini-flash-latest por ser el alias estable más compatible
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                + geminiApiKey;

        StringBuilder ctx = new StringBuilder();
        int n = historialUltimos.size();
        int from = Math.max(0, n - 10);
        for (int i = from; i < n; i++) {
            ctx.append(historialUltimos.get(i)).append("\n");
        }
        String userBlock = "Contexto reciente:\n" + ctx + "\nÚltimo mensaje del usuario:\n" + ultimoUsuario;

        ObjectNode root = mapper.createObjectNode();
        ObjectNode sysInst = mapper.createObjectNode();
        ArrayNode sysParts = sysInst.putArray("parts");
        sysParts.addObject().put("text", SYSTEM.trim());
        root.set("system_instruction", sysInst);

        ArrayNode contents = root.putArray("contents");
        ObjectNode turn = contents.addObject();
        turn.put("role", "user");
        turn.putArray("parts").addObject().put("text", userBlock);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() >= 400) {
            log.error("Gemini Error - Status: {}, Body: {}", res.statusCode(), res.body());
            throw new RuntimeException("Gemini HTTP " + res.statusCode() + ": " + res.body());
        }
        JsonNode tree = mapper.readTree(res.body());
        JsonNode text = tree.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        return text.asText().trim();
    }

    private String respuestaSinApi(String msg) {
        String m = msg.toLowerCase();
        if (m.contains("envio") || m.contains("envío") || m.contains("paquete")) {
            return "Para envíos: entra en Mis ventas tras una compra, abre la pantalla de envío y sigue el código SHIP-… "
                    + "Tienes un plazo para depositar el paquete en Correos, SEUR o MRW. ¿Algo más concreto?";
        }
        if (m.contains("pago") || m.contains("stripe") || m.contains("dinero")) {
            return "Los pagos en Nexus pasan por un sistema seguro; el dinero se libera al confirmar la entrega. "
                    + "Si ves un cargo extraño, revisa el email de confirmación o escribe a soporte.";
        }
        if (m.contains("devoluc")) {
            return "Las devoluciones se gestionan desde el pedido: el comprador puede abrir una solicitud y el vendedor recibe aviso.";
        }
        return "Gracias por escribir. Puedo orientarte sobre envíos, pagos y cuenta. ¿En qué más puedo ayudarte?";
    }

    public boolean pideHumanoExplicito(String text) {
        if (text == null)
            return false;
        String t = text.toLowerCase();
        return t.contains("agente") || t.contains("humano") || t.contains("persona real")
                || t.contains("operador") || t.contains("hablar con alguien") || t.contains("teléfono")
                || t.contains("telefono");
    }
}
