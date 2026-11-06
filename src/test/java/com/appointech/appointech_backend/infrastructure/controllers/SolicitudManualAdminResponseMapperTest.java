package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.infrastructure.dto.SolicitudManualAdminResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SolicitudManualAdminResponseMapperTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 5, 6, 9, 0);

    private static SolicitudManualDetalle detalle(MotivoSolicitudEspecial motivo) {
        return new SolicitudManualDetalle(70L, 2L, "Ana", "Ruiz", "300", "Calle 1", "Fuga", 5L, "Plomería",
                4.5, -74.1, FECHA, motivo, EstadoCita.PENDIENTE);
    }

    @Test
    void incluyeElMotivoAlFinalDeLaRespuesta() {
        SolicitudManualAdminResponse r = SolicitudManualAdminResponseMapper
                .aRespuesta(detalle(MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD));

        assertEquals(70L, r.id());
        assertEquals("300", r.telefono());
        assertEquals(FECHA, r.creadaEn());
        assertEquals("SIN_TECNICOS_ESPECIALIDAD", r.motivo());
        assertEquals("PENDIENTE", r.estado());
    }

    @Test
    void unaSolicitudAntiguaSinMotivoLoDevuelveNulo() {
        assertNull(SolicitudManualAdminResponseMapper.aRespuesta(detalle(null)).motivo());
    }
}
