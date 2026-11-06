package com.appointech.appointech_backend.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalificacionConfigTest {

    @Test
    void elPlazoConfiguradoLlegaALaConfiguracion() {
        assertEquals(7, new CalificacionConfig().configuracionCalificacion(7).plazoDias());
    }

    @Test
    void unPlazoInvalidoImpideArrancar() {
        assertThrows(IllegalArgumentException.class, () -> new CalificacionConfig().configuracionCalificacion(0));
    }
}
