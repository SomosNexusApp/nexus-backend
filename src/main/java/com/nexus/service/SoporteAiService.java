package com.nexus.service;
 
import jakarta.annotation.PostConstruct;

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
            Eres el asistente virtual de Nexus, el marketplace líder de compra-venta en España.
            Tu objetivo es ayudar a los usuarios con dudas sobre la plataforma.
            
            CONOCIMIENTO BASE (Centro de Ayuda):
            1. PAGOS: Usamos Stripe. El dinero se retiene de forma segura y se libera al vendedor solo cuando el comprador confirma que el producto está OK.
            2. ENVÍOS: Trabajamos con Correos, SEUR y MRW. Al vender, recibes un código de envío en la sección 'Mis Ventas'. El vendedor tiene 5 días para depositar el paquete.
            3. COMISIONES: Publicar es gratis. Nexus cobra una pequeña comisión de gestión al comprador por el servicio de Protección al Comprador.
            4. DEVOLUCIONES: Si el producto no coincide con la descripción, el comprador tiene 48h tras la recepción para abrir una disputa.
            5. SEGURIDAD: Nunca des tu teléfono o email. Todas las transacciones deben hacerse dentro de Nexus para estar protegidas.
            
            REGLAS DE RESPUESTA:
            - Responde SIEMPRE en español, con tono profesional pero cercano.
            - Sé breve (máximo 4-5 frases).
            - No uses emojis ni símbolos.
            - Si no sabes algo, remite a soporte humano (escalación por email).
            """;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${nexus.soporte.gemini-api-key:}")
    private String geminiApiKey;
 
    @Value("${nexus.soporte.gemini-api-version:v1}")
    private String apiVersion;
 
    @Value("${nexus.soporte.gemini-model:gemini-1.5-flash}")
    private String modelName;
 
    @Value("${nexus.soporte.groq-api-key:}")
    private String groqApiKey;
 
    @Value("${nexus.soporte.groq-model:llama3-8b-8192}")
    private String groqModel;
 
    @PostConstruct
    public void init() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            log.info("Iniciando depuración de modelos Gemini...");
            new Thread(this::debugModelos).start();
        }
    }
 
    private void debugModelos() {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + geminiApiKey.trim();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Lista de modelos Gemini disponibles: {}", res.body());
        } catch (Exception e) {
            log.warn("No se pudo listar los modelos: {}", e.getMessage());
        }
    }

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
        try {
            String responseText;
            if (groqApiKey != null && !groqApiKey.isBlank()) {
                responseText = callGroq(ultimoUsuario, historialUltimos);
            } else if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                responseText = callGemini(ultimoUsuario, historialUltimos);
            } else {
                log.warn("No hay API Key de IA configurada (Gemini/Groq).");
                return new SoporteAiResponse(respuestaSinApi(ultimoUsuario));
            }

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
        String cleanKey = geminiApiKey.trim();
        // Usamos configuración externalizada
        String url = String.format("https://generativelanguage.googleapis.com/%s/models/%s:generateContent?key=%s",
                apiVersion, modelName, cleanKey);

        StringBuilder ctx = new StringBuilder();
        int n = historialUltimos.size();
        int from = Math.max(0, n - 10);
        for (int i = from; i < n; i++) {
            ctx.append(historialUltimos.get(i)).append("\n");
        }
        String userBlock = "Contexto reciente:\n" + ctx + "\nÚltimo mensaje del usuario:\n" + ultimoUsuario;

        ObjectNode root = mapper.createObjectNode();
        
        // El campo system_instruction puede fallar en v1 dependiendo del modelo/región
        // Para máxima compatibilidad, si usamos v1, inyectamos las instrucciones en el bloque del usuario
        boolean useNativeSystem = apiVersion.equalsIgnoreCase("v1beta");

        if (useNativeSystem) {
            ObjectNode sysInst = mapper.createObjectNode();
            ArrayNode sysParts = sysInst.putArray("parts");
            sysParts.addObject().put("text", SYSTEM.trim());
            root.set("system_instruction", sysInst);
        }

        ArrayNode contents = root.putArray("contents");
        ObjectNode turn = contents.addObject();
        turn.put("role", "user");
        
        String finalPrompt = userBlock;
        if (!useNativeSystem) {
            finalPrompt = "INSTRUCCIONES DE SISTEMA:\n" + SYSTEM.trim() + "\n\n---\n\n" + userBlock;
        }
        
        turn.putArray("parts").addObject().put("text", finalPrompt);

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
        JsonNode candidates = tree.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
            log.error("Gemini Error: No hay candidatos en la respuesta. Body: {}", res.body());
            throw new RuntimeException("Gemini no devolvió respuesta (posible bloqueo por seguridad)");
        }
        
        JsonNode text = candidates.path(0).path("content").path("parts").path(0).path("text");
        if (text.isMissingNode()) {
            log.error("Gemini Error: Nodo 'text' no encontrado. Body: {}", res.body());
            throw new RuntimeException("Gemini no devolvió texto en el primer candidato");
        }
        
        String cleanedText = text.asText().trim();
        log.info("Gemini respondió correctamente: {}...", (cleanedText.length() > 50 ? cleanedText.substring(0, 50) : cleanedText));
        return cleanedText;
    }

    private String callGroq(String ultimoUsuario, List<String> historialUltimos) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";
        
        StringBuilder ctx = new StringBuilder();
        int from = Math.max(0, historialUltimos.size() - 10);
        for (int i = from; i < historialUltimos.size(); i++) {
            ctx.append(historialUltimos.get(i)).append("\n");
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("model", groqModel);
        ArrayNode messages = root.putArray("messages");
        
        messages.addObject().put("role", "system").put("content", SYSTEM.trim());
        messages.addObject().put("role", "user").put("content", "Contexto:\n" + ctx + "\nMensaje: " + ultimoUsuario);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() >= 400) {
            log.error("Groq Error Detallado - Status: {}, Body: {}", res.statusCode(), res.body());
            throw new RuntimeException("Groq HTTP " + res.statusCode() + " - " + res.body());
        }

        JsonNode tree = mapper.readTree(res.body());
        String text = tree.path("choices").path(0).path("message").path("content").asText().trim();
        log.info("Groq respondió correctamente.");
        return text;
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
