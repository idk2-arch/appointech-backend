package com.appointech.appointech_backend.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroSolicitudesManualesTest {

    @Test
    void soloEstaPaginadoCuandoVienenPaginaYTamanio() {
        assertTrue(new FiltroSolicitudesManuales(null, null, null, 0, 10).paginado());
        assertFalse(new FiltroSolicitudesManuales(null, null, null, null, null).paginado());
        assertFalse(new FiltroSolicitudesManuales(null, null, null, 0, null).paginado());
        assertFalse(new FiltroSolicitudesManuales(null, null, null, null, 10).paginado());
    }

    @Test
    void sinFiltrosNoFiltraNiPagina() {
        FiltroSolicitudesManuales f = FiltroSolicitudesManuales.sinFiltros();

        assertFalse(f.paginado());
        assertTrue(f.motivo() == null && f.especialidadId() == null && f.estado() == null);
    }
}
