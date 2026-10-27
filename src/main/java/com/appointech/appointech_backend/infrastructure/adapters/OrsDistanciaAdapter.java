package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Distancia por carretera con la API Matrix de OpenRouteService.
 * Un origen y hasta 25 destinos por petición; si algo falla se usa el respaldo (Haversine).
 * La clave solo viaja en el encabezado Authorization y nunca se escribe en los logs.
 */
public class OrsDistanciaAdapter implements CalculadoraDistanciaPort {

    static final int MAXIMO_DESTINOS_POR_PETICION = 25;

    private static final Logger log = LoggerFactory.getLogger(OrsDistanciaAdapter.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String rutaMatrix;
    private final String prefijoAutorizacion;
    private final CalculadoraDistanciaPort respaldo;

    public OrsDistanciaAdapter(RestClient restClient, String apiKey, String rutaMatrix,
                               String prefijoAutorizacion, CalculadoraDistanciaPort respaldo) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.rutaMatrix = rutaMatrix;
        this.prefijoAutorizacion = prefijoAutorizacion == null ? "" : prefijoAutorizacion;
        this.respaldo = respaldo;
    }

    @Override
    public List<Distancia> calcular(Coordenadas origen, List<Coordenadas> destinos) {
        if (destinos.isEmpty()) {
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ORS_API_KEY no está configurada; se usa Haversine para {} destino(s)", destinos.size());
            return respaldo.calcular(origen, destinos);
        }

        try {
            List<Distancia> resultado = new ArrayList<>(destinos.size());
            for (int desde = 0; desde < destinos.size(); desde += MAXIMO_DESTINOS_POR_PETICION) {
                int hasta = Math.min(desde + MAXIMO_DESTINOS_POR_PETICION, destinos.size());
                resultado.addAll(calcularLote(origen, destinos.subList(desde, hasta)));
            }
            return resultado;
        } catch (RestClientResponseException e) {
            log.warn("ORS respondió HTTP {}; se usa Haversine para {} destino(s)",
                    e.getStatusCode().value(), destinos.size());
        } catch (RuntimeException e) {
            log.warn("Falló la llamada a ORS ({}); se usa Haversine para {} destino(s)",
                    e.getClass().getSimpleName(), destinos.size());
        }
        return respaldo.calcular(origen, destinos);
    }

    private List<Distancia> calcularLote(Coordenadas origen, List<Coordenadas> lote) {
        // ORS usa el orden [longitud, latitud]
        List<List<Double>> locaciones = new ArrayList<>();
        locaciones.add(List.of(origen.longitud(), origen.latitud()));
        List<String> indicesDestino = new ArrayList<>();
        for (int i = 0; i < lote.size(); i++) {
            locaciones.add(List.of(lote.get(i).longitud(), lote.get(i).latitud()));
            indicesDestino.add(String.valueOf(i + 1));
        }

        Map<String, Object> cuerpo = Map.of(
                "locations", locaciones,
                "sources", List.of("0"),
                "destinations", indicesDestino,
                "metrics", List.of("distance", "duration"));

        Map<String, Object> respuesta = restClient.post()
                .uri(rutaMatrix)
                .header("Authorization", prefijoAutorizacion + apiKey)
                .body(cuerpo)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });

        List<?> metros = primeraFila(respuesta, "distances");
        List<?> segundos = primeraFila(respuesta, "durations");
        if (metros == null || metros.size() != lote.size()) {
            throw new IllegalStateException("Respuesta de ORS sin las distancias esperadas");
        }

        List<Distancia> distancias = new ArrayList<>(lote.size());
        List<Coordenadas> sinRuta = new ArrayList<>();
        List<Integer> posicionesSinRuta = new ArrayList<>();
        for (int i = 0; i < lote.size(); i++) {
            Object m = metros.get(i);
            Object s = segundos != null && segundos.size() == lote.size() ? segundos.get(i) : null;
            if (m instanceof Number metrosN && s instanceof Number segundosN) {
                distancias.add(new Distancia(metrosN.doubleValue() / 1000.0, segundosN.doubleValue() / 60.0));
            } else {
                distancias.add(null);
                sinRuta.add(lote.get(i));
                posicionesSinRuta.add(i);
            }
        }

        if (!sinRuta.isEmpty()) {
            log.warn("ORS no encontró ruta para {} destino(s); se usa Haversine solo para esos", sinRuta.size());
            List<Distancia> aproximadas = respaldo.calcular(origen, sinRuta);
            for (int k = 0; k < posicionesSinRuta.size(); k++) {
                distancias.set(posicionesSinRuta.get(k), aproximadas.get(k));
            }
        }
        return distancias;
    }

    private List<?> primeraFila(Map<String, Object> respuesta, String clave) {
        if (respuesta == null || !(respuesta.get(clave) instanceof List<?> filas) || filas.isEmpty()) {
            return null;
        }
        return filas.get(0) instanceof List<?> fila ? fila : null;
    }
}
