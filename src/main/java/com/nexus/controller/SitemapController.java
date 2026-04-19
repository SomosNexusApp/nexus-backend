package com.nexus.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.entity.Producto;
import com.nexus.service.ProductoService;

@RestController
public class SitemapController {

    @Autowired
    private ProductoService productoService;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        List<Producto> productos = productoService.findDisponibles();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // Rutas estáticas clave
        sb.append("    <url>\n        <loc>https://nexus-app.es/</loc>\n        <changefreq>always</changefreq>\n        <priority>1.00</priority>\n    </url>\n");
        sb.append("    <url>\n        <loc>https://nexus-app.es/ofertas</loc>\n        <changefreq>hourly</changefreq>\n        <priority>0.90</priority>\n    </url>\n");
        sb.append("    <url>\n        <loc>https://nexus-app.es/chollos</loc>\n        <changefreq>hourly</changefreq>\n        <priority>0.90</priority>\n    </url>\n");

        // Categorías principales y motor
        String[] categorias = {"motor", "tecnologia", "moda", "hogar", "deportes", "ocio"};
        for (String cat : categorias) {
            sb.append("    <url>\n        <loc>https://nexus-app.es/categoria/").append(cat).append("</loc>\n");
            sb.append("        <changefreq>daily</changefreq>\n        <priority>0.80</priority>\n    </url>\n");
        }

        // Productos dinámicos extraídos automáticamente de la base de datos
        for (Producto p : productos) {
            sb.append("    <url>\n        <loc>https://nexus-app.es/producto/").append(p.getId()).append("</loc>\n");
            
            if (p.getFechaPublicacion() != null) {
                try {
                    // Format ISO-8601 if it's a LocalDateTime.
                    // Depending on what getFechaPublicacion is, it will naturally toString() correctly or we format.
                    sb.append("        <lastmod>").append(p.getFechaPublicacion().toString().split("\\.")[0]).append("</lastmod>\n");
                } catch (Exception e) {
                    // if parsing fails skip lastmod
                }
            }
            sb.append("        <changefreq>daily</changefreq>\n        <priority>0.70</priority>\n    </url>\n");
        }

        sb.append("</urlset>");
        return sb.toString();
    }

    /**
     * Sitemap propio y exclusivo para la infraestructura del Backend (api.nexus-app.es)
     */
    @GetMapping(value = "/sitemap-api.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getApiSitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // Root API page
        sb.append("    <url>\n");
        sb.append("        <loc>https://api.nexus-app.es/</loc>\n");
        sb.append("        <changefreq>weekly</changefreq>\n");
        sb.append("        <priority>1.00</priority>\n");
        sb.append("    </url>\n");
        
        // Swagger UI Documentation
        sb.append("    <url>\n");
        sb.append("        <loc>https://api.nexus-app.es/swagger-ui.html</loc>\n");
        sb.append("        <changefreq>weekly</changefreq>\n");
        sb.append("        <priority>0.80</priority>\n");
        sb.append("    </url>\n");

        sb.append("</urlset>");
        return sb.toString();
    }

    /**
     * Objeto robots.txt nativo para el servidor Backend
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getApiRobotsTxt() {
        return "User-agent: *\n" +
               "Allow: /\n" +
               "Allow: /swagger-ui.html\n" +
               "Disallow: /api/auth/\n\n" +
               "Sitemap: https://api.nexus-app.es/sitemap-api.xml\n";
    }
}
