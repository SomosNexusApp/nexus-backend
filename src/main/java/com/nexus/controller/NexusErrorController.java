package com.nexus.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class NexusErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorCode = "ERROR";
        String errorMessage = "Ha ocurrido un error inesperado";
        String errorDescription = "Nuestro sistema ha detectado una anomalía en la solicitud.";

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorCode = "404";
                errorMessage = "Nodo No Encontrado";
                errorDescription = "El recurso que intentas orquestar no existe en nuestra infraestructura.";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorCode = "403";
                errorMessage = "Acceso Restringido";
                errorDescription = "No dispones de los privilegios de seguridad necesarios para acceder a este nodo.";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorCode = "500";
                errorMessage = "Falla de Sistema";
                errorDescription = "Se ha producido un error crítico durante el procesamiento interno.";
            }
        }

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Nexus | Error " + errorCode + "</title>" +
                "    <link rel='icon' type='image/x-icon' href='/favicon.ico'>" +
                "    <link rel='preconnect' href='https://fonts.googleapis.com'>" +
                "    <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>" +
                "    <link href='https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&family=JetBrains+Mono:wght@300;500&display=swap' rel='stylesheet'>"
                +
                "    <style>" +
                "        :root {" +
                "            --primary: #38bdf8;" +
                "            --error: #f43f5e;" +
                "            --bg: #0b0f1a;" +
                "            --text: #f1f5f9;" +
                "            --text-dim: #64748b;" +
                "            --border: rgba(56, 189, 248, 0.15);" +
                "            --grid: rgba(56, 189, 248, 0.05);" +
                "        }" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "        body {" +
                "            font-family: 'Outfit', sans-serif;" +
                "            background-color: var(--bg);" +
                "            color: var(--text);" +
                "            overflow: hidden;" +
                "            min-height: 100vh;" +
                "            display: flex; align-items: center; justify-content: center;" +
                "        }" +
                "        .bg-wrapper { position: fixed; inset: 0; z-index: -1; overflow: hidden; }" +
                "        .grid-lines { position: absolute; inset: 0;" +
                "            background-image: " +
                "                linear-gradient(var(--grid) 1px, transparent 1px)," +
                "                linear-gradient(90deg, var(--grid) 1px, transparent 1px);" +
                "            background-size: 50px 50px;" +
                "        }" +
                "        .bg-glow { position: absolute; top: 0; left: 50%; width: 100%; height: 500px;" +
                "            background: radial-gradient(circle at 50% 0%, rgba(56, 189, 248, 0.1) 0%, transparent 70%); transform: translateX(-50%); }"
                +
                "        .error-card {" +
                "            background: rgba(255, 255, 255, 0.02); border: 1px solid var(--border);" +
                "            border-radius: 24px; padding: 60px 40px; max-width: 500px; width: 90%; text-align: center;"
                +
                "            backdrop-filter: blur(12px); position: relative; overflow: hidden;" +
                "        }" +
                "        .error-card::before {" +
                "            content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 2px;" +
                "            background: linear-gradient(90deg, transparent, var(--primary), transparent);" +
                "        }" +
                "        .error-code { font-family: 'JetBrains Mono', monospace; font-size: 6rem; font-weight: 800; color: var(--error); letter-spacing: -4px; margin-bottom: 10px; opacity: 0.9; }"
                +
                "        .status-tag { font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; color: var(--primary); letter-spacing: 2px; text-transform: uppercase; margin-bottom: 25px; display: block; }"
                +
                "        .error-title { font-size: 2rem; font-weight: 800; margin-bottom: 15px; letter-spacing: -1px; }"
                +
                "        .error-desc { color: var(--text-dim); font-size: 1rem; line-height: 1.6; margin-bottom: 40px; font-weight: 300; }"
                +
                "        .btn-home {" +
                "            display: inline-flex; align-items: center; gap: 10px; padding: 14px 28px;" +
                "            background: rgba(56, 189, 248, 0.1); border: 1px solid var(--primary); border-radius: 12px;"
                +
                "            color: var(--primary); text-decoration: none; font-weight: 600; font-size: 0.9rem; transition: all 0.3s ease;"
                +
                "        }" +
                "        .btn-home:hover { background: var(--primary); color: var(--bg); transform: translateY(-3px); box-shadow: 0 10px 20px -10px var(--primary); }"
                +
                "        .footer-credit { margin-top: 50px; display: flex; align-items: center; justify-content: center; gap: 8px; color: var(--text-dim); font-size: 0.75rem; }"
                +
                "        .footer-credit img { height: 14px; opacity: 0.6; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='bg-wrapper'>" +
                "        <div class='grid-lines'></div>" +
                "        <div class='bg-glow'></div>" +
                "    </div>" +
                "    " +
                "    <div class='error-card'>" +
                "        <div style='margin-bottom: 30px;'>" +
                "            <img src='/logo.webp' alt='Nexus' style='height: 48px; opacity: 0.9;'>" +
                "        </div>" +
                "        <span class='status-tag'>Security Gateway Error</span>" +
                "        <div class='error-code'>" + errorCode + "</div>" +
                "        <h1 class='error-title'>" + errorMessage + "</h1>" +
                "        <p class='error-desc'>" + errorDescription + "</p>" +
                "        " +
                "        <a href='/' class='btn-home'>" +
                "            <svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='m12 19-7-7 7-7'/><path d='M19 12H5'/></svg>"
                +
                "            RECONECTAR AL NODO CENTRAL" +
                "        </a>" +
                "        " +
                "        <div class='footer-credit'>" +
                "            una aplicación de <img src='/ecentia-icon.ico' alt='Ecentia'> <strong>Ecentia</strong>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}
