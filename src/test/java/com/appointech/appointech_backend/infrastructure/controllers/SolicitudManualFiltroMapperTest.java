package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroSolicitudesManuales;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitudManualFiltroMapperTest {

    @Test
    void sinParametrosNoFiltraNiPagina() {
        assertEquals(FiltroSolicitudesManuales.sinFiltros(),
                SolicitudManualFiltroMapper.aFiltro(null, null, null, null, null));
    }

    @Test
    void textoEnBlancoSeTrataComoSinFiltro() {
        assertEquals(FiltroSolicitudesManuales.sinFiltros(),
                SolicitudManualFiltroMapper.aFiltro("  ", null, "", null, null));
    }

    @Test
    void conviertePasaYNormalizaMayusculasYEspacios() {
        assertEquals(new FiltroSolicitudesManuales(MotivoSolicitudEspecial.FUERA_DE_COBERTURA, 4L, EstadoCita.CANCELADA, 1, 25),
                SolicitudManualFiltroMapper.aFiltro(" fuera_de_cobertura ", 4L, "Cancelada", 1, 25));
    }

    @Test
    void motivoDesconocidoEsUnFiltroInvalido() {
        var e = assertThrows(FiltroCitasInvalidoException.class,
                () -> SolicitudManualFiltroMapper.aFiltro("OTRO", null, null, null, null));
        assertEquals("Valor inválido para motivo: OTRO", e.getMessage());
    }

    @Test
    void estadoDesconocidoEsUnFiltroInvalido() {
        var e = assertThrows(FiltroCitasInvalidoException.class,
                () -> SolicitudManualFiltroMapper.aFiltro(null, null, "EN_LA_LUNA", null, null));
        assertEquals("Valor inválido para estado: EN_LA_LUNA", e.getMessage());
    }
}
