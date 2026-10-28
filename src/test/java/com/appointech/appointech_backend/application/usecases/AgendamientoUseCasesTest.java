package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.exception.CoordenadasInvalidasException;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FranjaDisponible;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.exception.FranjaInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaNoDisponibleException;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import com.appointech.appointech_backend.infrastructure.adapters.HaversineDistanciaAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendamientoUseCasesTest {

    private static final double LATITUD_CLIENTE = 4.3365;
    private static final double LONGITUD_CLIENTE = -74.3638;

    private final List<Cita> citasGuardadas = new ArrayList<>();
    private final List<Tecnico> tecnicosDisponibles = new ArrayList<>();
    private final List<String> logsRegistrados = new ArrayList<>();
    private final List<Long> citasNotificadas = new ArrayList<>();
    private String nivelConsultado;
    private int limiteConsultado;
    private final LocalDate fechaPrueba = LocalDate.now().plusDays(3);

    private Usuario usuarioCliente;
    private Cliente cliente;
    private Cita solicitudPendiente;

    private final CitaRepositoryPort citaRepositoryPort = new CitaRepositoryPort() {
        @Override
        public List<Cita> buscarSolicitudesManualesPendientes() {
            return citasGuardadas.stream().filter(Cita::isAgendamientoManual)
                    .filter(c -> c.getEstado() == EstadoCita.PENDIENTE && c.getTecnicoId() == null).toList();
        }

        @Override
        public Cita guardar(Cita cita) {
            if (cita.getId() == null) {
                cita.setId((long) (citasGuardadas.size() + 1));
                citasGuardadas.add(cita);
            }
            return cita;
        }

        @Override
        public Optional<Cita> buscarPorId(Long id) {
            return citasGuardadas.stream().filter(c -> c.getId().equals(id)).findFirst();
        }

        @Override
        public List<Cita> listarPorCliente(Long clienteId) {
            return citasGuardadas.stream().filter(c -> clienteId.equals(c.getClienteId())).toList();
        }

        @Override
        public List<Cita> buscarActivasDelTecnicoEntre(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin) {
            return citasGuardadas.stream()
                    .filter(c -> tecnicoId.equals(c.getTecnicoId()) && c.getEstado() != EstadoCita.CANCELADA)
                    .filter(c -> c.getFechaHoraInicio().isBefore(fin) && c.getFechaHoraFin().isAfter(inicio))
                    .toList();
        }
    };

    private final BloqueIndisponibilidadRepositoryPort bloquesRepositoryPort =
            (tecnicoId, inicio, fin) -> new ArrayList<BloqueIndisponibilidad>();

    private final ConfiguracionFranjasRepositoryPort configuracionRepositoryPort = new ConfiguracionFranjasRepositoryPort() {
        @Override
        public Optional<ConfiguracionFranjas> obtenerActual() {
            return Optional.of(new ConfiguracionFranjas(1L, 120, 15, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        }

        @Override
        public ConfiguracionFranjas guardar(ConfiguracionFranjas configuracion) {
            return configuracion;
        }
    };

    private final UsuarioRepositoryPort usuarioRepositoryPort = new UsuarioRepositoryPort() {
        @Override
        public Optional<Usuario> buscarPorNumeroDocumento(String numeroDocumento) {
            return Optional.empty();
        }

        @Override
        public Usuario guardar(Usuario usuario) {
            return usuario;
        }

        @Override
        public Optional<Usuario> buscarPorCorreo(String correo) {
            return correo.equals(usuarioCliente.getCorreo()) ? Optional.of(usuarioCliente) : Optional.empty();
        }

        @Override
        public Optional<Usuario> buscarPorId(Long id) {
            return Optional.empty();
        }

        @Override
        public List<Usuario> listarTodos() {
            return List.of();
        }
    };

    private final ClienteRepositoryPort clienteRepositoryPort = new ClienteRepositoryPort() {
        @Override
        public Optional<Cliente> buscarPorId(Long id) {
            return Optional.of(cliente);
        }

        @Override
        public List<Cliente> listarTodos() {
            return List.of(cliente);
        }

        @Override
        public Cliente guardar(Cliente c) {
            return c;
        }

        @Override
        public Optional<Cliente> buscarPorUsuarioId(Long usuarioId) {
            return Optional.of(cliente);
        }
    };

    private final TecnicoRepositoryPort tecnicoRepositoryPort = new TecnicoRepositoryPort() {
        @Override
        public Tecnico guardar(Tecnico tecnico) {
            return tecnico;
        }

        @Override
        public Optional<Tecnico> buscarPorUsuarioId(Long usuarioId) {
            return Optional.empty();
        }

        @Override
        public Optional<Tecnico> buscarPorId(Long id) {
            return tecnicosDisponibles.stream().filter(t -> t.getId().equals(id)).findFirst();
        }

        @Override
        public List<Tecnico> listarTodos() {
            return tecnicosDisponibles;
        }

        @Override
        public List<Tecnico> buscarActivosPorEspecialidad(Long especialidadId) {
            return tecnicosDisponibles;
        }

        @Override
        public Optional<Tecnico> buscarPorIdBloqueando(Long id) {
            return buscarPorId(id);
        }
    };

    private final RegistroLogPort registroLogPort = new RegistroLogPort() {
        @Override
        public void registrar(NivelLog nivel, TipoLog tipo, String origen, String usuario, String mensaje) {
            logsRegistrados.add(nivel + "|" + tipo + "|" + mensaje);
        }

        @Override
        public List<RegistroLog> listar(String nivel, String tipo, int limite) {
            nivelConsultado = nivel;
            limiteConsultado = limite;
            return List.of();
        }

        @Override
        public int eliminarAnterioresA(LocalDateTime fechaLimite) {
            return 7;
        }
    };

    private final NotificadorCitaPort notificadorCitaPort = new NotificadorCitaPort() {
        @Override
        public void notificarCitaConfirmada(Cita cita, String correoCliente) {
            citasNotificadas.add(cita.getId());
        }

        @Override
        public void notificarCambioDeCita(Cita cita, String correoDestinatario, String mensaje) {
        }
    };

    private CalcularFranjasDisponiblesUseCaseImpl calcularFranjas;
    private ConfirmarCitaUseCaseImpl confirmarCita;

    @BeforeEach
    void preparar() {
        usuarioCliente = new Usuario();
        usuarioCliente.setId(10L);
        usuarioCliente.setCorreo("cliente@correo.com");
        usuarioCliente.setActivo(true);

        cliente = new Cliente();
        cliente.setId(5L);
        cliente.setUsuario(usuarioCliente);

        solicitudPendiente = new Cita();
        solicitudPendiente.setClienteId(5L);
        solicitudPendiente.setEspecialidadId(2L);
        solicitudPendiente.setDescripcionProblema("La lavadora no centrifuga");
        solicitudPendiente.setDireccionServicio("Fusagasugá");
        solicitudPendiente.setEstado(EstadoCita.PENDIENTE);
        citaRepositoryPort.guardar(solicitudPendiente);

        tecnicosDisponibles.clear();
        tecnicosDisponibles.add(crearTecnico(1L, LATITUD_CLIENTE, LONGITUD_CLIENTE, 10));

        calcularFranjas = new CalcularFranjasDisponiblesUseCaseImpl(
                new FiltrarTecnicosPorEspecialidadUseCaseImpl(tecnicoRepositoryPort),
                citaRepositoryPort, bloquesRepositoryPort, configuracionRepositoryPort,
                new HaversineDistanciaAdapter());

        confirmarCita = new ConfirmarCitaUseCaseImpl(
                usuarioRepositoryPort, clienteRepositoryPort, citaRepositoryPort,
                tecnicoRepositoryPort, bloquesRepositoryPort, configuracionRepositoryPort,
                registroLogPort, notificadorCitaPort, java.time.Clock.system(java.time.ZoneId.of("America/Bogota")));
    }

    @Test
    void ofreceTodasLasFranjasDeLaJornadaCuandoElTecnicoEstaLibre() {
        List<FranjaDisponible> franjas = calcularFranjas.calcular(2L, fechaPrueba, LATITUD_CLIENTE, LONGITUD_CLIENTE);

        assertEquals(5, franjas.size());
        assertEquals(fechaPrueba.atTime(8, 0), franjas.get(0).getInicio());
    }

    @Test
    void noOfreceLaFranjaQueYaTieneUnaCitaConfirmada() {
        agendarCitaOcupada(1L, fechaPrueba.atTime(10, 0), fechaPrueba.atTime(12, 0));

        List<FranjaDisponible> franjas = calcularFranjas.calcular(2L, fechaPrueba, LATITUD_CLIENTE, LONGITUD_CLIENTE);

        assertEquals(4, franjas.size());
        assertFalse(franjas.stream().anyMatch(f -> f.getInicio().equals(fechaPrueba.atTime(10, 0))));
    }

    @Test
    void descartaTecnicosFueraDelRadioDeCobertura() {
        tecnicosDisponibles.clear();
        tecnicosDisponibles.add(crearTecnico(1L, 4.9, -74.0, 10));

        List<FranjaDisponible> franjas = calcularFranjas.calcular(2L, fechaPrueba, LATITUD_CLIENTE, LONGITUD_CLIENTE);

        assertTrue(franjas.isEmpty());
    }

    @Test
    void confirmaLaCitaCuandoLaFranjaSigueLibre() {
        Cita cita = confirmarCita.confirmar("cliente@correo.com", solicitudPendiente.getId(), 1L,
                fechaPrueba.atTime(8, 0), fechaPrueba.atTime(10, 0));

        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
        assertEquals(1L, cita.getTecnicoId());
        assertEquals(1, logsRegistrados.size());
        assertTrue(logsRegistrados.get(0).startsWith("INFO|AUDITORIA"));
        assertEquals(List.of(cita.getId()), citasNotificadas);
    }

    @Test
    void consultaDeLogsNormalizaFiltrosYLimiteMaximo() {
        new ConsultarLogsUseCaseImpl(registroLogPort).consultar(" warn ", "", 5000);

        assertEquals("WARN", nivelConsultado);
        assertEquals(500, limiteConsultado);
    }

    @Test
    void limpiezaDeLogsDejaRegistroDeLaOperacion() {
        int eliminados = new LimpiarLogsAntiguosUseCaseImpl(registroLogPort).limpiar(30);

        assertEquals(7, eliminados);
        assertTrue(logsRegistrados.get(0).contains("7 registros"));
    }

    @Test
    void rechazaLaConfirmacionSiLaFranjaYaFueTomada() {
        agendarCitaOcupada(1L, fechaPrueba.atTime(8, 0), fechaPrueba.atTime(10, 0));

        assertThrows(FranjaNoDisponibleException.class, () ->
                confirmarCita.confirmar("cliente@correo.com", solicitudPendiente.getId(), 1L,
                        fechaPrueba.atTime(8, 0), fechaPrueba.atTime(10, 0)));
    }

    @Test
    void rechazaUnaFranjaQueNoCorrespondeALaConfiguracion() {
        assertThrows(FranjaInvalidaException.class, () ->
                confirmarCita.confirmar("cliente@correo.com", solicitudPendiente.getId(), 1L,
                        fechaPrueba.atTime(8, 0), fechaPrueba.atTime(9, 0)));
    }

    @Test
    void rechazaUnaFranjaMasLargaQueElBloqueConfigurado() {
        assertThrows(FranjaInvalidaException.class, () ->
                confirmarCita.confirmar("cliente@correo.com", solicitudPendiente.getId(), 1L,
                        fechaPrueba.atTime(8, 0), fechaPrueba.atTime(11, 0)));
    }

    @Test
    void noOfreceTecnicosSinCoordenadasBase() {
        tecnicosDisponibles.clear();
        tecnicosDisponibles.add(crearTecnicoSinCoordenadas(1L));
        List<List<Coordenadas>> llamadas = new ArrayList<>();

        List<FranjaDisponible> franjas = calcularFranjasConDistancias(llamadas, d -> 1.0)
                .calcular(2L, fechaPrueba, LATITUD_CLIENTE, LONGITUD_CLIENTE);

        assertTrue(franjas.isEmpty());
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void filtraPorRadioConLaDistanciaPorCarreteraYLlamaAlPuertoUnaVez() {
        // Ambos técnicos están en el mismo punto (Haversine daría 0 km); manda la distancia por carretera.
        tecnicosDisponibles.clear();
        tecnicosDisponibles.add(crearTecnico(1L, LATITUD_CLIENTE, LONGITUD_CLIENTE, 10));
        tecnicosDisponibles.add(crearTecnico(2L, LATITUD_CLIENTE, LONGITUD_CLIENTE, 10));
        List<List<Coordenadas>> llamadas = new ArrayList<>();
        List<Double> kmPorCarretera = List.of(25.0, 4.0);
        int[] indice = {0};

        List<FranjaDisponible> franjas = calcularFranjasConDistancias(llamadas, d -> kmPorCarretera.get(indice[0]++))
                .calcular(2L, fechaPrueba, LATITUD_CLIENTE, LONGITUD_CLIENTE);

        assertEquals(1, llamadas.size());
        assertEquals(2, llamadas.get(0).size());
        assertEquals(5, franjas.size());
        assertTrue(franjas.stream().allMatch(f -> f.getTecnicoId().equals(2L) && f.getDistanciaKm() == 4.0));
    }

    @Test
    void rechazaSolicitudSinLatitudOLongitud() {
        assertThrows(CoordenadasInvalidasException.class,
                () -> calcularFranjas.calcular(2L, fechaPrueba, null, LONGITUD_CLIENTE));
        assertThrows(CoordenadasInvalidasException.class,
                () -> calcularFranjas.calcular(2L, fechaPrueba, LATITUD_CLIENTE, null));
    }

    private CalcularFranjasDisponiblesUseCaseImpl calcularFranjasConDistancias(
            List<List<Coordenadas>> llamadas, ToDoubleFunction<Coordenadas> kmPorDestino) {
        CalculadoraDistanciaPort puertoSimulado = (origen, destinos) -> {
            llamadas.add(destinos);
            return destinos.stream().map(d -> new Distancia(kmPorDestino.applyAsDouble(d), 10)).toList();
        };
        return new CalcularFranjasDisponiblesUseCaseImpl(
                new FiltrarTecnicosPorEspecialidadUseCaseImpl(tecnicoRepositoryPort),
                citaRepositoryPort, bloquesRepositoryPort, configuracionRepositoryPort, puertoSimulado);
    }

    private Tecnico crearTecnicoSinCoordenadas(Long id) {
        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        Especialidad especialidad = new Especialidad(2L, "Lavadoras", "", true);
        return new Tecnico(id, usuario, "Base", null, null, 10, List.of(especialidad));
    }

    private Tecnico crearTecnico(Long id, double latitud, double longitud, int radioKm) {
        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        Especialidad especialidad = new Especialidad(2L, "Lavadoras", "", true);
        return new Tecnico(id, usuario, "Base", latitud, longitud, radioKm, List.of(especialidad));
    }

    private void agendarCitaOcupada(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin) {
        Cita ocupada = new Cita();
        ocupada.setClienteId(99L);
        ocupada.setTecnicoId(tecnicoId);
        ocupada.setEspecialidadId(2L);
        ocupada.setFechaHoraInicio(inicio);
        ocupada.setFechaHoraFin(fin);
        ocupada.setEstado(EstadoCita.CONFIRMADA);
        citaRepositoryPort.guardar(ocupada);
    }
}
