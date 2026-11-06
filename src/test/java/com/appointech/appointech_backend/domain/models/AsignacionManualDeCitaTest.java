package com.appointech.appointech_backend.domain.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsignacionManualDeCitaTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 6, 9, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 5, 2, 15, 0);

    @Test
    void unaCitaNuevaNoEstaAsignadaManualmente() {
        Cita c = new Cita();

        assertFalse(c.isAsignacionManual());
        assertNull(c.getAsignadaPorId());
        assertNull(c.getAsignadaEn());
    }

    @Test
    void asignarManualmenteConfirmaLaCitaYDejaQuienYCuando() {
        Cita c = new Cita();
        c.setEstado(EstadoCita.PENDIENTE);
        c.setAgendamientoManual(true);

        c.asignarManualmente(3L, INICIO, INICIO.plusHours(2), 1L, AHORA);

        assertEquals(EstadoCita.CONFIRMADA, c.getEstado());
        assertEquals(3L, c.getTecnicoId());
        assertEquals(INICIO, c.getFechaHoraInicio());
        assertEquals(INICIO.plusHours(2), c.getFechaHoraFin());
        assertTrue(c.isAsignacionManual());
        assertEquals(1L, c.getAsignadaPorId());
        assertEquals(AHORA, c.getAsignadaEn());
        assertEquals(AHORA, c.getActualizadaEn());
    }
}
