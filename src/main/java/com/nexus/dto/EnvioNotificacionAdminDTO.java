package com.nexus.dto;

import java.util.List;

/**
 * Envío masivo o individual de notificaciones desde el panel admin.
 */
public class EnvioNotificacionAdminDTO {
    /** Si se envía, ignora actorIds y notifica a todos los usuarios (paginado internamente). */
    private boolean broadcastTodos;
    private List<Integer> actorIds;
    private String titulo;
    private String mensaje;
    private String url;
    /** Nombre de {@link com.nexus.entity.TipoNotificacion}, p. ej. SISTEMA o ACCION_ADMIN */
    private String tipo = "SISTEMA";

    public boolean isBroadcastTodos() { return broadcastTodos; }
    public void setBroadcastTodos(boolean broadcastTodos) { this.broadcastTodos = broadcastTodos; }
    public List<Integer> getActorIds() { return actorIds; }
    public void setActorIds(List<Integer> actorIds) { this.actorIds = actorIds; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
