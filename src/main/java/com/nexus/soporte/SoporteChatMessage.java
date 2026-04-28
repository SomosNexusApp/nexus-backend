package com.nexus.soporte;
import com.nexus.entity.DomainEntity;
import com.nexus.soporte.SoporteChatSession;
import com.nexus.soporte.SoporteMensajeRol;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "soporte_chat_message")
public class SoporteChatMessage extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SoporteChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoporteMensajeRol rol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    private String tipoContenido;
    private Integer referenciaId;

    private LocalDateTime creadoEn;

    public SoporteChatMessage() {
        this.creadoEn = LocalDateTime.now();
    }

    public SoporteChatSession getSession() { return session; }
    public void setSession(SoporteChatSession session) { this.session = session; }

    public SoporteMensajeRol getRol() { return rol; }
    public void setRol(SoporteMensajeRol rol) { this.rol = rol; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }

    public Integer getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Integer referenciaId) { this.referenciaId = referenciaId; }
}
