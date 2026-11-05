package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.models.ConfiguracionPagos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Valores configurables del módulo de pagos (todo por variables de entorno). */
@Configuration
public class PagosConfig {

    @Bean
    public ConfiguracionPagos configuracionPagos(
            @Value("${pagos.umbral-diferencia-porcentaje:20}") BigDecimal umbralDiferenciaPorcentaje,
            @Value("${pagos.url-retorno:http://localhost:4200/cliente/pago/resultado}") String urlRetorno) {
        return new ConfiguracionPagos(umbralDiferenciaPorcentaje, urlRetorno);
    }
}
