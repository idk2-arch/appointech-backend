package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.CalificacionNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.CalificacionRecibida;
import com.appointech.appointech_backend.domain.models.CalificacionesDelTecnico;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.PaginacionOpcional;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.ports.out.CalificacionRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Consultas de calificaciones: la del cliente sobre su cita y las que recibió un técnico. */
class ConsultasDeCalificacionUseCasesTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 1, 12, 0);

    private CalificacionRepositoryPort calificacionRepo;

    @BeforeEach
    void preparar() {
        calificacionRepo = mock(CalificacionRepositoryPort.class);
    }

    // ---------- el cliente consulta la suya ----------

    private ConsultarCalificacionUseCaseImpl consultaDelCliente(Long duenoDeLaCita) {
        ResolverClienteAutenticado resolver = mock(ResolverClienteAutenticado.class);
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        when(resolver.resolver("ana@x.com")).thenReturn(cliente);
        CitaRepositoryPort citas = mock(CitaRepositoryPort.class);
        Cita cita = new Cita();
        cita.setId(10L);
        cita.setClienteId(duenoDeLaCita);
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita));
        return new ConsultarCalificacionUseCaseImpl(resolver, citas, calificacionRepo);
    }

    @Test
    void elClienteDuenoVeSuCalificacion() {
        Calificacion guardada = new Calificacion(1L, 10L, 4, "Bien", FECHA);
        when(calificacionRepo.buscarPorCitaId(10L)).thenReturn(Optional.of(guardada));

        assertEquals(guardada, consultaDelCliente(2L).consultar("ana@x.com", 10L));
    }

    @Test
    void sinCalificacionTodaviaEs404() {
        when(calificacionRepo.buscarPorCitaId(10L)).thenReturn(Optional.empty());

        var e = assertThrows(CalificacionNoEncontradaException.class, () -> consultaDelCliente(2L).consultar("ana@x.com", 10L));

        assertEquals("La cita 10 aún no tiene calificación", e.getMessage());
    }

    @Test
    void laCitaDeOtroClienteEs404SinConsultarCalificaciones() {
        var consulta = consultaDelCliente(99L);

        assertThrows(SolicitudNoEncontradaException.class, () -> consulta.consultar("ana@x.com", 10L));
        verify(calificacionRepo, never()).buscarPorCitaId(any());
    }

    @Test
    void unaCitaInexistenteEs404() {
        ResolverClienteAutenticado resolver = mock(ResolverClienteAutenticado.class);
        when(resolver.resolver("ana@x.com")).thenReturn(new Cliente());
        var consulta = new ConsultarCalificacionUseCaseImpl(resolver, mock(CitaRepositoryPort.class), calificacionRepo);

        assertThrows(SolicitudNoEncontradaException.class, () -> consulta.consultar("ana@x.com", 10L));
    }

    // ---------- el técnico ve las que recibió ----------

    private ConsultarCalificacionesTecnicoUseCaseImpl consultaDelTecnico() {
        ResolverTecnicoAutenticado resolver = mock(ResolverTecnicoAutenticado.class);
        Tecnico tecnico = new Tecnico();
        tecnico.setId(3L);
        when(resolver.resolver("luis@x.com")).thenReturn(tecnico);
        return new ConsultarCalificacionesTecnicoUseCaseImpl(resolver, calificacionRepo);
    }

    private static CalificacionRecibida recibida(long citaId, int puntuacion) {
        return new CalificacionRecibida(citaId, puntuacion, "c" + citaId, FECHA, "Ana");
    }

    @Test
    void devuelveElPromedioRedondeadoADosDecimalesElTotalYLasCalificaciones() {
        when(calificacionRepo.listarRecibidasPorTecnico(eq(3L), any()))
                .thenReturn(new ListadoConTotal<>(List.of(recibida(12, 5), recibida(11, 4), recibida(10, 4)), 3));
        when(calificacionRepo.promedioDelTecnico(3L)).thenReturn(Optional.of(4.333333333));

        CalificacionesDelTecnico r = consultaDelTecnico().consultar("luis@x.com", null, null);

        assertEquals(4.33, r.promedio());
        assertEquals(3, r.total());
        assertEquals(List.of(12L, 11L, 10L), r.calificaciones().stream().map(CalificacionRecibida::citaId).toList());
    }

    @Test
    void elRedondeoDelPromedioEsHaciaArribaEnLaMitad() {
        when(calificacionRepo.listarRecibidasPorTecnico(eq(3L), any())).thenReturn(new ListadoConTotal<>(List.of(), 0));
        when(calificacionRepo.promedioDelTecnico(3L)).thenReturn(Optional.of(4.125));

        assertEquals(4.13, consultaDelTecnico().consultar("luis@x.com", null, null).promedio());
    }

    @Test
    void sinCalificacionesElPromedioEsNuloYElTotalCero() {
        when(calificacionRepo.listarRecibidasPorTecnico(eq(3L), any())).thenReturn(new ListadoConTotal<>(List.of(), 0));
        when(calificacionRepo.promedioDelTecnico(3L)).thenReturn(Optional.empty());

        CalificacionesDelTecnico r = consultaDelTecnico().consultar("luis@x.com", null, null);

        assertNull(r.promedio());
        assertEquals(0, r.total());
        assertTrue(r.calificaciones().isEmpty());
    }

    @Test
    void sinPageNiSizeNoPagina() {
        when(calificacionRepo.listarRecibidasPorTecnico(eq(3L), any())).thenReturn(new ListadoConTotal<>(List.of(), 0));
        when(calificacionRepo.promedioDelTecnico(3L)).thenReturn(Optional.empty());

        consultaDelTecnico().consultar("luis@x.com", null, null);

        ArgumentCaptor<PaginacionOpcional> captor = ArgumentCaptor.forClass(PaginacionOpcional.class);
        verify(calificacionRepo).listarRecibidasPorTecnico(eq(3L), captor.capture());
        assertEquals(false, captor.getValue().paginado());
    }

    @Test
    void conPageYSizePideEsaPaginaPeroElTotalYElPromedioSonDeTodas() {
        when(calificacionRepo.listarRecibidasPorTecnico(eq(3L), any()))
                .thenReturn(new ListadoConTotal<>(List.of(recibida(10, 3)), 41));
        when(calificacionRepo.promedioDelTecnico(3L)).thenReturn(Optional.of(3.5));

        CalificacionesDelTecnico r = consultaDelTecnico().consultar("luis@x.com", 4, 10);

        ArgumentCaptor<PaginacionOpcional> captor = ArgumentCaptor.forClass(PaginacionOpcional.class);
        verify(calificacionRepo).listarRecibidasPorTecnico(eq(3L), captor.capture());
        assertEquals(new PaginacionOpcional(4, 10), captor.getValue());
        assertEquals(41, r.total());
        assertEquals(3.5, r.promedio());
        assertEquals(1, r.calificaciones().size());
    }

    @Test
    void paginaNegativaOTamanioFueraDeRangoSon400SinConsultar() {
        var consulta = consultaDelTecnico();

        assertThrows(FiltroCitasInvalidoException.class, () -> consulta.consultar("luis@x.com", -1, 10));
        assertThrows(FiltroCitasInvalidoException.class, () -> consulta.consultar("luis@x.com", 0, 101));
        assertThrows(FiltroCitasInvalidoException.class, () -> consulta.consultar("luis@x.com", 0, 0));
        verify(calificacionRepo, never()).listarRecibidasPorTecnico(any(), any());
    }
}
