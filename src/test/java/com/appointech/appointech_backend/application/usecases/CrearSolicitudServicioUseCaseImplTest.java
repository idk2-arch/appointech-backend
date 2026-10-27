package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CrearSolicitudServicioUseCaseImplTest {

    // Las coordenadas se validan antes de consultar cualquier puerto, por eso no se necesitan simulados.
    private final CrearSolicitudServicioUseCaseImpl useCase =
            new CrearSolicitudServicioUseCaseImpl(null, null, null, null, null);

    @Test
    void rechazaCoordenadasInvertidas() {
        assertThrows(CoordenadasInvalidasException.class,
                () -> useCase.crear("c@x.com", 1L, "falla", "Calle 1", -74.36, 4.33));
    }

    @Test
    void rechazaCoordenadasFueraDeColombia() {
        assertThrows(CoordenadasInvalidasException.class,
                () -> useCase.crear("c@x.com", 1L, "falla", "Calle 1", 40.4, -3.7));
    }

    @Test
    void rechazaCoordenadasFueraDeRangoMundial() {
        assertThrows(CoordenadasInvalidasException.class,
                () -> useCase.crear("c@x.com", 1L, "falla", "Calle 1", 95.0, -74.0));
    }
}
