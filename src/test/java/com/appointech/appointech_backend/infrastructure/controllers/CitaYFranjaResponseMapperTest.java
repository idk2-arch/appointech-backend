package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarCitaUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.ConfirmarCitaRequest;
import com.appointech.appointech_backend.infrastructure.dto.FranjaDisponibleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitaYFranjaResponseMapperTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 2, 9, 0);

    @Test
    void franjaExponeSoloNombreYApellidoDelTecnico() {
        FranjaDisponibleResponse r = FranjaResponseMapper.aRespuesta(
                new FranjaDisponible(3L, INICIO, INICIO.plusHours(2), 3.14159, "Ana", "Ruiz"));

        assertEquals(3L, r.tecnicoId());
        assertEquals(3.14, r.distanciaKm());
        assertEquals("Ana", r.tecnicoNombre());
        assertEquals("Ruiz", r.tecnicoApellido());
    }

    @Test
    void citaConfirmadaMapeaDatosDelTecnicoYEspecialidad() {
        Cita cita = cita(3L, EstadoCita.CONFIRMADA);

        CitaResponse r = CitaResponseMapper.aRespuesta(new DetalleCita(cita, "Ana", "Ruiz", "311", "Plomería"));

        assertEquals(10L, r.id());
        assertEquals(3L, r.tecnicoId());
        assertEquals("CONFIRMADA", r.estado());
        assertEquals("Ana", r.tecnicoNombre());
        assertEquals("Ruiz", r.tecnicoApellido());
        assertEquals("311", r.tecnicoTelefono());
        assertEquals("Plomería", r.especialidadNombre());
    }

    @Test
    void solicitudSinTecnicoDejaLosCamposDelTecnicoEnNulo() {
        CitaResponse r = CitaResponseMapper.aRespuesta(
                new DetalleCita(cita(null, EstadoCita.PENDIENTE), null, null, null, "Plomería"));

        assertNull(r.tecnicoId());
        assertNull(r.tecnicoNombre());
        assertNull(r.tecnicoApellido());
        assertNull(r.tecnicoTelefono());
        assertEquals("Plomería", r.especialidadNombre());
    }

    @Test
    void confirmarCitaDevuelveLaRespuestaEnriquecida() {
        ConfirmarCitaUseCase confirmar = mock(ConfirmarCitaUseCase.class);
        ObtenerDetalleCitaUseCase obtenerDetalle = mock(ObtenerDetalleCitaUseCase.class);
        Cita cita = cita(3L, EstadoCita.CONFIRMADA);
        when(confirmar.confirmar(any(), any(), any(), any(), any())).thenReturn(cita);
        when(obtenerDetalle.detallar(cita)).thenReturn(new DetalleCita(cita, "Ana", "Ruiz", "311", "Plomería"));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("cliente@x.com");

        CitaResponse r = new CitaController(confirmar, obtenerDetalle, mock(com.appointech.appointech_backend.domain.ports.in.SolicitarAgendamientoManualUseCase.class))
                .confirmar(new ConfirmarCitaRequest(1L, 3L, INICIO, INICIO.plusHours(2)), auth).getBody();

        assertEquals("Ana", r.tecnicoNombre());
        assertEquals("311", r.tecnicoTelefono());
        assertEquals("Plomería", r.especialidadNombre());
    }

    private static Cita cita(Long tecnicoId, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(10L);
        c.setTecnicoId(tecnicoId);
        c.setEspecialidadId(5L);
        c.setDescripcionProblema("Fuga");
        c.setDireccionServicio("Calle 1");
        c.setFechaHoraInicio(INICIO);
        c.setFechaHoraFin(INICIO.plusHours(2));
        c.setEstado(estado);
        return c;
    }
}
