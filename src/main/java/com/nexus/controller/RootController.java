package com.nexus.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

@RestController
public class RootController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Nexus | Infraestructura Backend</title>" +
                "    <link rel='icon' type='image/x-icon' href='/favicon.ico'>" +
                "    <link rel='shortcut icon' type='image/x-icon' href='/favicon.ico'>" +
                "    <link rel='preconnect' href='https://fonts.googleapis.com'>" +
                "    <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>" +
                "    <link href='https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&family=JetBrains+Mono:wght@300;500&display=swap' rel='stylesheet'>"
                +
                "    <style>" +
                "        :root {" +
                "            --primary: #38bdf8;" +
                "            --secondary: #818cf8;" +
                "            --success: #22c55e;" +
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
                "            overflow-x: hidden;" +
                "            min-height: 100vh;" +
                "        }" +
                "        /* Background Grid System */" +
                "        .bg-wrapper {" +
                "            position: fixed; inset: 0; z-index: -1; overflow: hidden;" +
                "        }" +
                "        .grid-lines {" +
                "            position: absolute; inset: 0;" +
                "            background-image: " +
                "                linear-gradient(var(--grid) 1px, transparent 1px)," +
                "                linear-gradient(90deg, var(--grid) 1px, transparent 1px);" +
                "            background-size: 50px 50px;" +
                "        }" +
                "        .bg-glow {" +
                "            position: absolute; top: 0; left: 50%; width: 100%; height: 500px;" +
                "            background: radial-gradient(circle at 50% 0%, rgba(56, 189, 248, 0.15) 0%, transparent 70%);"
                +
                "            transform: translateX(-50%);" +
                "        }" +
                "        /* Noise Effect */" +
                "        .bg-wrapper::after {" +
                "            content: ''; position: absolute; inset: 0;" +
                "            background-image: url(\"data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E\");"
                +
                "            opacity: 0.03; pointer-events: none;" +
                "        }" +
                "        /* Header */" +
                "        header {" +
                "            border-bottom: 1px solid var(--border);" +
                "            padding: 1.5rem 4rem; display: flex; justify-content: space-between; align-items: center;"
                +
                "            backdrop-filter: blur(8px); position: sticky; top: 0; z-index: 1000;" +
                "            background: rgba(11, 15, 26, 0.8);" +
                "        }" +
                "        .logo-nav img { height: 40px; }" +
                "        .status-header {" +
                "            font-family: 'JetBrains Mono', monospace;" +
                "            display: flex; align-items: center; gap: 15px;" +
                "        }" +
                "        .mobile-status-right { display: none; margin-left: auto; }" +
                "        .mobile-tick {" +
                "            color: var(--success); font-size: 1.4rem; " +
                "            text-shadow: 0 0 12px rgba(34, 197, 94, 0.5);" +
                "            animation: pulse-tick 2s infinite;" +
                "        }" +
                "        @keyframes pulse-tick {" +
                "            0%, 100% { transform: scale(1); opacity: 1; filter: brightness(1); }" +
                "            50% { transform: scale(1.15); opacity: 0.7; filter: brightness(1.4); }" +
                "        }" +
                "        .status-text {" +
                "            font-size: 0.75rem; letter-spacing: 2px; color: var(--success); font-weight: 500;" +
                "            border-right: 2px solid var(--success); padding-right: 10px;" +
                "            animation: blink 1.5s steps(1) infinite;" +
                "        }" +
                "        @keyframes blink { 50% { border-color: transparent; } }" +
                "        .status-label { font-size: 0.7rem; color: var(--text-dim); text-transform: uppercase; }" +
                "        /* Content Layout */" +
                "        .layout { max-width: 1400px; margin: 0 auto; padding: 80px 4rem; position: relative; }" +
                "        /* Decorative Lines */" +
                "        .line-v { position: absolute; left: 2rem; top: 0; bottom: 0; width: 1px; background: linear-gradient(transparent, var(--border), transparent); }"
                +
                "        .line-h { position: absolute; left: 0; right: 0; top: 40px; height: 1px; background: linear-gradient(90deg, transparent, var(--border), transparent); }"
                +
                "        /* Hero Section */" +
                "        .hero { margin-bottom: 120px; position: relative; }" +
                "        .hero h1 {" +
                "            font-size: 6rem; font-weight: 800; line-height: 0.9; margin-bottom: 2rem;" +
                "            letter-spacing: -4px;" +
                "        }" +
                "        .hero p {" +
                "            max-width: 600px; font-size: 1.25rem; color: var(--text-dim); font-weight: 300; margin-bottom: 3rem; "
                +
                "            border-left: 2px solid var(--primary); padding-left: 25px; " +
                "        }" +
                "        /* Bento Grid */" +
                "        .bento {" +
                "            display: grid; grid-template-columns: repeat(12, 1fr); gap: 24px; margin-bottom: 60px;" +
                "        }" +
                "        .bento-card {" +
                "            background: rgba(255, 255, 255, 0.02); border: 1px solid var(--border);" +
                "            border-radius: 12px; padding: 40px; position: relative; overflow: hidden;" +
                "            transition: all 0.3s ease;" +
                "        }" +
                "        .bento-card:hover { border-color: var(--primary); background: rgba(56, 189, 248, 0.05); }" +
                "        .bento-card h3 { font-size: 1.1rem; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 15px; color: var(--primary); }"
                +
                "        .bento-card p { color: var(--text-dim); font-size: 0.95rem; font-weight: 300; }" +
                "        .c-1 { grid-column: span 8; } .c-2 { grid-column: span 4; }" +
                "        .c-3 { grid-column: span 3; } .c-4 { grid-column: span 3; } .c-5 { grid-column: span 3; } .c-6 { grid-column: span 3; }"
                +
                "        .c-full { grid-column: span 12; }" +
                "        /* Tech Stack List */" +
                "        .tech-list { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 20px; }" +
                "        .tech-tag {" +
                "            font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; background: rgba(255,255,255,0.05);"
                +
                "            padding: 5px 12px; border-radius: 4px; border: 1px solid var(--border); color: var(--text-dim);"
                +
                "        }" +
                "        /* Code Section */" +
                "        .code-window {" +
                "            background: #000; border: 1px solid var(--border); border-radius: 8px; margin-top: 40px;" +
                "            display: flex; flex-direction: column; box-shadow: 0 40px 100px -20px rgba(0,0,0,0.8);" +
                "        }" +
                "        .win-header { padding: 12px 20px; border-bottom: 1px solid var(--border); display: flex; gap: 8px; align-items: center; }"
                +
                "        .win-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-dim); opacity: 0.3; }"
                +
                "        .win-title { margin-left: auto; font-size: 0.7rem; font-family: 'JetBrains Mono'; color: var(--text-dim); }"
                +
                "        .win-body { padding: 30px; font-family: 'JetBrains Mono', monospace; font-size: 0.85rem; line-height: 1.8; color: #94a3b8; }"
                +
                "        .highlight { color: var(--primary); }" +
                "        /* Metric Items */" +
                "        .metric-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-top: 25px; }"
                +
                "        .metric-item { border-left: 1px solid var(--border); padding-left: 20px; }" +
                "        .metric-item strong { display: block; font-size: 1.2rem; color: white; }" +
                "        .metric-item span { font-size: 0.75rem; color: var(--text-dim); text-transform: uppercase; }" +
                "        /* Dev Tools Section */" +
                "        .dev-tools { display: flex; gap: 20px; margin-top: 40px; flex-wrap: wrap; }" +
                "        .dev-btn {" +
                "            display: flex; align-items: center; gap: 12px; padding: 16px 24px;" +
                "            background: rgba(255,255,255,0.03); border: 1px solid var(--border); border-radius: 12px;"
                +
                "            text-decoration: none; color: white; transition: all 0.3s ease;" +
                "            font-weight: 500; font-size: 0.9rem;" +
                "        }" +
                "        .dev-btn:hover { border-color: var(--primary); transform: translateY(-3px); background: rgba(255,255,255,0.05); }"
                +
                "        .dev-btn img { height: 24px; width: auto; }" +
                "        /* Topology Section */" +
                "        .topology { display: flex; flex-direction: column; gap: 20px; margin: 100px 0; }" +
                "        .topo-row { display: flex; justify-content: center; align-items: center; gap: 40px; }" +
                "        .topo-node { background: rgba(255,255,255,0.03); border: 1px solid var(--border); border-radius: 12px; padding: 15px 25px; font-size: 0.8rem; font-family: 'JetBrains Mono'; color: var(--primary); text-align: center; }"
                +
                "        .topo-arrow { width: 40px; height: 1px; background: var(--border); position: relative; }" +
                "        .topo-arrow::after { content: '>'; position: absolute; right: -5px; top: -8px; color: var(--border); font-family: monospace; }"
                +
                "        /* FAQ Redesign */" +
                "        .section-tag { color: var(--primary); font-family: 'JetBrains Mono'; font-size: 0.75rem; letter-spacing: 2px; margin-bottom: 10px; display: block; }"
                +
                "        .section-title { font-size: 2.5rem; margin-bottom: 40px; letter-spacing: -1px; }" +
                "        .faq-grid {" +
                "            display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 100px;" +
                "        }" +
                "        .faq-box {" +
                "            padding: 30px; border: 1px solid var(--border); border-radius: 8px;" +
                "            background: rgba(255,255,255,0.01); display: flex; flex-direction: column; gap: 15px;" +
                "        }" +
                "        .faq-q { color: white; font-weight: 600; font-size: 1.05rem; display: flex; align-items: flex-start; gap: 10px; }"
                +
                "        .faq-q::before { content: '>'; color: var(--primary); font-family: 'JetBrains Mono'; }" +
                "        .faq-a { color: var(--text-dim); font-size: 0.9rem; line-height: 1.6; font-weight: 300; padding-left: 20px; }"
                +
                "        /* Waves Effect Container */" +
                "        .waves-container {" +
                "            position: absolute; bottom: 0; left: 0; width: 100%; height: 150px; opacity: 0.3; pointer-events: none; z-index: -1;"
                +
                "        }" +
                "        /* Footer */" +
                "        footer { padding: 100px 4rem 80px; border-top: 1px solid var(--border); display: flex; flex-direction: column; align-items: center; gap: 40px; }"
                +
                "        .footer-logo img { height: 50px; opacity: 0.8; transition: opacity 0.3s ease; }" +
                "        .footer-logo img:hover { opacity: 1; }" +
                "        .footer-info { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 60px; width: 100%; max-width: 1000px; text-align: left; }"
                +
                "        .info-col h5 { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 2px; color: var(--primary); margin-bottom: 20px; }"
                +
                "        .info-col p, .info-col a { font-size: 0.9rem; color: var(--text-dim); text-decoration: none; display: block; margin-bottom: 10px; }"
                +
                "        .info-col a:hover { color: white; }" +
                "        .ecentia-credit {" +
                "            display: flex; align-items: center; gap: 8px; color: var(--text-dim); font-size: 0.85rem;"
                +
                "        }" +
                "        .ecentia-credit img { height: 18px; }" +
                "        /* Responsive */" +
                "        @media (max-width: 1024px) {" +
                "            .hero h1 { font-size: 4rem; }" +
                "            .c-1, .c-2, .c-3, .c-4, .c-5, .c-6 { grid-column: span 12; }" +
                "            header, .layout, footer { padding: 1.5rem 2rem; }" +
                "            .metric-grid { grid-template-columns: 1fr; }" +
                "            .topo-row { flex-direction: column; }" +
                "            .topo-arrow { width: 1px; height: 30px; }" +
                "            .topo-arrow::after { transform: rotate(90deg); bottom: -10px; right: -5px; top: auto; }" +
                "            .faq-grid { grid-template-columns: 1fr; }" +
                "        }" +
                "        @media (max-width: 768px) {" +
                "            header { padding: 1rem 1.5rem; display: flex; align-items: center; justify-content: space-between; }"
                +
                "            .status-header { display: none; }" +
                "            .mobile-status-right { display: flex; align-items: center; }" +
                "            .logo-nav { display: flex; align-items: center; }" +
                "            .hero h1 { font-size: 2.8rem; letter-spacing: -2px; }" +
                "            .hero p { font-size: 1.1rem; padding-left: 15px; }" +
                "            .layout { padding: 40px 1.5rem; }" +
                "            .section-title { font-size: 1.8rem; }" +
                "            .bento-card { padding: 25px; }" +
                "            .metric-grid { gap: 15px; }" +
                "            .dev-tools { flex-direction: column; }" +
                "            .dev-btn { justify-content: center; }" +
                "            .win-body { padding: 15px; font-size: 0.75rem; }" +
                "            footer { padding: 60px 1.5rem; }" +
                "            .footer-info { gap: 40px; }" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='bg-wrapper'>" +
                "        <div class='grid-lines'></div>" +
                "        <div class='bg-glow'></div>" +
                "    </div>" +
                "    " +
                "    <header>" +
                "        <div class='logo-nav'>" +
                "            <img src='/logo.webp' alt='Nexus'>" +
                "        </div>" +
                "        <div class='mobile-status-right'>" +
                "            <span class='mobile-tick'>✔</span>" +
                "        </div>" +
                "        <div class='status-header'>" +
                "            <span class='status-text'>BACKEND OPERATIVO</span>" +
                "            <span class='status-label'>Core Gateway v3.5</span>" +
                "        </div>" +
                "    </header>" +
                "    " +
                "    <main class='layout'>" +
                "        <div class='line-v'></div>" +
                "        <div class='line-h'></div>" +
                "        " +
                "        <section class='hero'>" +
                "            <h1>Servicios Unificados.</h1>" +
                "            <p>Ecosistema digital centralizado para la orquestación de recursos de mercado, mensajería de baja latencia y gestión de identidad corporativa.</p>"
                +
                "            <div class='tech-list'>" +
                "                <span class='tech-tag'>Java 17 JRE</span>" +
                "                <span class='tech-tag'>Spring Security 6</span>" +
                "                <span class='tech-tag'>PostgreSQL Driver</span>" +
                "                <span class='tech-tag'>Hibernate ORM</span>" +
                "                <span class='tech-tag'>WebSocket API</span>" +
                "                <span class='tech-tag'>Swagger OpenAPI</span>" +
                "            </div>" +
                "            " +
                "            <div class='dev-tools'>" +
                "                <a href='/swagger-ui.html' class='dev-btn'>" +
                "                    <img src='/swagger-logo.webp' alt='Swagger'> Documentación Swagger UI" +
                "                </a>" +
                "                <a href=\"https://somosnexusapp-1419993.postman.co/workspace/Nexus's-Workspace~fed8b792-0672-4ba9-8e45-743c50854ad4/collection/52000346-10c36880-a26e-4bbb-bda3-bda71061387f?action=share&source=copy-link&creator=52000346\" target='_blank' class='dev-btn'>"
                +
                "                    <img src='/postman-icon.webp' alt='Postman'> Workspace de Postman" +
                "                </a>" +
                "            </div>" +
                "        </section>" +
                "        " +
                "        <div class='bento'>" +
                "            <div class='bento-card c-1'>" +
                "                <h3>Infraestructura y Rendimiento</h3>" +
                "                <p>Nuestra arquitectura está optimizada para respuestas ultra-rápidas, utilizando un pool de conexiones balanceado y caché de segundo nivel para operaciones masivas de marketplace.</p>"
                +
                "                <div class='metric-grid'>" +
                "                    <div class='metric-item'><strong>~15ms</strong><span>Response Avg</span></div>" +
                "                    <div class='metric-item'><strong>256-bit</strong><span>AES Standard</span></div>" +
                "                    <div class='metric-item'><strong>Zero</strong><span>Downtime Deploy</span></div>" +
                "                </div>" +
                "                <div class='code-window'>" +
                "                    <div class='win-header'>" +
                "                        <div class='win-dot'></div><div class='win-dot'></div><div class='win-dot'></div>"
                +
                "                        <div class='win-title'>HealthController.java</div>" +
                "                    </div>" +
                "                    <div class='win-body'>" +
                "                        <span class='highlight'>@GetMapping</span>(\"/status\")<br>" +
                "                        <span class='highlight'>public</span> Map&lt;String, Object&gt; getStatus() {<br>"
                +
                "                        &nbsp;&nbsp;<span class='highlight'>return</span> Map.of(<br>" +
                "                        &nbsp;&nbsp;&nbsp;&nbsp;\"system\", \"online\",<br>" +
                "                        &nbsp;&nbsp;&nbsp;&nbsp;\"timestamp\", System.currentTimeMillis()<br>" +
                "                        &nbsp;&nbsp;);<br>" +
                "                        }" +
                "                    </div>" +
                "                </div>" +
                "            </div>" +
                "            " +
                "            <div class='bento-card c-2'>" +
                "                <h3>Servicios Core</h3>" +
                "                <ul style='list-style: none; margin-top: 20px; color: var(--text-dim); font-size: 0.9rem;'>"
                +
                "                    <li style='margin-bottom: 20px;'>• <strong>Auth Engine:</strong> Autenticación centralizada con soporte para 2FA y sesiones persistentes.</li>"
                +
                "                    <li style='margin-bottom: 20px;'>• <strong>Market Core:</strong> Gestión unificada de catálogo (Productos, Vehículos, Ofertas) con filtrado avanzado.</li>"
                +
                "                    <li style='margin-bottom: 20px;'>• <strong>Comms Stack:</strong> Sistema de chat WebSocket con soporte para archivos y notificaciones push.</li>"
                +
                "                    <li style='margin-bottom: 20px;'>• <strong>Admin Panel API:</strong> Completo set de herramientas de gestión para categorías, cupones y moderación.</li>"
                +
                "                </ul>" +
                "            </div>" +
                "            " +
                "            <div class='bento-card c-full' style='background: linear-gradient(90deg, rgba(56, 189, 248, 0.05) 0%, transparent 100%);'>"
                +
                "                 <h3 style='color: var(--primary);'>Seguridad y Moderación Inteligente</h3>" +
                "                 <p>Implementamos un motor de moderación avanzado capaz de analizar contenido textual para garantizar un entorno seguro. La seguridad se complementa con protección contra intrusiones, auditoría de logs y gestión de usuarios baneados/verificados.</p>"
                +
                "            </div>" +
                "            " +
                "            <div class='bento-card c-3'>" +
                "                <h3>Transacciones</h3>" +
                "                <p>Gestión de flujo de compras completo, incluyendo devoluciones, cupones de descuento y contratos de empresa/vendedor.</p>"
                +
                "            </div>" +
                "            " +
                "            <div class='bento-card c-4'>" +
                "                <h3>Social & Feedback</h3>" +
                "                <p>Sistemas integrados de valoraciones entre usuarios, comentarios en productos y votaciones (Spark/Drip Votos).</p>"
                +
                "            </div>" +
                "            " +
                "            <div class='bento-card c-5'>" +
                "                <h3>Logística Real</h3>" +
                "                <p>Orquestación de envíos mediante transportistas integrados, con sistema de reportes y soporte técnico vía chat.</p>"
                +
                "            </div>" +
                "            <div class='bento-card c-6'>" +
                "                <h3>Comunidad</h3>" +
                "                <p>Sistema de Newsletter completo con suscripción, confirmación por email y gestión de preferencias.</p>"
                +
                "            </div>" +
                "        </div>" +
                "        " +
                "        <section class='topology'>" +
                "            <span class='section-tag'>Arquitectura Global</span>" +
                "            <h2 class='section-title'>Flujo de Datos Unificado</h2>" +
                "            <div class='topo-row'>" +
                "                <div class='topo-node'>Web / App Client</div>" +
                "                <div class='topo-arrow'></div>" +
                "                <div class='topo-node'>Nexus API Gateway</div>" +
                "                <div class='topo-arrow'></div>" +
                "                <div class='topo-node'>Security Filter Chain</div>" +
                "            </div>" +
                "            <div class='topo-row' style='margin-top: 20px;'>" +
                "                <div class='topo-node' style='border-color: var(--success); color: var(--success);'>Standard Controllers</div>"
                +
                "                <div class='topo-arrow'></div>" +
                "                <div class='topo-node'>Persistence Layer (DB)</div>" +
                "                <div class='topo-arrow'></div>" +
                "                <div class='topo-node' style='border-color: var(--secondary); color: var(--secondary);'>External Cloud (Media/STP)</div>"
                +
                "            </div>" +
                "        </section>" +
                "        " +
                "        <section class='faq-area'>" +
                "            <span class='section-tag'>Knowledge Base</span>" +
                "            <h2 class='section-title'>Documentación de Integración</h2>" +
                "            <div class='faq-grid'>" +
                "                <div class='faq-box'>" +
                "                    <div class='faq-q'>Tokens de Acceso (Auth)</div>" +
                "                    <div class='faq-a'>Autenticación mediante cabecera <code>Authorization: Bearer <token></code>. Los tokens se obtienen en <code>/api/auth/login</code> y tienen una validez temporal configurable para seguridad máxima.</div>"
                +
                "                </div>" +
                "                <div class='faq-box'>" +
                "                    <div class='faq-q'>Estructura de Errores Standard</div>" +
                "                    <div class='faq-a'>Implementamos un formato unificado: <code>{ timestamp, status, error, message, path }</code>. Los códigos 4xx indican error de cliente (validación) y 5xx errores críticos de infraestructura.</div>"
                +
                "                </div>" +
                "                <div class='faq-box'>" +
                "                    <div class='faq-q'>Paginación Dinámica</div>" +
                "                    <div class='faq-a'>La mayoría de listados soportan los parámetros <code>page</code> y <code>size</code>. El servidor responde con metadatos de paginación incluyendo <code>totalElements</code> y <code>totalPages</code>.</div>"
                +
                "                </div>" +
                "                <div class='faq-box'>" +
                "                    <div class='faq-q'>Protocolo WebSocket (Real-time)</div>" +
                "                    <div class='faq-a'>Disponible en el endpoint <code>/ws</code>. Utiliza protocolo STOMP para suscripciones a canales de chat, notificaciones y actualizaciones de estado en vivo.</div>"
                +
                "                </div>" +
                "                <div class='faq-box'>" +
                "                    <div class='faq-q'>Política de CORS & Orígenes</div>" +
                "                    <div class='faq-a'>Permitimos peticiones desde dominios verificados: <code>nexus-app.es</code>, <code>admin.nexus-app.es</code> y entornos locales <code>localhost:4200/4201</code>.</div>"
                +
                "                </div>" +
                "                <div class='faq-box'>" +
                "                    <div class='faq-q'>Límites de Carga & Cuotas</div>" +
                "                    <div class='faq-a'>Transferencia optimizada mediante MultipartFile. Límites estándar de 10MB por archivo, procesados asíncronamente para evitar bloqueos en el hilo principal.</div>"
                +
                "                </div>" +
                "            </div>" +
                "        </section>" +
                "        " +
                "        <div class='waves-container'>" +
                "            <svg viewBox='0 0 120 18' style='width: 100%; height: 100%; fill: var(--primary);'>" +
                "                <path d='M0 9 C20 0 40 0 60 9 C80 18 100 18 120 9 V18 H0 Z' />" +
                "            </svg>" +
                "        </div>" +
                "    </main>" +
                "    " +
                "    <footer>" +
                "        <div class='footer-logo'>" +
                "            <img src='/logo.webp' alt='Nexus'>" +
                "        </div>" +
                "        <div class='footer-info'>" +
                "            <div class='info-col'>" +
                "                <h5>Ecosistema</h5>" +
                "                <a href='https://nexus-app.es'>Nexus Web Portal</a>" +
                "                <a href='https://admin.nexus-app.es'>Consola de Administración</a>" +
                "                <a href='https://about.nexus-app.es'>Sobre Nexus (Nexus App)</a>" +
                "            </div>" +
                "            <div class='info-col'>" +
                "                <h5>Servicios API</h5>" +
                "                <p>Autenticación (JWT + 2FA)</p>" +
                "                <p>Marketplace & Logística</p>" +
                "                <p>Chat & Moderación IA</p>" +
                "            </div>" +
                "            <div class='info-col'>" +
                "                <h5>Soporte</h5>" +
                "                <a href='mailto:somosnexusapp@gmail.com'>Ingeniería / Soporte</a>" +
                "                <div class='ecentia-credit' style='margin-top: 25px;'>" +
                "                    una aplicación de <img src='/ecentia-icon.ico' alt='Ecentia'> <strong>Ecentia</strong>"
                +
                "                </div>" +
                "            </div>" +
                "        </div>" +
                "    </footer>" +
                "</body>" +
                "</html>";
    }
}
