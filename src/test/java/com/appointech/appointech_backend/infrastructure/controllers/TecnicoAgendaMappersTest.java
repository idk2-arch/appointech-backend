package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaConCliente;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.ParadaRuta;
import com.appointech.appointech_backend.domain.models.RutaDelDia;
import com.appointech.appointech_backend.infrastructure.dto.BloqueResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaTecnicoResponse;
import com.appointech.appointech_backend.infrastructure.dto.RutaResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TecnicoAgendaMappersTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 3, 9, 0);

    @Test
    void bloqueSeTraduceConSusCuatroCampos() {
        BloqueIndisponibilidad bloque = new BloqueIndisponibilidad(3L, 1L, INICIO, INICIO.plusHours(2), "Almuerzo");

        BloqueResponse r = TecnicoAgendaMappers.aRespuesta(bloque);

        assertEquals(3L, r.id());
        assertEquals(INICIO, r.inicio());
        assertEquals(INICIO.plusHours(2), r.fin());
        assertEquals("Almuerzo", r.motivo());
    }

    @Test
    void citaConClienteIncluyeEstadoComoTextoYResumenDelCliente() {
        Cita cita = new Cita();
        cita.setId(7L);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(INICIO);
        cita.setFechaHoraFin(INICIO.plusHours(1));
        cita.setDescripcionProblema("fuga");
        cita.setDireccionServicio("Calle 1");
        cita.setLatitud(4.6);
        cita.setLongitud(-74.1);

        CitaTecnicoResponse r = TecnicoAgendaMappers.aRespuesta(
                new CitaConCliente(cita, new ClienteResumen("Luis", "Perez", "300")));

        assertEquals(7L, r.id());
        assertEquals("CONFIRMADA", r.estado());
        assertEquals("fuga", r.descripcionProblema());
        assertEquals("Calle 1", r.direccionServicio());
        assertEquals(4.6, r.latitud());
        assertEquals("Luis", r.cliente().nombre());
        assertEquals("300", r.cliente().telefono());
    }

    @Test
    void citaSinClienteResumenDejaClienteNulo() {
        Cita cita = new Cita();
        cita.setEstado(EstadoCita.PENDIENTE);

        assertNull(TecnicoAgendaMappers.aRespuesta(new CitaConCliente(cita, null)).cliente());
    }

    @Test
    void rutaRedondeaADosDecimalesYConservaElOrdenDeLasParadas() {
        ParadaRuta p1 = new ParadaRuta(1, 10L, INICIO, INICIO.plusHours(1), "Calle 1",
                new Coordenadas(4.7, -74.0), new ClienteResumen("Luis", "Perez", "300"),
                3.14159, 7.777, INICIO.minusMinutes(5), true, 0);
        ParadaRuta p2 = new ParadaRuta(2, 11L, INICIO.plusHours(2), INICIO.plusHours(3), "Calle 2",
                new Coordenadas(4.8, -74.1), null,
                1.005, 2.0, INICIO.plusHours(2).plusMinutes(10), false, 10);
        RutaDelDia ruta = new RutaDelDia(LocalDate.of(2026, 8, 3), INICIO, new Coordenadas(4.6, -74.0),
                List.of(p1, p2), 4.14159, 9.777);

        RutaResponse r = TecnicoAgendaMappers.aRespuesta(ruta);

        assertEquals(LocalDate.of(2026, 8, 3), r.fecha());
        assertEquals(4.6, r.origen().latitud());
        assertEquals(4.14, r.totalKm());
        assertEquals(9.78, r.totalMinutosDeViaje());
        assertEquals(2, r.paradas().size());
        RutaResponse.Parada a = r.paradas().get(0);
        assertEquals(1, a.orden());
        assertEquals(10L, a.citaId());
        assertEquals(3.14, a.distanciaKm());
        assertEquals(7.78, a.minutosDesdeAnterior());
        assertEquals("Luis", a.cliente().nombre());
        assertTrue(a.alcanzaATiempo());
        RutaResponse.Parada b = r.paradas().get(1);
        assertNull(b.cliente());
        assertFalse(b.alcanzaATiempo());
        assertEquals(10, b.retrasoEstimadoMinutos());
        assertEquals(4.8, b.latitud());
    }
}
