package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.SolicitudManualNoPermitidaException;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.TelefonoContactoRequeridoException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MotivoSinFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.CalcularFranjasDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgendamientoManualUseCasesTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 6, 9, 0);
    private static final LocalDateTime FIN = INICIO.plusHours(2);

    private UsuarioRepositoryPort usuarioRepo;
    private ClienteRepositoryPort clienteRepo;
    private EspecialidadRepositoryPort especialidadRepo;
    private CitaRepositoryPort citaRepo;
    private TecnicoRepositoryPort tecnicoRepo;
    private BloqueIndisponibilidadRepositoryPort bloquesRepo;
    private CalcularFranjasDisponiblesUseCase franjas;
    private RegistroLogPort log;
    private NotificadorCitaPort notificador;
    private Usuario usuarioCliente;

    @BeforeEach
    void preparar() {
        usuarioRepo = mock(UsuarioRepositoryPort.class);
        clienteRepo = mock(ClienteRepositoryPort.class);
        especialidadRepo = mock(EspecialidadRepositoryPort.class);
        citaRepo = mock(CitaRepositoryPort.class);
        tecnicoRepo = mock(TecnicoRepositoryPort.class);
        bloquesRepo = mock(BloqueIndisponibilidadRepositoryPort.class);
        franjas = mock(CalcularFranjasDisponiblesUseCase.class);
        log = mock(RegistroLogPort.class);
        notificador = mock(NotificadorCitaPort.class);

        usuarioCliente = new Usuario();
        usuarioCliente.setId(8L);
        usuarioCliente.setCorreo("cliente@x.com");
        usuarioCliente.setTelefono("3001112233");
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setUsuario(usuarioCliente);
        when(usuarioRepo.buscarPorCorreo("cliente@x.com")).thenReturn(Optional.of(usuarioCliente));
        when(clienteRepo.buscarPorUsuarioId(8L)).thenReturn(Optional.of(cliente));
        when(clienteRepo.buscarPorId(2L)).thenReturn(Optional.of(cliente));
        when(especialidadRepo.buscarPorId(5L)).thenReturn(Optional.of(especialidad(5L, "Plomería")));
        when(citaRepo.guardar(any(Cita.class))).thenAnswer(i -> {
            Cita c = i.getArgument(0);
            if (c.getId() == null) c.setId(70L);
            return c;
        });
    }

    // ---------- solicitar ----------

    private SolicitarAgendamientoManualUseCaseImpl solicitar() {
        return new SolicitarAgendamientoManualUseCaseImpl(usuarioRepo, clienteRepo, especialidadRepo, citaRepo,
                franjas, log);
    }

    private void motivo(MotivoSinFranjas motivo) {
        when(franjas.calcularConMotivo(anyLong(), any(), any(), any()))
                .thenReturn(new ResultadoFranjas(List.of(), motivo));
    }

    @Test
    void solicitudManualValidaQuedaPendienteSinTecnicoNiHorario() {
        motivo(MotivoSinFranjas.FUERA_DE_COBERTURA);

        Cita c = solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Vereda Lejana", 4.5, -74.1, null);

        assertEquals(EstadoCita.PENDIENTE, c.getEstado());
        assertTrue(c.isAgendamientoManual());
        assertNull(c.getTecnicoId());
        assertNull(c.getFechaHoraInicio());
        assertNull(c.getFechaHoraFin());
        assertEquals(2L, c.getClienteId());
        assertEquals("3001112233", c.getTelefonoContacto());   // usa el teléfono de la cuenta
        assertEquals(4.5, c.getLatitud());
    }

    @Test
    void elTelefonoIndicadoTienePrioridadSobreElDeLaCuenta() {
        motivo(MotivoSinFranjas.FUERA_DE_COBERTURA);

        Cita c = solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Vereda", 4.5, -74.1, " 3109998877 ");

        assertEquals("3109998877", c.getTelefonoContacto());
    }

    @Test
    void sinTelefonoEnLaCuentaNiEnLaSolicitudSeExige() {
        usuarioCliente.setTelefono(null);
        motivo(MotivoSinFranjas.FUERA_DE_COBERTURA);

        assertThrows(TelefonoContactoRequeridoException.class,
                () -> solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Vereda", 4.5, -74.1, "  "));
        verify(citaRepo, never()).guardar(any());
    }

    @Test
    void conSinTelefonoEnCuentaPeroTelefonoEnSolicitudSeAcepta() {
        usuarioCliente.setTelefono(null);
        motivo(MotivoSinFranjas.FUERA_DE_COBERTURA);

        assertEquals("3109998877",
                solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Vereda", 4.5, -74.1, "3109998877")
                        .getTelefonoContacto());
    }

    @Test
    void dentroDeCoberturaNoSePermiteSolicitudManual() {
        motivo(MotivoSinFranjas.SIN_HORARIOS);

        SolicitudManualNoPermitidaException ex = assertThrows(SolicitudManualNoPermitidaException.class,
                () -> solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Calle 1", 4.5, -74.1, null));
        assertTrue(ex.getMessage().contains("dentro de la cobertura"));
        verify(citaRepo, never()).guardar(any());
    }

    @Test
    void conFranjasDisponiblesTampocoSePermite() {
        when(franjas.calcularConMotivo(anyLong(), any(), any(), any()))
                .thenReturn(new ResultadoFranjas(List.of(), null));

        assertThrows(SolicitudManualNoPermitidaException.class,
                () -> solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Calle 1", 4.5, -74.1, null));
    }

    @Test
    void sinTecnicosDeLaEspecialidadNoSePermite() {
        motivo(MotivoSinFranjas.SIN_TECNICOS_ESPECIALIDAD);

        SolicitudManualNoPermitidaException ex = assertThrows(SolicitudManualNoPermitidaException.class,
                () -> solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Calle 1", 4.5, -74.1, null));
        assertTrue(ex.getMessage().contains("No hay técnicos"));
    }

    @Test
    void coordenadasInvalidasSeRechazanAntesDeConsultarCobertura() {
        assertThrows(CoordenadasInvalidasException.class,
                () -> solicitar().solicitar("cliente@x.com", 5L, "Fuga", "Calle 1", 4.5, null, null));
        verify(franjas, never()).calcularConMotivo(anyLong(), any(), any(), any());
    }

    // ---------- listar ----------

    @Test
    void listaLasSolicitudesConDatosDelClienteYLaEspecialidad() {
        Cita c = manualPendiente(70L);
        when(citaRepo.buscarSolicitudesManualesPendientes()).thenReturn(List.of(c));
        ConsultaClientesPort consulta = mock(ConsultaClientesPort.class);
        when(consulta.resumenesPorId(any())).thenReturn(Map.of(2L, new ClienteResumen("Ana", "Ruiz", "3001112233")));
        when(especialidadRepo.buscarPorIds(List.of(5L))).thenReturn(List.of(especialidad(5L, "Plomería")));

        List<SolicitudManualDetalle> lista =
                new ListarSolicitudesManualesUseCaseImpl(citaRepo, consulta, especialidadRepo).listar();

        assertEquals(1, lista.size());
        SolicitudManualDetalle d = lista.get(0);
        assertEquals("Ana", d.clienteNombre());
        assertEquals("Ruiz", d.clienteApellido());
        assertEquals("3109998877", d.telefonoContacto());     // el de contacto de la solicitud
        assertEquals("Vereda", d.direccionServicio());
        assertEquals("Fuga", d.descripcionProblema());
        assertEquals("Plomería", d.especialidadNombre());
    }

    @Test
    void sinSolicitudesNoConsultaClientesNiEspecialidades() {
        when(citaRepo.buscarSolicitudesManualesPendientes()).thenReturn(List.of());
        ConsultaClientesPort consulta = mock(ConsultaClientesPort.class);

        assertTrue(new ListarSolicitudesManualesUseCaseImpl(citaRepo, consulta, especialidadRepo).listar().isEmpty());
        verify(consulta, never()).resumenesPorId(any());
    }

    // ---------- asignar ----------

    private AsignarSolicitudManualUseCaseImpl asignar() {
        return new AsignarSolicitudManualUseCaseImpl(citaRepo, tecnicoRepo, bloquesRepo, clienteRepo, log, notificador);
    }

    @Test
    void asignacionValidaConfirmaLaCitaYNotificaAlCliente() {
        Cita c = manualPendiente(70L);
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(c));
        when(tecnicoRepo.buscarPorIdBloqueando(3L)).thenReturn(Optional.of(tecnico(true, 5L)));

        Cita r = asignar().asignar("admin@x.com", 70L, 3L, INICIO, FIN);

        assertEquals(EstadoCita.CONFIRMADA, r.getEstado());
        assertEquals(3L, r.getTecnicoId());
        assertEquals(INICIO, r.getFechaHoraInicio());
        assertEquals(FIN, r.getFechaHoraFin());
        verify(notificador).notificarCitaConfirmada(r, "cliente@x.com");
        verify(log).registrar(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void choqueConOtraCitaDelTecnicoLanza409() {
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(manualPendiente(70L)));
        when(tecnicoRepo.buscarPorIdBloqueando(3L)).thenReturn(Optional.of(tecnico(true, 5L)));
        Cita otra = new Cita();
        otra.setId(99L);
        otra.setFechaHoraInicio(INICIO.plusHours(1));
        otra.setFechaHoraFin(FIN.plusHours(1));
        when(citaRepo.buscarActivasDelTecnicoEntre(3L, INICIO, FIN)).thenReturn(List.of(otra));

        assertThrows(FranjaNoDisponibleException.class, () -> asignar().asignar("a", 70L, 3L, INICIO, FIN));
        verify(citaRepo, never()).guardar(any());
    }

    @Test
    void choqueConUnBloqueDeIndisponibilidadLanza409() {
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(manualPendiente(70L)));
        when(tecnicoRepo.buscarPorIdBloqueando(3L)).thenReturn(Optional.of(tecnico(true, 5L)));
        BloqueIndisponibilidad bloque = mock(BloqueIndisponibilidad.class);
        when(bloque.getInicio()).thenReturn(INICIO.minusHours(1));
        when(bloque.getFin()).thenReturn(INICIO.plusMinutes(30));
        when(bloquesRepo.buscarDelTecnicoEntre(3L, INICIO, FIN)).thenReturn(List.of(bloque));

        assertThrows(FranjaNoDisponibleException.class, () -> asignar().asignar("a", 70L, 3L, INICIO, FIN));
    }

    @Test
    void tecnicoSinLaEspecialidadLanza409() {
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(manualPendiente(70L)));
        when(tecnicoRepo.buscarPorIdBloqueando(3L)).thenReturn(Optional.of(tecnico(true, 6L)));

        FranjaNoDisponibleException ex = assertThrows(FranjaNoDisponibleException.class,
                () -> asignar().asignar("a", 70L, 3L, INICIO, FIN));
        assertTrue(ex.getMessage().contains("especialidad"));
    }

    @Test
    void tecnicoInactivoLanza409() {
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(manualPendiente(70L)));
        when(tecnicoRepo.buscarPorIdBloqueando(3L)).thenReturn(Optional.of(tecnico(false, 5L)));

        assertThrows(FranjaNoDisponibleException.class, () -> asignar().asignar("a", 70L, 3L, INICIO, FIN));
    }

    @Test
    void tecnicoInexistenteLanza404() {
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(manualPendiente(70L)));
        when(tecnicoRepo.buscarPorIdBloqueando(3L)).thenReturn(Optional.empty());

        assertThrows(TecnicoNoEncontradoException.class, () -> asignar().asignar("a", 70L, 3L, INICIO, FIN));
    }

    @Test
    void solicitudYaAsignadaLanza409() {
        Cita c = manualPendiente(70L);
        c.setEstado(EstadoCita.CONFIRMADA);
        c.setTecnicoId(3L);
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(c));

        assertThrows(FranjaNoDisponibleException.class, () -> asignar().asignar("a", 70L, 4L, INICIO, FIN));
        verify(citaRepo, never()).guardar(any());
    }

    @Test
    void solicitudInexistenteOQueNoEsManualLanza404() {
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.empty());
        Cita normal = manualPendiente(71L);
        normal.setAgendamientoManual(false);
        when(citaRepo.buscarPorId(71L)).thenReturn(Optional.of(normal));

        assertThrows(SolicitudNoEncontradaException.class, () -> asignar().asignar("a", 70L, 3L, INICIO, FIN));
        assertThrows(SolicitudNoEncontradaException.class, () -> asignar().asignar("a", 71L, 3L, INICIO, FIN));
    }

    @Test
    void horarioConFinNoPosteriorAlInicioLanza400() {
        assertThrows(FranjaInvalidaException.class, () -> asignar().asignar("a", 70L, 3L, FIN, INICIO));
    }

    @Test
    void laCoberturaNoSeExigeAlAsignar() {
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(manualPendiente(70L)));
        when(tecnicoRepo.buscarPorIdBloqueando(3L)).thenReturn(Optional.of(tecnico(true, 5L)));

        asignar().asignar("a", 70L, 3L, INICIO, FIN);

        verify(franjas, never()).calcularConMotivo(anyLong(), any(), any(), any());
    }

    // ---------- confirmar normal no puede tomar una solicitud manual ----------

    @Test
    void elClienteNoPuedeConfirmarPorSuCuentaUnaSolicitudManual() {
        Cita c = manualPendiente(70L);
        when(citaRepo.buscarPorId(70L)).thenReturn(Optional.of(c));
        var confirmar = new ConfirmarCitaUseCaseImpl(usuarioRepo, clienteRepo, citaRepo, tecnicoRepo, bloquesRepo,
                mock(com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort.class),
                log, notificador, java.time.Clock.system(java.time.ZoneId.of("America/Bogota")));

        FranjaNoDisponibleException ex = assertThrows(FranjaNoDisponibleException.class,
                () -> confirmar.confirmar("cliente@x.com", 70L, 3L, INICIO, FIN));
        assertTrue(ex.getMessage().contains("agendamiento manual"));
    }

    // ---------- helpers ----------

    private static Cita manualPendiente(Long id) {
        Cita c = new Cita();
        c.setId(id);
        c.setClienteId(2L);
        c.setEspecialidadId(5L);
        c.setDescripcionProblema("Fuga");
        c.setDireccionServicio("Vereda");
        c.setLatitud(4.5);
        c.setLongitud(-74.1);
        c.setEstado(EstadoCita.PENDIENTE);
        c.setAgendamientoManual(true);
        c.setTelefonoContacto("3109998877");
        c.setCreadaEn(LocalDateTime.of(2030, 5, 1, 8, 0));
        return c;
    }

    private static Especialidad especialidad(Long id, String nombre) {
        Especialidad e = new Especialidad();
        e.setId(id);
        e.setNombre(nombre);
        e.setActivo(true);
        return e;
    }

    private static Tecnico tecnico(boolean activo, Long especialidadId) {
        Usuario u = new Usuario();
        u.setId(30L);
        u.setActivo(activo);
        Tecnico t = new Tecnico();
        t.setId(3L);
        t.setUsuario(u);
        t.setEspecialidades(List.of(especialidad(especialidadId, "Esp")));
        return t;
    }
}
