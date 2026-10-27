package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.infrastructure.adapters.HaversineDistanciaAdapter;
import com.appointech.appointech_backend.infrastructure.adapters.OrsDistanciaAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class MapasConfig {

    private static final Logger log = LoggerFactory.getLogger(MapasConfig.class);

    @Bean
    public CalculadoraDistanciaPort calculadoraDistanciaPort(
            @Value("${mapas.proveedor:ors}") String proveedor,
            @Value("${ors.api-key:}") String apiKey,
            @Value("${ors.base-url:https://api.heigit.org}") String baseUrl,
            @Value("${ors.matrix-path:/openrouteservice/v2/matrix/driving-car}") String rutaMatrix,
            @Value("${ors.auth-prefix:}") String prefijoAutorizacion,
            @Value("${ors.timeout-conexion-ms:3000}") long timeoutConexionMs,
            @Value("${ors.timeout-lectura-ms:5000}") long timeoutLecturaMs,
            @Value("${mapas.haversine.velocidad-kmh:40}") double velocidadHaversineKmH) {

        HaversineDistanciaAdapter haversine = new HaversineDistanciaAdapter(velocidadHaversineKmH);
        if (!"ors".equalsIgnoreCase(proveedor)) {
            log.info("Proveedor de mapas: haversine (línea recta)");
            return haversine;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("mapas.proveedor=ors pero ORS_API_KEY está vacía; se usará Haversine hasta configurarla");
        }

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutConexionMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutLecturaMs));
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .build();
        return new OrsDistanciaAdapter(restClient, apiKey, rutaMatrix, prefijoAutorizacion, haversine);
    }
}
