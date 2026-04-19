package com.nexus.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración global para garantizar un encoding UTF-8 en todo el backend.
 * Asegura que los caracteres españoles (ñ, acentos, etc.) se envíen y reciban correctamente.
 */
@Configuration
public class EncodingConfig implements WebMvcConfigurer {

    /**
     * Fuerza el CharacterEncodingFilter como el filtro de más alta prioridad en Spring.
     * Sobrescribe cualquier codificación por defecto (ej. Windows-1252/Cp1252).
     */
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterBean() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding(StandardCharsets.UTF_8.name());
        filter.setForceEncoding(true);            // Fuerza en Request
        filter.setForceRequestEncoding(true);     // Doble comprobación Request
        filter.setForceResponseEncoding(true);    // Doble comprobación Response

        FilterRegistrationBean<CharacterEncodingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Reemplaza todos los StringHttpMessageConverter para que usen UTF-8 explícitamente.
     * (Por defecto, Spring MVC utiliza ISO-8859-1 para los Strings, lo que corrompe las eñes y tildes).
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false); // Para no ensuciar la cabecera
        converters.add(0, stringConverter);
    }
}
