package com.appointech.appointech_backend.infrastructure.perf;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Role;

/**
 * Medición de tiempos solo para el perfil "perf": con cualquier otro perfil esta clase ni se carga.
 * El umbral se ajusta con PERF_UMBRAL_LENTO_MS (propiedad perf.umbral-lento-ms).
 */
@Configuration
@Profile("perf")
public class MedicionTiemposPerfConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor medicionTiemposAdvisor(@Value("${perf.umbral-lento-ms:300}") long umbralMs) {
        return new DefaultPointcutAdvisor(new PuntoCorteCapasAplicacion(), new MedidorTiemposInterceptor(umbralMs));
    }
}
