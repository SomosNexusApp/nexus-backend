package com.nexus.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tabla de precios de envío sin pérdida (tarifa real 2025 + margen mínimo).
 * Cuando CarrierApiService tenga credenciales configuradas, este precio
 * se usa como fallback; de lo contrario, es el precio efectivo.
 *
 * ┌──────────────┬────────────┬──────────────────┐
 * │ Peso │ Domicilio │ Punto recogida │
 * ├──────────────┼────────────┼──────────────────┤
 * │ < 0,5 kg │ 3,90 € │ 3,40 € │
 * │ < 2 kg │ 5,20 € │ 4,70 € │
 * │ < 5 kg │ 7,50 € │ 6,90 € │
 * │ < 10 kg │ 11,00 € │ 10,20 € │
 * └──────────────┴────────────┴──────────────────┘
 */
@Service
public class ShippingPriceService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Precios ────────────────────────────────────────────────────────────

    /**
     * Calcula el precio de envío a domicilio según el peso.
     * 
     * @param pesoKg peso del paquete en kilogramos (0 < p <= 10)
     */
    public double calculateShippingPrice(double pesoKg) {
        return calculateShippingPrice(pesoKg, false);
    }

    /**
     * Calcula el precio de envío según el peso y el tipo de entrega.
     * 
     * @param pesoKg     peso del paquete en kilogramos (0 < p <= 10)
     * @param esRecogida true = punto de recogida (más barato), false = domicilio
     */
    public double calculateShippingPrice(double pesoKg, boolean esRecogida) {
        if (pesoKg <= 0)
            throw new IllegalArgumentException("El peso debe ser mayor que 0");
        if (pesoKg > 10)
            throw new IllegalArgumentException("El peso máximo permitido es 10 kg");

        if (esRecogida) {
            if (pesoKg <= 0.5)
                return 3.40;
            if (pesoKg <= 2.0)
                return 4.70;
            if (pesoKg <= 5.0)
                return 6.90;
            return 10.20; // <= 10 kg
        } else {
            if (pesoKg <= 0.5)
                return 3.90;
            if (pesoKg <= 2.0)
                return 5.20;
            if (pesoKg <= 5.0)
                return 7.50;
            return 11.00; // <= 10 kg
        }
    }

    /**
     * Precio de domicilio menos precio de recogida para un peso dado.
     */
    public double ahorroRecogida(double pesoKg) {
        return calculateShippingPrice(pesoKg, false) - calculateShippingPrice(pesoKg, true);
    }

    // ── Código único ──────────────────────────────────────────────────────

    /**
     * Genera un código de envío único — SHIP-XXXXXXXX (8 chars alfanuméricos).
     */
    public String generateShippingCode() {
        StringBuilder sb = new StringBuilder("SHIP-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    // ── QR ────────────────────────────────────────────────────────────────

    /**
     * Genera un QR de 300×300 px y lo devuelve como base64 PNG.
     * 
     * @param content texto a codificar (el codigoEnvio)
     */
    public String generateQrBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | java.io.IOException e) {
            throw new RuntimeException("Error generando QR: " + e.getMessage(), e);
        }
    }
}
