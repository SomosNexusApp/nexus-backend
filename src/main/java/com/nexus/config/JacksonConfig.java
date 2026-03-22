package com.nexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

@Configuration
public class JacksonConfig {
	@Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Esta línea es la magia: ignora las excepciones de Lazy Loading
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
}
