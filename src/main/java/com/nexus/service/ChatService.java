package com.nexus.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.entity.*;
import com.nexus.repository.ChatMensajeRepository;

@Service
public class ChatService {

    @Autowired
    private ChatMensajeRepository chatMensajeRepository;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private StorageService storageService;

    @Transactional
    public ChatMensaje guardarMensajeTexto(Integer productoId, Integer remitenteId,
            Integer receptorId, String texto) {
        ChatMensaje msg = buildBase(productoId, remitenteId, receptorId);
        msg.setTexto(texto);
        msg.setTipo(TipoMensaje.TEXTO);
        return chatMensajeRepository.save(msg);
    }

    @Transactional
    public ChatMensaje guardarMensajeImagen(Integer productoId, Integer remitenteId,
            Integer receptorId, MultipartFile archivo) {
        String url = storageService.subirImagen(archivo);
        if (url == null)
            throw new RuntimeException("Error al subir imagen al chat");
        ChatMensaje msg = buildBase(productoId, remitenteId, receptorId);
        msg.setMediaUrl(url);
        msg.setTipo(TipoMensaje.IMAGEN);
        return chatMensajeRepository.save(msg);
    }

    @Transactional
    public ChatMensaje guardarMensajeGif(Integer productoId, Integer remitenteId,
            Integer receptorId, String gifUrl) {
        ChatMensaje msg = buildBase(productoId, remitenteId, receptorId);
        msg.setMediaUrl(gifUrl);
        msg.setTipo(TipoMensaje.IMAGEN);
        return chatMensajeRepository.save(msg);
    }

    @Transactional
    public ChatMensaje guardarMensajeVideo(Integer productoId, Integer remitenteId,
            Integer receptorId, MultipartFile archivo) {
        String url = storageService.subirVideo(archivo);
        if (url == null)
            throw new RuntimeException("Error al subir vídeo");
        String thumb = url.replaceAll("\\.(mp4|mov|avi|webm)$", ".jpg");
        ChatMensaje msg = buildBase(productoId, remitenteId, receptorId);
        msg.setMediaUrl(url);
        msg.setMediaThumbnail(thumb);
        msg.setTipo(TipoMensaje.VIDEO);
        return chatMensajeRepository.save(msg);
    }

    /**
     * Mensaje de voz.
     *
     * El navegador graba con MediaRecorder API y envía un Blob .webm/.ogg.
     * Se sube a Cloudinary como recurso de audio y se guarda la duración
     * para mostrar la barra de progreso en Angular.
     *
     * Angular (chat.component.ts):
     * // Grabar
     * this.recorder = new MediaRecorder(stream);
     * this.recorder.ondataavailable = (e) => chunks.push(e.data);
     * this.recorder.onstop = async () => {
     * const blob = new Blob(chunks, { type: 'audio/webm' });
     * const file = new File([blob], 'voice.webm');
     * const formData = new FormData();
     * formData.append('archivo', file);
     * formData.append('tipo', 'AUDIO');
     * formData.append('duracion', Math.round(duracionSegundos).toString());
     * await this.http.post('/chat/media?productoId=...', formData).toPromise();
     * };
     *
     * // Reproducir
     * const audio = new Audio(mensaje.mediaUrl);
     * audio.play();
     */
    @Transactional
    public ChatMensaje guardarMensajeAudio(Integer productoId, Integer remitenteId,
            Integer receptorId, MultipartFile archivo,
            Integer duracionSegundos) {
        String url = storageService.subirAudio(archivo);
        if (url == null)
            throw new RuntimeException("Error al subir audio");
        ChatMensaje msg = buildBase(productoId, remitenteId, receptorId);
        msg.setMediaUrl(url);
        msg.setAudioDuracionSegundos(duracionSegundos);
        msg.setTipo(TipoMensaje.AUDIO);
        return chatMensajeRepository.save(msg);
    }

    @Transactional
    public ChatMensaje guardarPropuestaPrecio(Integer productoId, Integer remitenteId,
            Integer receptorId, Double precio) {
        ChatMensaje msg = buildBase(productoId, remitenteId, receptorId);
        msg.setTexto("Propuesta de precio: " + precio + "€");
        msg.setTipo(TipoMensaje.OFERTA_PRECIO);
        msg.setPrecioPropuesto(precio);
        msg.setEstadoPropuesta("PENDIENTE");
        return chatMensajeRepository.save(msg);
    }

    @Transactional
    public ChatMensaje responderPropuesta(Integer mensajeId, boolean aceptada) {
        ChatMensaje msg = chatMensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));
        if (msg.getTipo() != TipoMensaje.OFERTA_PRECIO)
            throw new IllegalArgumentException("Este mensaje no es una propuesta de precio");
        msg.setEstadoPropuesta(aceptada ? "ACEPTADA" : "RECHAZADA");
        return chatMensajeRepository.save(msg);
    }

    @Transactional
    public ChatMensaje mensajeSistema(Integer productoId, Integer remitenteId,
            Integer receptorId, String texto) {
        ChatMensaje msg = buildBase(productoId, remitenteId, receptorId);
        msg.setTexto(texto);
        msg.setTipo(TipoMensaje.SISTEMA);
        return chatMensajeRepository.save(msg);
    }

    public List<ChatMensaje> getHistorial(String roomId) {
        return chatMensajeRepository.findByRoomId(roomId);
    }

    public List<ChatMensaje> getConversacion(String roomId, Integer u1, Integer u2) {
        return chatMensajeRepository.findConversacion(roomId, u1, u2);
    }

    public List<ChatMensaje> getUltimasConversaciones(Integer usuarioId) {
        return chatMensajeRepository.findUltimosMensajesPorUsuario(usuarioId);
    }

    public long getNoLeidos(Integer usuarioId) {
        return chatMensajeRepository.countNoLeidosByReceptor(usuarioId);
    }

    @Transactional
    public void marcarLeidos(String roomId, Integer receptorId) {
        chatMensajeRepository.marcarComoLeidosEnRoom(roomId, receptorId);
    }

    // ── Helper ──────────────────────────────────────────────────────────────
    private ChatMensaje buildBase(Integer productoId, Integer remitenteId, Integer receptorId) {
        ChatMensaje msg = new ChatMensaje();
        if (productoId != null) {
            productoService.findById(productoId).ifPresent(msg::setProducto);
            msg.setRoomId("P_" + productoId);
        } else if (remitenteId != null && receptorId != null) {
            int user1 = Math.min(remitenteId, receptorId);
            int user2 = Math.max(remitenteId, receptorId);
            msg.setRoomId("D_" + user1 + "_" + user2);
        }
        usuarioService.findById(remitenteId).ifPresent(msg::setRemitente);
        if (receptorId != null)
            usuarioService.findById(receptorId).ifPresent(msg::setReceptor);
        return msg;
    }
}