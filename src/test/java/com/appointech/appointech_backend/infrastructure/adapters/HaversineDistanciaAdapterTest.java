package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineDistanciaAdapterTest {

    private static final Coordenadas PUNTO = new Coordenadas(4.3365, -74.3638);

    @Test
    void mismasCoordenadasDanCeroKilometrosYCeroMinutos() {
        Distancia d = new HaversineDistanciaAdapter().calcular(PUNTO, List.of(PUNTO)).get(0);

        assertEquals(0.0, d.kilometros());
        assertEquals(0.0, d.minutos());
    }

    @Test
    void coordenadasDistintasDanMinutosPositivos() {
        Distancia d = new HaversineDistanciaAdapter().calcular(PUNTO, List.of(new Coordenadas(4.40, -74.30))).get(0);

        assertTrue(d.kilometros() > 0);
        assertTrue(d.minutos() > 0);
    }
}
