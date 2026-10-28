package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCambiosCita;
import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.DatosCitaInvalidosException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;
import com.appointech.appointech_backend.domain.exception.PerfilTecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
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

/** SCRUM-19, SCRUM-20, SCRUM-21 (cancelar) y SCRUM-22: cambios de estado, cancelaciones y diagnóstico. */
@ExtendWith(MockitoExtension.class)
class CambiosDeEstadoCitaUseCasesTest {

    // 14:00 UTC = 09:00 en Bogotá
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-27T14:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 9, 0);

    @Mock
    private ResolverTecnicoAutenticado resolverTecnico;
    @Mock
    private ResolverClienteAutenticado resolverCliente;
    @Mock
    private CitaRepositoryPort citas;
    @Mock
    private HistorialEstadoCitaPort historial;
    @Mock
    private NotificadorCitaPort notificador;
    @Mock
    private ClienteRepositoryPort clientes;
    @Mock
    private TecnicoRepositoryPort tecnicos;
    @Mock
    private RegistroLogPort logs;

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

    private Tecnico tecnico(Long id, String correo) {
        Tecnico t = new Tecnico();
        t.setId(id);
        t.setUsuario(usuario(correo));
        return t;
    }

    private Cliente cliente(Long id, String correo) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setUsuario(usuario(correo));
        return c;
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
        c.setFechaHoraInicio(AHORA.plusDays(1));
        c.setFechaHoraFin(AHORA.plusDays(1).plusHours(2));
        return c;
    }

    private void guardarDevuelveLoRecibido() {
        when(citas.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private HistorialEstadoCita historialRegistrado() {
        ArgumentCaptor<HistorialEstadoCita> captor = ArgumentCaptor.forClass(HistorialEstadoCita.class);
        verify(historial).registrar(captor.capture());
        return captor.getValue();
    }

    // ================= ActualizarEstadoCita (SCRUM-19) =================

    private ActualizarEstadoCitaUseCaseImpl actualizarEstado() {
        return new ActualizarEstadoCitaUseCaseImpl(resolverTecnico, citas, gestor);
    }

    @Test
    void elTecnicoAvanzaSuCitaGuardaHistorialYNotificaAlCliente() {
        Cita original = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(original));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));

        Cita r = actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO);

        assertEquals(EstadoCita.EN_CAMINO, r.getEstado());
        assertEquals(AHORA, r.getActualizadaEn());
        assertEquals("Fuga", r.getDescripcionProblema(), "conserva el resto de los datos");
        assertEquals("Calle 1", r.getDireccionServicio());
        assertEquals(5L, r.getTecnicoId());
        HistorialEstadoCita fila = historialRegistrado();
        assertEquals(10L, fila.citaId());
        assertEquals(EstadoCita.CONFIRMADA, fila.estadoAnterior());
        assertEquals(EstadoCita.EN_CAMINO, fila.estadoNuevo());
        assertEquals("tec@mail.com", fila.actor());
        assertEquals(AHORA, fila.fecha());
        assertNull(fila.motivo());
        verify(notificador).notificarCambioDeCita(eq(r), eq("ana@mail.com"), contains("CONFIRMADA → EN_CAMINO"));
    }

    @Test
    void recorreElCicloCompletoHastaFinalizada() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));
        ActualizarEstadoCitaUseCaseImpl caso = actualizarEstado();

        caso.actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO);
        caso.actualizar("tec@mail.com", 10L, EstadoCita.EN_PROCESO);
        Cita fin = caso.actualizar("tec@mail.com", 10L, EstadoCita.FINALIZADA);

        assertEquals(EstadoCita.FINALIZADA, fin.getEstado());
        assertNull(fin.getDiagnostico(), "no se exige diagnóstico para finalizar");
    }

    @Test
    void unaCitaInexistenteResponde404() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class,
                () -> actualizarEstado().actualizar("tec@mail.com", 99L, EstadoCita.EN_CAMINO));

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historial, notificador);
    }

    @Test
    void unaCitaAsignadaAOtroTecnicoSeTrataComoInexistente() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.CONFIRMADA, 1L, 77L)));

        assertThrows(SolicitudNoEncontradaException.class,
                () -> actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO));

        verify(citas, never()).guardar(any());
    }

    @Test
    void unaCitaSinTecnicoNoPerteneceANingunTecnico() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.PENDIENTE, 1L, null)));

        assertThrows(SolicitudNoEncontradaException.class,
                () -> actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO));
    }

    @Test
    void unUsuarioSinPerfilDeTecnicoNoPuedeActualizar() {
        when(resolverTecnico.resolver("x@mail.com")).thenThrow(new PerfilTecnicoNoEncontradoException("x@mail.com"));

        assertThrows(PerfilTecnicoNoEncontradoException.class,
                () -> actualizarEstado().actualizar("x@mail.com", 10L, EstadoCita.EN_CAMINO));

        verifyNoInteractions(citas);
    }

    @Test
    void unaTransicionInvalidaNoGuardaNiNotificaNiRegistraHistorial() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));

        assertThrows(EstadoCitaInvalidoException.class,
                () -> actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.FINALIZADA));

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historial, notificador);
    }

    @Test
    void desdeFinalizadaYCanceladaNoSePuedeCambiarNada() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.FINALIZADA, 1L, 5L)));
        when(citas.buscarPorId(11L)).thenReturn(Optional.of(cita(11L, EstadoCita.CANCELADA, 1L, 5L)));
        ActualizarEstadoCitaUseCaseImpl caso = actualizarEstado();

        assertThrows(EstadoCitaInvalidoException.class, () -> caso.actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO));
        assertThrows(EstadoCitaInvalidoException.class, () -> caso.actualizar("tec@mail.com", 11L, EstadoCita.CONFIRMADA));

        verify(citas, never()).guardar(any());
    }

    @Test
    void siFallaElNotificadorElCambioQuedaGuardado() {
        Cita c = cita(10L, EstadoCita.EN_CAMINO, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));
        doThrow(new IllegalStateException("servicio caído")).when(notificador)
                .notificarCambioDeCita(any(), anyString(), anyString());

        Cita r = actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.EN_PROCESO);

        assertEquals(EstadoCita.EN_PROCESO, r.getEstado());
        verify(citas).guardar(c);
        verify(historial).registrar(any(HistorialEstadoCita.class));
    }

    @Test
    void siNoSeEncuentraAlClienteParaAvisarElCambioIgualSeCompleta() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.empty());

        Cita r = actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO);

        assertEquals(EstadoCita.EN_CAMINO, r.getEstado());
        verifyNoInteractions(notificador);
    }

    @Test
    void siFallaLaBusquedaDelDestinatarioElCambioIgualSeCompleta() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenThrow(new IllegalStateException("db"));

        assertEquals(EstadoCita.EN_CAMINO,
                actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO).getEstado());
    }

    @Test
    void alFinalizarSeEnviaUnaSolaVezElCorreoDeFinalizacionDespuesDeGuardarElHistorial() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        c.setDiagnostico("Sifón roto");
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));

        Cita r = actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.FINALIZADA);

        assertEquals(EstadoCita.FINALIZADA, r.getEstado());
        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(historial, notificador);
        orden.verify(historial).registrar(any(HistorialEstadoCita.class));
        orden.verify(notificador).notificarCitaFinalizada(r, "ana@mail.com");
        verify(notificador, org.mockito.Mockito.times(1)).notificarCitaFinalizada(any(), anyString());
    }

    @Test
    void losDemasCambiosDeEstadoNoEnvianElCorreoDeFinalizacion() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));
        ActualizarEstadoCitaUseCaseImpl caso = actualizarEstado();

        caso.actualizar("tec@mail.com", 10L, EstadoCita.EN_CAMINO);
        caso.actualizar("tec@mail.com", 10L, EstadoCita.EN_PROCESO);

        verify(notificador, never()).notificarCitaFinalizada(any(), anyString());
    }

    @Test
    void siFallaElCorreoDeFinalizacionLaCitaIgualQuedaFinalizada() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));
        doThrow(new IllegalStateException("caído")).when(notificador).notificarCitaFinalizada(any(), anyString());

        Cita r = actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.FINALIZADA);

        assertEquals(EstadoCita.FINALIZADA, r.getEstado());
        verify(citas).guardar(c);
        verify(historial).registrar(any(HistorialEstadoCita.class));
    }

    @Test
    void siNoSeEncuentraAlClienteNoSeEnviaElCorreoDeFinalizacionPeroElEstadoCambia() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.empty());

        assertEquals(EstadoCita.FINALIZADA,
                actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.FINALIZADA).getEstado());

        verifyNoInteractions(notificador);
    }

    @Test
    void siFallaLaBusquedaDelClienteAlFinalizarElEstadoIgualCambia() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenThrow(new IllegalStateException("db"));

        assertEquals(EstadoCita.FINALIZADA,
                actualizarEstado().actualizar("tec@mail.com", 10L, EstadoCita.FINALIZADA).getEstado());
    }

    // ================= CancelarCitaCliente (SCRUM-21) =================

    private CancelarCitaClienteUseCaseImpl cancelarCliente() {
        return new CancelarCitaClienteUseCaseImpl(resolverCliente, citas, gestor);
    }

    @Test
    void elClienteCancelaConMotivoYSeAvisaAAmbosParticipantes() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L, "ana@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));
        when(tecnicos.buscarPorId(5L)).thenReturn(Optional.of(tecnico(5L, "tec@mail.com")));

        Cita r = cancelarCliente().cancelar("ana@mail.com", 10L, "  Ya no la necesito ");

        assertEquals(EstadoCita.CANCELADA, r.getEstado());
        HistorialEstadoCita fila = historialRegistrado();
        assertEquals(EstadoCita.CONFIRMADA, fila.estadoAnterior());
        assertEquals(EstadoCita.CANCELADA, fila.estadoNuevo());
        assertEquals("Ya no la necesito", fila.motivo());
        assertEquals("ana@mail.com", fila.actor());
        verify(notificador).notificarCambioDeCita(eq(r), eq("ana@mail.com"), contains("cancelada. Motivo: Ya no la necesito"));
        verify(notificador).notificarCambioDeCita(eq(r), eq("tec@mail.com"), contains("canceló la cita #10"));
    }

    @Test
    void elMotivoDelClienteEsOpcional() {
        Cita c = cita(10L, EstadoCita.PENDIENTE, 1L, null);
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L, "ana@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));

        Cita r = cancelarCliente().cancelar("ana@mail.com", 10L, "   ");

        assertEquals(EstadoCita.CANCELADA, r.getEstado());
        assertNull(historialRegistrado().motivo());
        // sin técnico asignado solo se avisa al cliente, y el texto no menciona un motivo vacío
        verify(notificador).notificarCambioDeCita(eq(r), eq("ana@mail.com"), eq("Tu cita #10 fue cancelada."));
        verify(tecnicos, never()).buscarPorId(any());
    }

    @Test
    void unaCitaDeOtroClienteSeTrataComoInexistente() {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L, "ana@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.CONFIRMADA, 2L, 5L)));

        assertThrows(SolicitudNoEncontradaException.class, () -> cancelarCliente().cancelar("ana@mail.com", 10L, null));

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historial, notificador);
    }

    @Test
    void cancelarUnaCitaInexistenteResponde404() {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L, "ana@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class, () -> cancelarCliente().cancelar("ana@mail.com", 10L, null));
    }

    @Test
    void elClienteNoPuedeCancelarUnaCitaEnCaminoEnProcesoFinalizadaOYaCancelada() {
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L, "ana@mail.com"));
        EstadoCita[] noCancelables = {EstadoCita.EN_CAMINO, EstadoCita.EN_PROCESO, EstadoCita.FINALIZADA, EstadoCita.CANCELADA};
        long id = 20L;
        for (EstadoCita estado : noCancelables) {
            when(citas.buscarPorId(id)).thenReturn(Optional.of(cita(id, estado, 1L, 5L)));
            long idActual = id;
            assertThrows(EstadoCitaInvalidoException.class,
                    () -> cancelarCliente().cancelar("ana@mail.com", idActual, "x"), estado.name());
            id++;
        }

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historial, notificador);
    }

    @Test
    void siFallaElNotificadorLaCancelacionDelClienteQuedaGuardada() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverCliente.resolver("ana@mail.com")).thenReturn(cliente(1L, "ana@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));
        when(tecnicos.buscarPorId(5L)).thenReturn(Optional.of(tecnico(5L, "tec@mail.com")));
        doThrow(new IllegalStateException("caído")).when(notificador).notificarCambioDeCita(any(), anyString(), anyString());

        Cita r = cancelarCliente().cancelar("ana@mail.com", 10L, "x");

        assertEquals(EstadoCita.CANCELADA, r.getEstado());
        verify(historial).registrar(any(HistorialEstadoCita.class));
    }

    // ================= CancelarCitaTecnico (SCRUM-21) =================

    private CancelarCitaTecnicoUseCaseImpl cancelarTecnico() {
        return new CancelarCitaTecnicoUseCaseImpl(resolverTecnico, citas, gestor);
    }

    @Test
    void elTecnicoCancelaConMotivoYSeAvisaAlCliente() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));

        Cita r = cancelarTecnico().cancelar("tec@mail.com", 10L, " Falta de repuesto ");

        assertEquals(EstadoCita.CANCELADA, r.getEstado());
        HistorialEstadoCita fila = historialRegistrado();
        assertEquals(EstadoCita.EN_PROCESO, fila.estadoAnterior());
        assertEquals("Falta de repuesto", fila.motivo());
        assertEquals("tec@mail.com", fila.actor());
        verify(notificador).notificarCambioDeCita(eq(r), eq("ana@mail.com"), contains("Motivo: Falta de repuesto"));
    }

    @Test
    void elMotivoDelTecnicoEsObligatorio() {
        CancelarCitaTecnicoUseCaseImpl caso = cancelarTecnico();

        assertThrows(DatosCitaInvalidosException.class, () -> caso.cancelar("tec@mail.com", 10L, null));
        assertThrows(DatosCitaInvalidosException.class, () -> caso.cancelar("tec@mail.com", 10L, "   "));

        verifyNoInteractions(citas, historial, notificador);
    }

    @Test
    void elTecnicoNoPuedeCancelarUnaCitaAjenaNiInexistente() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.CONFIRMADA, 1L, 77L)));
        when(citas.buscarPorId(11L)).thenReturn(Optional.empty());
        CancelarCitaTecnicoUseCaseImpl caso = cancelarTecnico();

        assertThrows(SolicitudNoEncontradaException.class, () -> caso.cancelar("tec@mail.com", 10L, "x"));
        assertThrows(SolicitudNoEncontradaException.class, () -> caso.cancelar("tec@mail.com", 11L, "x"));

        verify(citas, never()).guardar(any());
    }

    @Test
    void elTecnicoNoPuedeCancelarUnaCitaFinalizada() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.FINALIZADA, 1L, 5L)));

        assertThrows(EstadoCitaInvalidoException.class, () -> cancelarTecnico().cancelar("tec@mail.com", 10L, "x"));

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historial, notificador);
    }

    @Test
    void siFallaElNotificadorLaCancelacionDelTecnicoQuedaGuardada() {
        Cita c = cita(10L, EstadoCita.CONFIRMADA, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "ana@mail.com")));
        doThrow(new IllegalStateException("caído")).when(notificador).notificarCambioDeCita(any(), anyString(), anyString());

        assertEquals(EstadoCita.CANCELADA, cancelarTecnico().cancelar("tec@mail.com", 10L, "x").getEstado());
        verify(historial).registrar(any(HistorialEstadoCita.class));
    }

    // ================= RegistrarDiagnostico (SCRUM-22) =================

    private RegistrarDiagnosticoUseCaseImpl diagnostico() {
        return new RegistrarDiagnosticoUseCaseImpl(resolverTecnico, citas, gestor);
    }

    @Test
    void elTecnicoRegistraElDiagnosticoDeUnaCitaEnProcesoSinTocarElHistorial() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();

        Cita r = diagnostico().registrar("tec@mail.com", 10L, "Sifón roto", "Cambiar sifón");

        assertEquals("Sifón roto", r.getDiagnostico());
        assertEquals("Cambiar sifón", r.getRecomendaciones());
        assertEquals(AHORA, r.getFechaDiagnostico());
        assertEquals(AHORA, r.getActualizadaEn());
        assertEquals(EstadoCita.EN_PROCESO, r.getEstado());
        verifyNoInteractions(historial, notificador);
    }

    @Test
    void registrarDeNuevoActualizaElDiagnosticoExistente() {
        Cita c = cita(10L, EstadoCita.EN_PROCESO, 1L, 5L);
        c.setDiagnostico("Primero");
        c.setRecomendaciones("Vieja");
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(c));
        guardarDevuelveLoRecibido();

        Cita r = diagnostico().registrar("tec@mail.com", 10L, "Corregido", null);

        assertEquals("Corregido", r.getDiagnostico());
        assertNull(r.getRecomendaciones());
    }

    @Test
    void sinEstarEnProcesoElDiagnosticoResponde409() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.EN_CAMINO, 1L, 5L)));

        assertThrows(EstadoCitaInvalidoException.class,
                () -> diagnostico().registrar("tec@mail.com", 10L, "Algo", null));

        verify(citas, never()).guardar(any());
    }

    @Test
    void elDiagnosticoDeUnaCitaAjenaOInexistenteResponde404() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.EN_PROCESO, 1L, 77L)));
        when(citas.buscarPorId(11L)).thenReturn(Optional.empty());
        RegistrarDiagnosticoUseCaseImpl caso = diagnostico();

        assertThrows(SolicitudNoEncontradaException.class, () -> caso.registrar("tec@mail.com", 10L, "x", null));
        assertThrows(SolicitudNoEncontradaException.class, () -> caso.registrar("tec@mail.com", 11L, "x", null));
    }

    @Test
    void unDiagnosticoVacioSeRechazaYNoSeGuarda() {
        when(resolverTecnico.resolver("tec@mail.com")).thenReturn(tecnico(5L, "tec@mail.com"));
        when(citas.buscarPorId(10L)).thenReturn(Optional.of(cita(10L, EstadoCita.EN_PROCESO, 1L, 5L)));

        assertThrows(DatosCitaInvalidosException.class, () -> diagnostico().registrar("tec@mail.com", 10L, " ", "r"));

        verify(citas, never()).guardar(any());
    }

    // ================= GestorCambiosCita =================

    @Test
    void avisarAlTecnicoDeUnaCitaSinTecnicoNoHaceNada() {
        gestor.notificarAlTecnico(cita(10L, EstadoCita.PENDIENTE, 1L, null), "hola");

        verifyNoInteractions(tecnicos, notificador);
    }

    @Test
    void elGestorUsaLaZonaHorariaDelRelojInyectado() {
        assertEquals(AHORA, gestor.ahora());
    }

    @Test
    void guardarConHistorialRegistraUnaAuditoriaConElActor() {
        Cita c = cita(10L, EstadoCita.EN_CAMINO, 1L, 5L);
        guardarDevuelveLoRecibido();

        gestor.guardarConHistorial(c, EstadoCita.CONFIRMADA, "tec@mail.com", null);

        verify(logs).registrar(any(), any(), eq("GestorCambiosCita"), eq("tec@mail.com"),
                contains("CONFIRMADA -> EN_CAMINO"));
    }
}
