package com.nexus.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.ChatMensaje;
import com.nexus.service.ChatService;
import com.nexus.service.BloqueoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "Chat en tiempo real con texto, imágenes, vídeos y mensajes de voz")
public class ChatController {

    @Autowired
    private ChatService chatService;
    @Autowired
    private BloqueoService bloqueoService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/historial/{roomId}")
    public ResponseEntity<List<ChatMensaje>> historial(@PathVariable String roomId) {
        return ResponseEntity.ok(chatService.getHistorial(roomId));
    }

    /**
     * Enviar un mensaje de texto por REST (fallback cuando WebSocket no está
     * conectado).
     */
    @PostMapping("/texto")
    @Operation(summary = "Enviar mensaje de texto por REST")
    public ResponseEntity<?> enviarTexto(@RequestBody Map<String, Object> payload) {
        try {
            Integer productoId = (Integer) payload.get("productoId");
            Integer remitenteId = (Integer) payload.get("remitenteId");
            Integer receptorId = (Integer) payload.get("receptorId");
            String texto = (String) payload.get("texto");

            ChatMensaje guardado = chatService.guardarMensajeTexto(
                    productoId, remitenteId, receptorId, texto);

            // Publicar en WebSocket para que el receptor lo reciba en tiempo real
            messagingTemplate.convertAndSend("/topic/chat/" + guardado.getRoomId(), guardado);
            if (receptorId != null) {
                messagingTemplate.convertAndSendToUser(
                        receptorId.toString(), "/queue/notificaciones",
                        Map.of("tipo", "NUEVO_MENSAJE", "roomId", guardado.getRoomId()));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al enviar texto: " + e.getMessage()));
        }
    }

    @GetMapping("/conversacion/{roomId}")
    public ResponseEntity<List<ChatMensaje>> conversacion(@PathVariable String roomId,
            @RequestParam Integer usuario1Id, @RequestParam Integer usuario2Id) {
        return ResponseEntity.ok(chatService.getConversacion(roomId, usuario1Id, usuario2Id));
    }

    @GetMapping("/conversaciones/{usuarioId}")
    @Operation(summary = "Bandeja de entrada: último mensaje de cada conversación")
    public ResponseEntity<List<ChatMensaje>> bandeja(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(chatService.getUltimasConversaciones(usuarioId));
    }

    @GetMapping("/no-leidos/{usuarioId}")
    public ResponseEntity<Map<String, Long>> noLeidos(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(Map.of("noLeidos", chatService.getNoLeidos(usuarioId)));
    }

    @PutMapping("/leer/{roomId}")
    public ResponseEntity<?> marcarLeidos(@PathVariable String roomId,
            @RequestParam Integer receptorId) {
        chatService.marcarLeidos(roomId, receptorId);
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/leidos",
                Map.of("receptorId", receptorId, "leido", true));
        return ResponseEntity.ok(Map.of("mensaje", "Marcados como leídos"));
    }

    /**
     * Subir imagen, vídeo o mensaje de voz al chat.
     *
     * ┌─────────────────────────────────────────────────────────────────┐
     * │ Angular (chat.component.ts) │
     * │ │
     * │ // IMAGEN o VÍDEO: │
     * │ const fd = new FormData(); │
     * │ fd.append('archivo', file); │
     * │ fd.append('tipo', file.type.startsWith('video') ? 'VIDEO':'IMAGEN'); │
     * │ http.post(`/chat/media?productoId=42&remitenteId=5&receptorId=7`, fd) │
     * │ │
     * │ // AUDIO (MediaRecorder → Blob): │
     * │ fd.append('tipo', 'AUDIO'); │
     * │ fd.append('duracion', segundos.toString()); │
     * │ fd.append('archivo', audioBlob, 'voz.webm'); │
     * └─────────────────────────────────────────────────────────────────┘
     *
     * El servidor sube a Cloudinary, guarda en BD y publica el mensaje
     * en el topic WebSocket → Angular no necesita procesar la respuesta REST.
     */
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar imagen, vídeo o mensaje de voz")
    public ResponseEntity<?> subirMedia(
            @RequestParam(required = false) Integer productoId,
            @RequestParam Integer remitenteId,
            @RequestParam Integer receptorId,
            @RequestParam String tipo,
            @RequestParam(required = false, defaultValue = "0") Integer duracion,
            @RequestPart("archivo") MultipartFile archivo) {
        
        if (bloqueoService.estaBloqueado(remitenteId, receptorId) || bloqueoService.estaBloqueado(receptorId, remitenteId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Usuario bloqueado"));
        }

        try {
            ChatMensaje guardado;
            switch (tipo.toUpperCase()) {
                case "VIDEO" ->
                    guardado = chatService.guardarMensajeVideo(productoId, remitenteId, receptorId, archivo);
                case "AUDIO" ->
                    guardado = chatService.guardarMensajeAudio(productoId, remitenteId, receptorId, archivo, duracion);
                default -> guardado = chatService.guardarMensajeImagen(productoId, remitenteId, receptorId, archivo);
            }

            // Publicar en WebSocket
            messagingTemplate.convertAndSend("/topic/chat/" + guardado.getRoomId(), guardado);
            messagingTemplate.convertAndSendToUser(
                    receptorId.toString(), "/queue/notificaciones",
                    Map.of("tipo", "NUEVO_MENSAJE", "roomId", guardado.getRoomId()));

            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al subir media: " + e.getMessage()));
        }
    }

    @PostMapping("/propuesta")
    public ResponseEntity<?> proponerPrecio(@RequestParam Integer productoId,
            @RequestParam Integer remitenteId, @RequestParam Integer receptorId,
            @RequestParam Double precioPropuesto) {
        try {
            if (bloqueoService.estaBloqueado(remitenteId, receptorId) || bloqueoService.estaBloqueado(receptorId, remitenteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Usuario bloqueado"));
            }

            ChatMensaje msg = chatService.guardarPropuestaPrecio(
                    productoId, remitenteId, receptorId, precioPropuesto);
            messagingTemplate.convertAndSend("/topic/chat/" + msg.getRoomId(), msg);
            return ResponseEntity.status(HttpStatus.CREATED).body(msg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/propuesta/{mensajeId}/responder")
    public ResponseEntity<?> responderPropuesta(@PathVariable Integer mensajeId,
            @RequestParam Boolean aceptada) {
        try {
            ChatMensaje msg = chatService.responderPropuesta(mensajeId, aceptada);
            messagingTemplate.convertAndSend("/topic/chat/" + msg.getRoomId(), msg);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/propuesta/aceptada")
    public ResponseEntity<?> getOfertaAceptada(@RequestParam Integer productoId,
            @RequestParam Integer compradorId) {
        Double precio = chatService.getPrecioNegociado(productoId, compradorId);
        return ResponseEntity.ok(Map.of("precio", precio != null ? precio : 0));
    }
}