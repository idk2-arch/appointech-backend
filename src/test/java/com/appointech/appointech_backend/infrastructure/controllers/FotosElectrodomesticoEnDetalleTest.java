package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaConCliente;
import com.appointech.appointech_backend.domain.models.CitaFoto;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.infrastructure.dto.CitaDetalleResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaTecnicoResponse;
import com.appointech.appointech_backend.infrastructure.dto.FotoCitaResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** fotosElectrodomestico: lista de {id, url}, vacía (nunca null) cuando la cita no tiene fotos. */
class FotosElectrodomesticoEnDetalleTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 8, 30);

    private Cita cita() {
        Cita c = new Cita();
        c.setId(20L);
        c.setEstado(EstadoCita.CONFIRMADA);
        return c;
    }

    private List<CitaFoto> dosFotos() {
        return List.of(new CitaFoto(5L, 20L, "citas/20/a", AHORA), new CitaFoto(6L, 20L, "citas/20/b", AHORA));
    }

    @Test
    void elDetalleParaClienteYAdministradorListaLasFotosConIdYUrlDeLaRutaAutenticada() {
        DetalleCita detalle = new DetalleCita(cita(), "Luis", "Perez", "300", "Plomeria");

        CitaDetalleResponse respuesta = CitaDetalleMapper.aRespuesta(new DetalleCitaCompleto(detalle, List.of(), dosFotos()));

        assertEquals(List.of(new FotoCitaResponse(5L, "/api/citas/20/fotos/5"),
                new FotoCitaResponse(6L, "/api/citas/20/fotos/6")), respuesta.fotosElectrodomestico());
    }

    @Test
    void sinFotosElDetalleDevuelveListaVaciaYNoNull() {
        DetalleCita detalle = new DetalleCita(cita(), null, null, null, "Plomeria");

        assertTrue(CitaDetalleMapper.aRespuesta(new DetalleCitaCompleto(detalle, List.of(), List.of()))
                .fotosElectrodomestico().isEmpty());
        assertTrue(CitaDetalleMapper.aRespuesta(new DetalleCitaCompleto(detalle, List.of()))
                .fotosElectrodomestico().isEmpty());
    }

    @Test
    void elTecnicoVeLasFotosDeLaCitaEnSuAgenda() {
        CitaConCliente conFotos = new CitaConCliente(cita(), new ClienteResumen("Ana", "Gomez", "300"), dosFotos());
        CitaConCliente sinFotos = new CitaConCliente(cita(), null);

        assertEquals(List.of(new FotoCitaResponse(5L, "/api/citas/20/fotos/5"),
                new FotoCitaResponse(6L, "/api/citas/20/fotos/6")),
                TecnicoAgendaMappers.aRespuesta(conFotos).fotosElectrodomestico());
        assertTrue(TecnicoAgendaMappers.aRespuesta(sinFotos).fotosElectrodomestico().isEmpty());
    }

    @Test
    void elNombreDelCampoJsonEsExactamenteFotosElectrodomestico() {
        assertEquals("fotosElectrodomestico", ultimoComponente(CitaDetalleResponse.class));
        assertEquals("fotosElectrodomestico", ultimoComponente(CitaTecnicoResponse.class));
        assertEquals(List.of("id", "url"), Arrays.stream(FotoCitaResponse.class.getRecordComponents())
                .map(c -> c.getName()).toList());
    }

    private String ultimoComponente(Class<? extends Record> tipo) {
        var componentes = tipo.getRecordComponents();
        return componentes[componentes.length - 1].getName();
    }
}
