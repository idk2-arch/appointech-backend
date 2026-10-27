package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.models.ConfiguracionRuta;
import com.appointech.appointech_backend.domain.models.ContactoAdminConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Valores configurables de la agenda del técnico: zona horaria del reloj, margen y tolerancia de retraso de la ruta. */
@Configuration
public class AgendaConfig {

    @Bean
    public Clock reloj(@Value("${app.zona-horaria:America/Bogota}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }

    /** Teléfono de respaldo del administrador (CONTACTO_ADMIN_TELEFONO); opcional, sin valor por defecto. */
    @Bean
    public ContactoAdminConfig contactoAdminConfig(@Value("${contacto.admin.telefono:}") String telefono) {
        return new ContactoAdminConfig(telefono);
    }

    @Bean
    public ConfiguracionRuta configuracionRuta(@Value("${ruta.margen-minutos:0}") int margenMinutos,
                                               @Value("${ruta.tolerancia-retraso-minutos:5}") int toleranciaRetrasoMinutos) {
        return new ConfiguracionRuta(margenMinutos, toleranciaRetrasoMinutos);
    }
}
