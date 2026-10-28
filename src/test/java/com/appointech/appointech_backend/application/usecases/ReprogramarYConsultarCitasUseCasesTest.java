package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCambiosCita;
import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.DetalleCitaCompleto;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.HistorialEstadoCitaPort;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** SCRUM-21 (reprogramar) y SCRUM-18 (consultar estado e historial). */
@ExtendWith(MockitoExtension.class)
class ReprogramarYConsultarCitasUseCasesTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-27T14:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 9, 0);
    private static final LocalDateTime ACTUAL = AHORA.plusDays(1).withHour(10);
    private static final LocalDateTime NUEVO = AHORA.plusDays(2).withHour(14);

    @Mock
    private ResolverClienteAutenticado resolverCliente;
    @Mock
    private CitaRepositoryPort citas;
    @Mock
    private TecnicoRepositoryPort tecnicos;
    @Mock
    private CalcularFranjasDisponiblesUseCase calcularFranjas;
    @Mock
    private HistorialEstadoCitaPort historial;
    @Mock
    private com.appointech.appointech_backend.domain.ports.out.CitaFotoRepositoryPort fotos;
    @Mock
    private NotificadorCitaPort notificador;
    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private RegistroLogPort logs;
    @Mock
    private ObtenerDetalleCitaUseCase detalleUseCase;

    private GestorCambiosCita gestor;

    @BeforeEach
    void preparar() {
        gestor = new GestorCambiosCita(citas, historial, notificador, clientes, tecnicos, logs, RELOJ);
    }

    private Usuario usuario(String correo) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        return u;
    }

    private Cliente cliente(Long id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setUsuario(usuario("ana@mail.com"));
        return c;
    }

    private Tecnico tecnico(Long id) {
        Tecnico t = new Tecnico();
        t.setId(id);
        t.setUsuario(usuario("tec@mail.com"));
        return t;
    }

    private Cita cita(Long id, EstadoCita estado, Long clienteId, Long tecnicoId) {
        Cita c = new Cita();
        c.setId(id);
        c.setEstado(estado);
        c.setClienteId(clienteId);
        c.setTecnicoId(tecnicoId);
        c.setEspecialidadId(2L);
        c.setDescripcionProblema("Fuga");
        c.setDireccionServicio("Calle 1");
        c.setLatitud(4.6);
        c.setLongitud(-74.1);
        c.setFechaHoraInicio(ACTUAL);
        c.setFechaHoraFin(ACTUAL.plusHours(2));
        return c;
    }

    private FranjaDisponible franja(Long tecnicoId, LocalDateTime inicio) {
        return new FranjaDisponible(tecnicoId, inicio, inicio.plusHours(2), 1.5, "Luis", "Perez");
    }

    private ReprogramarCitaUseCaseImpl reprogramar() {
        return new ReprogramarCitaUseCaseImpl(resolverCliente, citas, tecnicos, calcularFranjas, gestor);
    }

    private void clienteYCita(Cita cita) {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L));
        when(citas.buscarPorId(cita.getId())).thenReturn(Optional.of(cita));
    }

    // ================= Reprogramar =================

    @Test
    void reprogramarMueveLaCitaALaNuevaFranjaDelMismoTecnicoYRegistraElHistorial() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        clienteYCita(c);
        when(tecnicos.buscarPorIdBloqueando(5L)).thenReturn(Optional.of(tecnico(5L)));
        when(calcularFranjas.calcular(2L, NUEVO.toLocalDate(), 4.6, -74.1))
                .thenReturn(List.of(franja(8L, NUEVO), franja(5L, NUEVO.minusHours(2)), franja(5L, NUEVO)));
        when(citas.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L)));
        when(tecnicos.buscarPorId(5L)).thenReturn(Optional.of(tecnico(5L)));

        Cita r = reprogramar().reprogramar("ana@mail.com", 10L, NUEVO);

        assertEquals(NUEVO, r.getFechaHoraInicio());
        assertEquals(NUEVO.plusHours(2), r.getFechaHoraFin());
        assertEquals(5L, r.getTecnicoId(), "se queda con el mismo técnico");
        assertEquals(EstadoCita.CONFIRMADA, r.getEstado());
        ArgumentCaptor<HistorialEstadoCita> fila = ArgumentCaptor.forClass(HistorialEstadoCita.class);
        verify(historial).registrar(fila.capture());
        assertEquals(EstadoCita.CONFIRMADA, fila.getValue().estadoAnterior());
        assertEquals(EstadoCita.CONFIRMADA, fila.getValue().estadoNuevo());
        assertEquals("ana@mail.com", fila.getValue().actor());
        assertTrue(fila.getValue().motivo().contains(ACTUAL.toString()) && fila.getValue().motivo().contains(NUEVO.toString()));
        verify(notificador).notificarCambioDeCita(eq(r), eq("ana@mail.com"), contains("reprogramada"));
        verify(notificador).notificarCambioDeCita(eq(r), eq("tec@mail.com"), contains("reprogramó"));
    }

    @Test
    void sinFranjaDelMismoTecnicoEnEseHorarioResponde409YNoCambiaNada() {
        Cita c = cita(10L, EstadoCita.PENDIENTE, 1L, 5L);
        clienteYCita(c);
        when(tecnicos.buscarPorIdBloqueando(5L)).thenReturn(Optional.of(tecnico(5L)));
        // solo otro técnico tiene libre esa hora
        when(calcularFranjas.calcular(2L, NUEVO.toLocalDate(), 4.6, -74.1)).thenReturn(List.of(franja(8L, NUEVO)));

        assertThrows(FranjaNoDisponibleException.class, () -> reprogramar().reprogramar("ana@mail.com", 10L, NUEVO));

        assertEquals(ACTUAL, c.getFechaHoraInicio());
        verify(citas, never()).guardar(any());
        verifyNoInteractions(historial, notificador);
    }

    @Test
    void sinNingunaFranjaResponde409() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        clienteYCita(c);
        when(tecnicos.buscarPorIdBloqueando(5L)).thenReturn(Optional.of(tecnico(5L)));
        when(calcularFranjas.calcular(any(), any(LocalDate.class), any(), any())).thenReturn(List.of());

        assertThrows(FranjaNoDisponibleException.class, () -> reprogramar().reprogramar("ana@mail.com", 10L, NUEVO));
    }

    @Test
    void unaCitaDeOtroClienteOInexistenteResponde404() {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.CONFIRMADA, 2L, 5L)));
        when(citas.buscarPorId(11L)).thenReturn(Optional.empty());
        ReprogramarCitaUseCaseImpl caso = reprogramar();

        assertThrows(SolicitudNoEncontradaException.class, () -> caso.reprogramar("ana@mail.com", 10L, NUEVO));
        assertThrows(SolicitudNoEncontradaException.class, () -> caso.reprogramar("ana@mail.com", 11L, NUEVO));

        verifyNoInteractions(calcularFranjas);
    }

    @Test
    void soloSeReprogramaPendienteOConfirmada() {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L));
        EstadoCita[] noPermitidos = {EstadoCita.EN_CAMINO, EstadoCita.EN_PROCESO, EstadoCita.FINALIZADA, EstadoCita.CANCELADA};
        long id = 30L;
        for (EstadoCita estado : noPermitidos) {
            when(citas.buscarPorId(id)).thenReturn(Optional.of(cita(id, estado, 1L, 5L)));
            long actual = id;
            assertThrows(EstadoCitaInvalidoException.class,
                    () -> reprogramar().reprogramar("ana@mail.com", actual, NUEVO), estado.name());
            id++;
        }

        verifyNoInteractions(calcularFranjas);
        verify(citas, never()).guardar(any());
    }

    @Test
    void unaSolicitudSinTecnicoNoSePuedeReprogramar() {
        clienteYCita(cita(10L, EstadoCita.PENDIENTE, 1L, null));

        FranjaNoDisponibleException ex = assertThrows(FranjaNoDisponibleException.class,
                () -> reprogramar().reprogramar("ana@mail.com", 10L, NUEVO));

        assertTrue(ex.getMessage().contains("técnico"));
        verifyNoInteractions(calcularFranjas);
    }

    @Test
    void unaFranjaEnElPasadoOElMismoInstanteSeRechaza() {
        clienteYCita(cita(10L, EstadoCita.CONFIRMADA, 1L, 5L));

        assertThrows(FranjaInvalidaException.class, () -> reprogramar().reprogramar("ana@mail.com", 10L, AHORA));
        assertThrows(FranjaInvalidaException.class, () -> reprogramar().reprogramar("ana@mail.com", 10L, AHORA.minusDays(1)));

        verifyNoInteractions(calcularFranjas);
    }

    @Test
    void unaCitaSinCoordenadasNoSePuedeValidarContraLaDisponibilidad() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        c.setLatitud(null);
        clienteYCita(c);

        assertThrows(FranjaNoDisponibleException.class, () -> reprogramar().reprogramar("ana@mail.com", 10L, NUEVO));

        verifyNoInteractions(calcularFranjas);
    }

    @Test
    void unTecnicoInexistenteImpideReprogramar() {
        clienteYCita(cita(10L, EstadoCita.CONFIRMADA, 1L, 5L));
        when(tecnicos.buscarPorIdBloqueando(5L)).thenReturn(Optional.empty());

        assertThrows(TecnicoNoEncontradoException.class, () -> reprogramar().reprogramar("ana@mail.com", 10L, NUEVO));

        verifyNoInteractions(calcularFranjas);
    }

    @Test
    void siFallaElNotificadorLaReprogramacionQuedaGuardada() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        clienteYCita(c);
        when(tecnicos.buscarPorIdBloqueando(5L)).thenReturn(Optional.of(tecnico(5L)));
        when(calcularFranjas.calcular(any(), any(LocalDate.class), any(), any())).thenReturn(List.of(franja(5L, NUEVO)));
        when(citas.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L)));
        when(tecnicos.buscarPorId(5L)).thenReturn(Optional.of(tecnico(5L)));
        doThrow(new IllegalStateException("caído")).when(notificador).notificarCambioDeCita(any(), anyString(), anyString());

        Cita r = reprogramar().reprogramar("ana@mail.com", 10L, NUEVO);

        assertEquals(NUEVO, r.getFechaHoraInicio());
        verify(historial).registrar(any(HistorialEstadoCita.class));
    }

    // ================= Consultas del cliente =================

    private ConsultarCitasDelClienteUseCaseImpl consultaCliente() {
        return new ConsultarCitasDelClienteUseCaseImpl(resolverCliente, citas, detalleUseCase, historial, fotos);
    }

    @Test
    void listarDevuelveSoloLasCitasDelClienteConSuDetalle() {
        Cita a = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        Cita b = cita(11L, EstadoCita.PENDIENTE, 1L, null);
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L));
        when(citas.listarPorCliente(1L)).thenReturn(List.of(a, b));
        when(detalleUseCase.detallar(a)).thenReturn(new DetalleCita(a, "Luis", "Perez", "300", "Plomeria"));
        when(detalleUseCase.detallar(b)).thenReturn(new DetalleCita(b, null, null, null, "Plomeria"));

        List<DetalleCita> r = consultaCliente().listar("ana@mail.com");

        assertEquals(2, r.size());
        assertEquals("Luis", r.get(0).tecnicoNombre());
        assertEquals(EstadoCita.PENDIENTE, r.get(1).cita().getEstado());
        assertNull(r.get(1).tecnicoNombre());
    }

    @Test
    void listarSinCitasDevuelveListaVacia() {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L));
        when(citas.listarPorCliente(1L)).thenReturn(List.of());

        assertTrue(consultaCliente().listar("ana@mail.com").isEmpty());
    }

    @Test
    void listarParaUnUsuarioQueNoEsClientePropagaElError() {
        when(resolverCliente.resolver("x@mail.com")).thenThrow(new ClienteNoEncontradoException("x@mail.com"));

        assertThrows(ClienteNoEncontradoException.class, () -> consultaCliente().listar("x@mail.com"));
    }

    @Test
    void elDetalleIncluyeDiagnosticoEHistorialEnOrden() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        c.setDiagnostico("Sifón roto");
        DetalleCita detalle = new DetalleCita(c, "Luis", "Perez", "300", "Plomeria");
        List<HistorialEstadoCita> filas = List.of(
                new HistorialEstadoCita(1L, 10L, EstadoCita.CONFIRMADA, EstadoCita.EN_CAMINO, AHORA, "tec@mail.com", null),
                new HistorialEstadoCita(2L, 10L, EstadoCita.EN_CAMINO, EstadoCita.EN_PROCESO, AHORA.plusMinutes(30), "tec@mail.com", null));
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        when(detalleUseCase.detallar(c)).thenReturn(detalle);
        when(historial.listarPorCita(10L)).thenReturn(filas);

        DetalleCitaCompleto r = consultaCliente().detallar("ana@mail.com", 10L);

        assertSame(detalle, r.detalle());
        assertEquals("Sifón roto", r.detalle().cita().getDiagnostico());
        assertEquals(filas, r.historial());
    }

    @Test
    void unClienteNoVeLaCitaDeOtroNiUnaInexistente() {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.CONFIRMADA, 2L, 5L)));
        when(citas.buscarPorId(11L)).thenReturn(Optional.empty());
        ConsultarCitasDelClienteUseCaseImpl caso = consultaCliente();

        assertThrows(SolicitudNoEncontradaException.class, () -> caso.detallar("ana@mail.com", 10L));
        assertThrows(SolicitudNoEncontradaException.class, () -> caso.detallar("ana@mail.com", 11L));

        verifyNoInteractions(historial, detalleUseCase);
    }

    // ================= Consulta del administrador =================

    @Test
    void elAdministradorVeElDetalleDeCualquierCita() {
        Cita c = cita(10L, EstadoCita.FINALIZADA, 99L, 5L);
        DetalleCita detalle = new DetalleCita(c, "Luis", "Perez", "300", "Plomeria");
        List<HistorialEstadoCita> filas = List.of(
                new HistorialEstadoCita(1L, 10L, EstadoCita.EN_PROCESO, EstadoCita.FINALIZADA, AHORA, "tec@mail.com", null));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        when(detalleUseCase.detallar(c)).thenReturn(detalle);
        when(historial.listarPorCita(10L)).thenReturn(filas);

        DetalleCitaCompleto r = new ConsultarDetalleCitaAdminUseCaseImpl(citas, detalleUseCase, historial, fotos).detallar(10L);

        assertEquals(99L, r.detalle().cita().getClienteId(), "no se filtra por cliente");
        assertEquals(filas, r.historial());
    }

    @Test
    void elAdministradorRecibe404SiLaCitaNoExiste() {
        when(citas.buscarPorId(10L)).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class,
                () -> new ConsultarDetalleCitaAdminUseCaseImpl(citas, detalleUseCase, historial, fotos).detallar(10L));

        verifyNoInteractions(historial);
    }
}
