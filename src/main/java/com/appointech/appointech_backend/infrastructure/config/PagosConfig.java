package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.models.ConfiguracionPagos;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import com.appointech.appointech_backend.infrastructure.adapters.PasarelaPagoSimuladaAdapter;
import com.appointech.appointech_backend.infrastructure.adapters.PasarelaPagoWompiPendienteAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Valores configurables del módulo de pagos (todo por variables de entorno). */
@Configuration
public class PagosConfig {

    static final String PASARELA_SIMULADA = "simulada";
    static final String PASARELA_WOMPI = "wompi";

    @Bean
    public ConfiguracionPagos configuracionPagos(
            @Value("${pagos.umbral-diferencia-porcentaje:20}") BigDecimal umbralDiferenciaPorcentaje,
            @Value("${pagos.url-retorno:http://localhost:4200/cliente/pago/resultado}") String urlRetorno) {
        return new ConfiguracionPagos(umbralDiferenciaPorcentaje, urlRetorno);
    }

    /**
     * Única definición del puerto de la pasarela: PASARELA_PAGOS elige la implementación. Es fail-closed: sin valor se usa
     * "wompi" (pago en línea no disponible); la simulada solo se activa con "simulada" explícito y cualquier otro valor
     * impide arrancar.
     */
    @Bean
    public PasarelaPagoPort pasarelaPago(@Value("${pagos.pasarela:" + PASARELA_WOMPI + "}") String pasarela,
                                         @Value("${pagos.simulada.aprobar:true}") boolean aprobarSimulada) {
        return switch (pasarela.strip().toLowerCase()) {
            case PASARELA_SIMULADA -> new PasarelaPagoSimuladaAdapter(aprobarSimulada);
            case PASARELA_WOMPI -> new PasarelaPagoWompiPendienteAdapter();
            default -> throw new IllegalStateException("PASARELA_PAGOS='" + pasarela
                    + "' no es válido: use '" + PASARELA_SIMULADA + "' o '" + PASARELA_WOMPI + "'");
        };
    }
}
