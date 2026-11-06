package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.infrastructure.dto.CalificacionAdminResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleAdminResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CalificacionMapperTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 1, 12, 0);

    private static DetalleCita detalle() {
        Cita c = new Cita();
        c.setId(10L);
        c.setEstado(EstadoCita.FINALIZADA);
        c.setEspecialidadId(5L);
        c.setTecnicoId(3L);
        return new DetalleCita(c, "Luis", "Mora", "311", "Plomería");
    }

    @Test
    void elDetalleDelAdminTraeLaCalificacionAlFinal() {
        CitaDetalleAdminResponse r = CitaDetalleMapper.aRespuestaAdmin(
                new DetalleCitaCompleto(detalle(), List.of(), List.of(), new Calificacion(1L, 10L, 4, "Bien", FECHA)));

        assertEquals(new CalificacionAdminResponse(4, "Bien", FECHA), r.calificacion());
        assertEquals(10L, r.id());
        assertEquals("Luis", r.tecnicoNombre());
        var componentes = CitaDetalleAdminResponse.class.getRecordComponents();
        assertEquals("calificacion", componentes[componentes.length - 1].getName());
    }

    @Test
    void sinCalificacionElDetalleDelAdminTraeNulo() {
        assertNull(CitaDetalleMapper.aRespuestaAdmin(new DetalleCitaCompleto(detalle(), List.of())).calificacion());
    }

    @Test
    void elDetalleDelAdminConservaTodosLosCamposDelDetalleDelCliente() {
        var delCliente = Arrays.stream(CitaDetalleResponse.class.getRecordComponents()).map(c -> c.getName()).toList();
        var delAdmin = Arrays.stream(CitaDetalleAdminResponse.class.getRecordComponents()).map(c -> c.getName()).toList();

        assertEquals(delCliente, delAdmin.subList(0, delCliente.size()));
        assertEquals(List.of("calificacion"), delAdmin.subList(delCliente.size(), delAdmin.size()));
    }

    @Test
    void laCalificacionNoApareceEnLasRespuestasDelClienteNiDelTecnico() {
        var detalleCliente = Arrays.stream(CitaDetalleResponse.class.getRecordComponents()).map(c -> c.getName()).toList();
        var citaResponse = Arrays.stream(CitaResponse.class.getRecordComponents()).map(c -> c.getName()).toList();

        assertFalse(detalleCliente.contains("calificacion"));
        assertFalse(citaResponse.contains("calificacion"));
    }
}
