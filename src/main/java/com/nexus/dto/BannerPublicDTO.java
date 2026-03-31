package com.nexus.dto;

import com.nexus.entity.TipoContrato;

/**
 * Banner patrocinado activo (contrato pagado) para home / pie de página.
 */
public class BannerPublicDTO {

    private Integer contratoId;
    private TipoContrato tipoContrato;
    private String textoBanner;
    private String urlClick;
    private Integer productoId;
    private String empresaNombre;

    public Integer getContratoId() {
        return contratoId;
    }

    public void setContratoId(Integer contratoId) {
        this.contratoId = contratoId;
    }

    public TipoContrato getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(TipoContrato tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public String getTextoBanner() {
        return textoBanner;
    }

    public void setTextoBanner(String textoBanner) {
        this.textoBanner = textoBanner;
    }

    public String getUrlClick() {
        return urlClick;
    }

    public void setUrlClick(String urlClick) {
        this.urlClick = urlClick;
    }

    public Integer getProductoId() {
        return productoId;
    }

    public void setProductoId(Integer productoId) {
        this.productoId = productoId;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public void setEmpresaNombre(String empresaNombre) {
        this.empresaNombre = empresaNombre;
    }
}
