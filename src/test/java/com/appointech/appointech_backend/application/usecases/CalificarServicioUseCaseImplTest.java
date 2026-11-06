package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.CalificacionInvalidaException;
import com.appointech.appointech_backend.domain.exception.CitaNoFinalizadaException;
import com.appointech.appointech_backend.domain.exception.CitaYaCalificadaException;
import com.appointech.appointech_backend.domain.exception.PlazoCalificacionVencidoException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Calificacion;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.ConfiguracionCalificacion;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.ports.out.CalificacionRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.HistorialEstadoCitaPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalificarServicioUseCaseImplTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 1, 12, 0);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private ResolverClienteAutenticado resolverCliente;
    private CitaRepositoryPort citaRepo;
    private CalificacionRepositoryPort calificacionRepo;
    private HistorialEstadoCitaPort historial;
    private CalificarServicioUseCaseImpl casoDeUso;

    @BeforeEach
    void preparar() {
        resolverCliente = mock(ResolverClienteAutenticado.class);
        citaRepo = mock(CitaRepositoryPort.class);
        calificacionRepo = mock(CalificacionRepositoryPort.class);
        historial = mock(HistorialEstadoCitaPort.class);
        casoDeUso = new CalificarServicioUseCaseImpl(resolverCliente, citaRepo, calificacionRepo, historial,
                new ConfiguracionCalificacion(30), RELOJ);

        Cliente cliente = new Cliente();
        cliente.setId(2L);
        when(resolverCliente.resolver("ana@x.com")).thenReturn(cliente);
        when(citaRepo.buscarPorId(10L)).thenReturn(Optional.of(cita(2L, EstadoCita.FINALIZADA)));
        when(calificacionRepo.buscarPorCitaId(10L)).thenReturn(Optional.empty());
        when(calificacionRepo.guardar(any(Calificacion.class))).thenAnswer(i -> {
            Calificacion c = i.getArgument(0);
            return new Calificacion(77L, c.citaId(), c.puntuacion(), c.comentario(), c.creadaEn());
        });
        finalizadaHace(5);
    }

    private static Cita cita(Long clienteId, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(10L);
        c.setClienteId(clienteId);
        c.setEstado(estado);
        return c;
    }

    private void finalizadaEl(LocalDateTime fecha) {
        when(historial.listarPorCita(10L)).thenReturn(List.of(
                new HistorialEstadoCita(1L, 10L, EstadoCita.EN_PROCESO, EstadoCita.FINALIZADA, fecha, "tec@x.com", null)));
    }

    private void finalizadaHace(int dias) {
        finalizadaEl(AHORA.minusDays(dias));
    }

    @Test
    void elClienteDuenoCalificaUnaCitaFinalizada() {
        Calificacion r = casoDeUso.calificar("ana@x.com", 10L, 5, "Excelente");

        assertEquals(77L, r.id());
        assertEquals(10L, r.citaId());
        assertEquals(5, r.puntuacion());
        assertEquals("Excelente", r.comentario());
        assertEquals(AHORA, r.creadaEn());
    }

    @Test
    void laFechaDeLaCalificacionSaleDelRelojInyectado() {
        casoDeUso.calificar("ana@x.com", 10L, 4, null);

        org.mockito.ArgumentCaptor<Calificacion> guardada = org.mockito.ArgumentCaptor.forClass(Calificacion.class);
        verify(calificacionRepo).guardar(guardada.capture());
        assertEquals(AHORA, guardada.getValue().creadaEn());
        assertNull(guardada.getValue().comentario());
    }

    @Test
    void unComentarioEnBlancoSeGuardaComoNulo() {
        assertNull(casoDeUso.calificar("ana@x.com", 10L, 3, "   ").comentario());
    }

    @Test
    void laCitaDeOtroClienteEs404YNoSeGuardaNada() {
        when(citaRepo.buscarPorId(10L)).thenReturn(Optional.of(cita(99L, EstadoCita.FINALIZADA)));

        assertThrows(SolicitudNoEncontradaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 5, null));
        verify(calificacionRepo, never()).guardar(any());
    }

    @Test
    void unaCitaInexistenteEs404() {
        when(citaRepo.buscarPorId(10L)).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 5, null));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = "FINALIZADA", mode = EnumSource.Mode.EXCLUDE)
    void soloSePuedeCalificarUnaCitaFinalizadaEnCualquierOtroEstadoEs409(EstadoCita estado) {
        when(citaRepo.buscarPorId(10L)).thenReturn(Optional.of(cita(2L, estado)));

        var e = assertThrows(CitaNoFinalizadaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 5, null));

        assertEquals("Solo puedes calificar una cita finalizada (estado actual: " + estado + ")", e.getMessage());
        verify(calificacionRepo, never()).guardar(any());
    }

    @Test
    void unaCitaYaCalificadaEs409() {
        when(calificacionRepo.buscarPorCitaId(10L))
                .thenReturn(Optional.of(new Calificacion(1L, 10L, 3, null, AHORA.minusDays(1))));

        var e = assertThrows(CitaYaCalificadaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 5, null));

        assertEquals("Esta cita ya fue calificada", e.getMessage());
        verify(calificacionRepo, never()).guardar(any());
    }

    @Test
    void justoEnElLimiteDelPlazoAunSePuedeCalificar() {
        finalizadaEl(AHORA.minusDays(30));

        assertEquals(5, casoDeUso.calificar("ana@x.com", 10L, 5, null).puntuacion());
    }

    @Test
    void unSegundoDespuesDelLimiteElPlazoEstaVencido() {
        finalizadaEl(AHORA.minusDays(30).minusSeconds(1));

        var e = assertThrows(PlazoCalificacionVencidoException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 5, null));

        assertEquals("El plazo de 30 días para calificar esta cita ya venció", e.getMessage());
        verify(calificacionRepo, never()).guardar(any());
    }

    @Test
    void siLaCitaSeFinalizoVariasVecesCuentaLaUltimaFinalizacion() {
        when(historial.listarPorCita(10L)).thenReturn(List.of(
                new HistorialEstadoCita(1L, 10L, EstadoCita.EN_PROCESO, EstadoCita.FINALIZADA, AHORA.minusDays(90), "tec@x.com", null),
                new HistorialEstadoCita(2L, 10L, EstadoCita.EN_PROCESO, EstadoCita.FINALIZADA, AHORA.minusDays(2), "tec@x.com", null)));

        assertEquals(4, casoDeUso.calificar("ana@x.com", 10L, 4, null).puntuacion());
    }

    @Test
    void sinFilaDeFinalizacionEnElHistorialUsaLaUltimaActualizacionDeLaCita() {
        Cita antigua = cita(2L, EstadoCita.FINALIZADA);
        antigua.setActualizadaEn(AHORA.minusDays(31));
        when(citaRepo.buscarPorId(10L)).thenReturn(Optional.of(antigua));
        when(historial.listarPorCita(10L)).thenReturn(List.of());

        assertThrows(PlazoCalificacionVencidoException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 5, null));

        antigua.setActualizadaEn(AHORA.minusDays(10));
        assertEquals(5, casoDeUso.calificar("ana@x.com", 10L, 5, null).puntuacion());
    }

    @Test
    void sinNingunaFechaDeFinalizacionNoSePuedeAplicarElPlazoYSePermite() {
        when(historial.listarPorCita(10L)).thenReturn(List.of());

        assertEquals(5, casoDeUso.calificar("ana@x.com", 10L, 5, null).puntuacion());
    }

    @Test
    void puntuacionNulaCeroOSeisEsInvalidaYNoLlegaAlRepositorio() {
        assertThrows(CalificacionInvalidaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, null, null));
        assertThrows(CalificacionInvalidaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 0, null));
        assertThrows(CalificacionInvalidaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 6, null));
        verify(calificacionRepo, never()).guardar(any());
    }

    @Test
    void comentarioDe501CaracteresEsInvalido() {
        String largo = "x".repeat(501);

        assertThrows(CalificacionInvalidaException.class, () -> casoDeUso.calificar("ana@x.com", 10L, 5, largo));
        verify(calificacionRepo, never()).guardar(any());
    }
}
