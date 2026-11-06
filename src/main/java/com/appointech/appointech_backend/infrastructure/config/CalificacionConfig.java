package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.models.ConfiguracionCalificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Valores configurables de las calificaciones (por variables de entorno). */
@Configuration
public class CalificacionConfig {

    @Bean
    public ConfiguracionCalificacion configuracionCalificacion(@Value("${calificacion.plazo-dias:30}") int plazoDias) {
        return new ConfiguracionCalificacion(plazoDias);
    }
}
