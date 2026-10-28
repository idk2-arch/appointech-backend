package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordenadasTest {

    @Test
    void aceptaCoordenadasDeColombia() {
        Coordenadas c = new Coordenadas(4.3365, -74.3638);
        assertEquals(4.3365, c.latitud());
        assertEquals(-74.3638, c.longitud());
    }

    @Test
    void rechazaLatitudYLongitudInvertidas() {
        assertThrows(CoordenadasInvalidasException.class, () -> new Coordenadas(-74.3638, 4.3365));
    }

    @Test
    void rechazaValoresFueraDelRangoMundial() {
        assertThrows(CoordenadasInvalidasException.class, () -> new Coordenadas(91, -74));
        assertThrows(CoordenadasInvalidasException.class, () -> new Coordenadas(4, -181));
        assertThrows(CoordenadasInvalidasException.class, () -> new Coordenadas(Double.NaN, -74));
    }

    @Test
    void rechazaPuntosValidosFueraDeColombia() {
        assertThrows(CoordenadasInvalidasException.class, () -> new Coordenadas(40.4, -3.7));
        assertThrows(CoordenadasInvalidasException.class, () -> new Coordenadas(15, -74));
        assertThrows(CoordenadasInvalidasException.class, () -> new Coordenadas(4, -65));
    }

    @Test
    void aceptaLosLimitesDeColombia() {
        Coordenadas suroeste = new Coordenadas(-5, -82);
        Coordenadas noreste = new Coordenadas(14, -66);

        assertEquals(-5, suroeste.latitud());
        assertEquals(-82, suroeste.longitud());
        assertEquals(14, noreste.latitud());
        assertEquals(-66, noreste.longitud());
    }

    @Test
    void opcionalExigeAmbosValoresONinguno() {
        assertNull(Coordenadas.opcional(null, null));
        assertEquals(new Coordenadas(4, -74), Coordenadas.opcional(4.0, -74.0));
        assertThrows(CoordenadasInvalidasException.class, () -> Coordenadas.opcional(4.0, null));
        assertThrows(CoordenadasInvalidasException.class, () -> Coordenadas.opcional(null, -74.0));
        assertThrows(CoordenadasInvalidasException.class, () -> Coordenadas.de(null, null));
    }
}
