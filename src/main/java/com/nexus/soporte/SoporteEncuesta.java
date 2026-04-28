package com.nexus.soporte;
import com.nexus.entity.DomainEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @deprecated Esta entidad ya NO se usa. Los campos de encuesta (valoracion y comentario)
 * se fusionaron directamente en SoporteChatSession como 'encuestaValoracion' y 'encuestaComentario'.
 * Esto simplifica el modelo: en lugar de hacer JOIN entre dos tablas para ver la encuesta
 * de una sesion, esta todo en la misma fila de soporte_chat_session.
 * Para el UML, esta tabla puede omitirse.
 */
@Deprecated
@Entity
@Table(name = "soporte_encuesta")
public class SoporteEncuesta extends DomainEntity {

    @Column(nullable = false)
    private Integer sessionId;

    @Column(nullable = false)
    private int valoracion; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comentario;

    public SoporteEncuesta() {}

    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }

    public int getValoracion() { return valoracion; }
    public void setValoracion(int valoracion) { this.valoracion = valoracion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
