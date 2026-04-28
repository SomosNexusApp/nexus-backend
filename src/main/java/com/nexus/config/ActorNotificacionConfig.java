package com.nexus.config;

import jakarta.persistence.Embeddable;

/**
 * Preferencias de notificacion embebidas en Actor.
 * El usuario puede activar/desactivar cada tipo en Ajustes -> Notificaciones.
 */
@Embeddable
public class ActorNotificacionConfig {

    private Boolean emailNuevoMensaje  = true;
    private Boolean emailNuevaCompra   = true;
    private Boolean emailEstadoEnvio   = true;
    private Boolean emailMarketing     = false;
    private Boolean pushNuevoMensaje   = true;
    private Boolean pushNuevaCompra    = true;

    public Boolean getEmailNuevoMensaje()              { return emailNuevoMensaje; }
    public void    setEmailNuevoMensaje(Boolean b)     { this.emailNuevoMensaje = b; }
    public Boolean getEmailNuevaCompra()               { return emailNuevaCompra; }
    public void    setEmailNuevaCompra(Boolean b)      { this.emailNuevaCompra = b; }
    public Boolean getEmailEstadoEnvio()               { return emailEstadoEnvio; }
    public void    setEmailEstadoEnvio(Boolean b)      { this.emailEstadoEnvio = b; }
    public Boolean getEmailMarketing()                 { return emailMarketing; }
    public void    setEmailMarketing(Boolean b)        { this.emailMarketing = b; }
    public Boolean getPushNuevoMensaje()               { return pushNuevoMensaje; }
    public void    setPushNuevoMensaje(Boolean b)      { this.pushNuevoMensaje = b; }
    public Boolean getPushNuevaCompra()                { return pushNuevaCompra; }
    public void    setPushNuevaCompra(Boolean b)       { this.pushNuevaCompra = b; }
}