package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Sin llamadas reales: el servidor de ORS se simula con MockRestServiceServer. */
class OrsDistanciaAdapterTest {

    private static final String URL = "https://ors.prueba/openrouteservice/v2/matrix/driving-car";
    private static final Coordenadas ORIGEN = new Coordenadas(4.3365, -74.3638);
    private static final Coordenadas DESTINO_A = new Coordenadas(4.40, -74.30);
    private static final Coordenadas DESTINO_B = new Coordenadas(4.50, -74.20);

    private final HaversineDistanciaAdapter haversine = new HaversineDistanciaAdapter();
    private MockRestServiceServer servidor;
    private RestClient.Builder builder;

    @BeforeEach
    void preparar() {
        builder = RestClient.builder().baseUrl("https://ors.prueba");
        servidor = MockRestServiceServer.bindTo(builder).build();
    }

    private OrsDistanciaAdapter adapter(String clave) {
        return new OrsDistanciaAdapter(builder.build(), clave,
                "/openrouteservice/v2/matrix/driving-car", "", haversine);
    }

    @Test
    void conviertePeticionYRespuestaAKmYMinutos() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "clave-secreta"))
                .andExpect(jsonPath("$.locations[0][0]").value(-74.3638))
                .andExpect(jsonPath("$.locations[0][1]").value(4.3365))
                .andExpect(jsonPath("$.sources[0]").value("0"))
                .andExpect(jsonPath("$.destinations[0]").value("1"))
                .andExpect(jsonPath("$.destinations[1]").value("2"))
                .andExpect(jsonPath("$.metrics[0]").value("distance"))
                .andExpect(jsonPath("$.metrics[1]").value("duration"))
                .andRespond(withSuccess("{\"distances\":[[12500.0,3000.0]],\"durations\":[[1500.0,300.0]]}",
                        MediaType.APPLICATION_JSON));

        List<Distancia> r = adapter("clave-secreta").calcular(ORIGEN, List.of(DESTINO_A, DESTINO_B));

        assertEquals(2, r.size());
        assertEquals(12.5, r.get(0).kilometros(), 1e-9);
        assertEquals(25.0, r.get(0).minutos(), 1e-9);
        assertEquals(3.0, r.get(1).kilometros(), 1e-9);
        servidor.verify();
    }

    @Test
    void usaHaversineSoloParaElDestinoSinRuta() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"distances\":[[null,3000.0]],\"durations\":[[null,300.0]]}", MediaType.APPLICATION_JSON));

        List<Distancia> r = adapter("clave").calcular(ORIGEN, List.of(DESTINO_A, DESTINO_B));

        assertEquals(haversine.calcular(ORIGEN, List.of(DESTINO_A)).get(0), r.get(0));
        assertEquals(3.0, r.get(1).kilometros(), 1e-9);
    }

    @Test
    void usaHaversineParaTodoCuandoOrsFalla() {
        for (HttpStatus status : List.of(HttpStatus.UNAUTHORIZED, HttpStatus.TOO_MANY_REQUESTS,
                HttpStatus.INTERNAL_SERVER_ERROR)) {
            servidor.reset();
            servidor.expect(requestTo(URL)).andRespond(withStatus(status));

            List<Distancia> r = adapter("clave").calcular(ORIGEN, List.of(DESTINO_A, DESTINO_B));

            assertEquals(haversine.calcular(ORIGEN, List.of(DESTINO_A, DESTINO_B)), r,
                    "respaldo con HTTP " + status.value());
        }
    }

    @Test
    void usaHaversineSinLlamarAOrsCuandoNoHayClave() {
        List<Distancia> r = adapter("").calcular(ORIGEN, List.of(DESTINO_A));

        assertEquals(haversine.calcular(ORIGEN, List.of(DESTINO_A)), r);
        servidor.verify();
    }

    @Test
    void divideEnLotesDeMaximo25Destinos() {
        List<Coordenadas> destinos = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            destinos.add(DESTINO_A);
        }
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.destinations.length()").value(25))
                .andRespond(withSuccess(respuestaDe(25, 1000), MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.destinations.length()").value(5))
                .andRespond(withSuccess(respuestaDe(5, 2000), MediaType.APPLICATION_JSON));

        List<Distancia> r = adapter("clave").calcular(ORIGEN, destinos);

        assertEquals(30, r.size());
        assertEquals(1.0, r.get(24).kilometros(), 1e-9);
        assertEquals(2.0, r.get(25).kilometros(), 1e-9);
        servidor.verify();
    }

    private String respuestaDe(int cantidad, int metros) {
        String fila = String.join(",", Collections.nCopies(cantidad, metros + ".0"));
        return "{\"distances\":[[" + fila + "]],\"durations\":[[" + fila + "]]}";
    }
}
