package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ReglasFranjasTrabajo;
import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.exception.FranjaTrabajoConflictoException;
import com.appointech.appointech_backend.domain.exception.FranjaTrabajoInvalidaException;
import com.appointech.appointech_backend.domain.exception.FranjaTrabajoNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Crear, cambiar, borrar y listar franjas de trabajo, y el radio de atención por técnico. */
class FranjasTrabajoUseCasesTest {

    @Test
    void listarVariasFranjasConsultaLaConfiguracionUnaSolaVez() {
        franjas.abrir(1L, MANANA, 8, 12);
        franjas.abrir(1L, MANANA.plusDays(1), 8, 12);
        var resultado = consultar.listar(1L, MANANA, MANANA.plusDays(1));
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(f -> f.bloques() == 2 && f.bloquesOcupados() == 0));
        verify(configuracion, org.mockito.Mockito.times(1)).obtenerActual();
    }

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    /** Jueves 2030-05-02 a las 10:00 en Bogotá. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2030-05-02T15:00:00Z"), BOGOTA);
    private static final LocalDate HOY = LocalDate.of(2030, 5, 2);
    private static final LocalDate MANANA = HOY.plusDays(1);
    private static final String ADMIN = "admin@appointech.com";

    private FranjasEnMemoria franjas;
    private TecnicoRepositoryPort tecnicos;
    private UsuarioRepositoryPort usuarios;
    private BloqueIndisponibilidadRepositoryPort bloques;
    private ConsultaCitasDelTecnicoPort consultaCitas;
    private ConfiguracionFranjasRepositoryPort configuracion;
    private RegistroLogPort log;
    private AdministrarFranjasTrabajoUseCaseImpl administrar;
    private ConsultarFranjasTrabajoUseCaseImpl consultar;
    private CambiarRadioAtencionUseCaseImpl cambiarRadio;

    @BeforeEach
    void preparar() {
        franjas = new FranjasEnMemoria();
        tecnicos = mock(TecnicoRepositoryPort.class);
        usuarios = mock(UsuarioRepositoryPort.class);
        bloques = mock(BloqueIndisponibilidadRepositoryPort.class);
        consultaCitas = mock(ConsultaCitasDelTecnicoPort.class);
        configuracion = mock(ConfiguracionFranjasRepositoryPort.class);
        log = mock(RegistroLogPort.class);
        when(configuracion.obtenerActual()).thenReturn(Optional.empty());      // duración por defecto: 120 min
        Tecnico tecnico = new Tecnico();
        tecnico.setId(1L);
        tecnico.setRadioCoberturaKm(15);
        when(tecnicos.buscarPorIdBloqueando(1L)).thenReturn(Optional.of(tecnico));
        when(tecnicos.buscarPorId(1L)).thenReturn(Optional.of(tecnico));
        when(tecnicos.guardar(any(Tecnico.class))).thenAnswer(i -> i.getArgument(0));
        Usuario admin = new Usuario();
        admin.setId(77L);
        when(usuarios.buscarPorCorreo(ADMIN)).thenReturn(Optional.of(admin));
        when(bloques.buscarDelTecnicoEntre(anyLong(), any(), any())).thenReturn(List.of());
        when(consultaCitas.buscarPorEstadosEntre(anyLong(), anyCollection(), any(), any())).thenReturn(List.of());

        ReglasFranjasTrabajo reglas = new ReglasFranjasTrabajo(configuracion, franjas, bloques, consultaCitas, RELOJ);
        administrar = new AdministrarFranjasTrabajoUseCaseImpl(tecnicos, usuarios, franjas, reglas, log);
        consultar = new ConsultarFranjasTrabajoUseCaseImpl(tecnicos, franjas, reglas);
        cambiarRadio = new CambiarRadioAtencionUseCaseImpl(tecnicos, log);
    }

    private static Cita cita(long id, LocalDateTime inicio, LocalDateTime fin) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(fin);
        return cita;
    }

    private FranjaTrabajoConOcupacion crear(LocalDate fecha, int inicio, int fin) {
        return administrar.crear(ADMIN, 1L, fecha, LocalTime.of(inicio, 0), LocalTime.of(fin, 0));
    }

    // ---------- crear ----------

    @Test
    void creaLaFranjaConQuienLaCreoYCuantosBloquesTiene() {
        FranjaTrabajoConOcupacion creada = crear(MANANA, 8, 12);

        assertEquals(1L, creada.franja().id());
        assertEquals(MANANA, creada.franja().fecha());
        assertEquals(LocalTime.of(8, 0), creada.franja().horaInicio());
        assertEquals(77L, creada.franja().creadaPorId());
        assertEquals(LocalDateTime.of(2030, 5, 2, 10, 0), creada.franja().creadaEn());
        assertEquals(2, creada.bloques());
        assertEquals(0, creada.bloquesOcupados());
        assertEquals(1, franjas.franjas.size());
        verify(log).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void variasFranjasSeguidasOEnOtroDiaNoSeCruzan() {
        crear(MANANA, 8, 12);
        crear(MANANA, 12, 16);
        crear(MANANA.plusDays(1), 8, 12);

        assertEquals(3, franjas.franjas.size());
    }

    @Test
    void conUnTecnicoInexistenteEs404() {
        when(tecnicos.buscarPorIdBloqueando(99L)).thenReturn(Optional.empty());

        assertThrows(TecnicoNoEncontradoException.class,
                () -> administrar.crear(ADMIN, 99L, MANANA, LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    @Test
    void conUnAdministradorDesconocidoNoCreaNada() {
        assertThrows(UsuarioNoEncontradoException.class,
                () -> administrar.crear("nadie@x.com", 1L, MANANA, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        assertTrue(franjas.franjas.isEmpty());
    }

    @Test
    void losDatosInvalidosSonFranjaTrabajoInvalida() {
        assertThrows(FranjaTrabajoInvalidaException.class, () -> crear(MANANA, 12, 8));
        assertThrows(FranjaTrabajoInvalidaException.class, () -> crear(MANANA, 8, 9));
        assertThrows(FranjaTrabajoInvalidaException.class, () -> crear(HOY.minusDays(1), 8, 12));
        assertThrows(FranjaTrabajoInvalidaException.class, () -> crear(HOY, 9, 14));
        assertTrue(franjas.franjas.isEmpty());
    }

    @Test
    void siEsHoyYLaHoraNoHaPasadoSeCrea() {
        crear(HOY, 11, 15);

        assertEquals(1, franjas.franjas.size());
    }

    @Test
    void laDuracionGlobalDeLaCitaDefineElMinimoDeLaFranja() {
        when(configuracion.obtenerActual()).thenReturn(Optional.of(
                new com.appointech.appointech_backend.domain.models.ConfiguracionFranjas(1L, 60, 15, LocalTime.of(8, 0), LocalTime.of(18, 0))));

        FranjaTrabajoConOcupacion creada = administrar.crear(ADMIN, 1L, MANANA, LocalTime.of(8, 0), LocalTime.of(9, 0));

        assertEquals(1, creada.bloques());
    }

    @Test
    void siSeCruzaConOtraFranjaDelMismoTecnicoDiceConCualYNoCrea() {
        crear(MANANA, 8, 12);

        FranjaTrabajoConflictoException ex = assertThrows(FranjaTrabajoConflictoException.class, () -> crear(MANANA, 10, 14));

        assertEquals("La franja se cruza con otra franja del técnico: 2030-05-03 de 08:00 a 12:00", ex.getMessage());
        assertEquals(1, franjas.franjas.size());
    }

    @Test
    void siSeCruzaConUnaNoDisponibilidadReportaElBloqueConFechasYHorasYNoCrea() {
        when(bloques.buscarDelTecnicoEntre(1L, MANANA.atTime(8, 0), MANANA.atTime(12, 0))).thenReturn(List.of(
                new BloqueIndisponibilidad(5L, 1L, MANANA.atTime(9, 0), MANANA.atTime(10, 30), "Cita médica privada")));

        FranjaTrabajoConflictoException ex = assertThrows(FranjaTrabajoConflictoException.class, () -> crear(MANANA, 8, 12));

        assertEquals("La franja se cruza con una no disponibilidad del técnico (del 2030-05-03 09:00 al 2030-05-03 10:30); "
                + "no se creó la franja", ex.getMessage());
        assertTrue(!ex.getMessage().contains("médica"), "el motivo del técnico es privado y no se reporta");
        assertTrue(franjas.franjas.isEmpty());
    }

    @Test
    void conVariasNoDisponibilidadesLasReportaTodas() {
        when(bloques.buscarDelTecnicoEntre(anyLong(), any(), any())).thenReturn(List.of(
                new BloqueIndisponibilidad(5L, 1L, MANANA.atTime(8, 30), MANANA.atTime(9, 0), null),
                new BloqueIndisponibilidad(6L, 1L, MANANA.atTime(11, 0), MANANA.atTime(11, 30), null)));

        FranjaTrabajoConflictoException ex = assertThrows(FranjaTrabajoConflictoException.class, () -> crear(MANANA, 8, 12));

        assertTrue(ex.getMessage().contains("del 2030-05-03 08:30 al 2030-05-03 09:00; del 2030-05-03 11:00 al 2030-05-03 11:30"));
    }

    // ---------- cambiar ----------

    @Test
    void cambiaLaFranjaSinCitasConservandoQuienLaCreoYSinChocarConSiMisma() {
        FranjaTrabajo original = crear(MANANA, 8, 12).franja();

        FranjaTrabajoConOcupacion cambiada = administrar.actualizar(ADMIN, 1L, original.id(), MANANA,
                LocalTime.of(9, 0), LocalTime.of(13, 0));

        assertEquals(original.id(), cambiada.franja().id());
        assertEquals(LocalTime.of(9, 0), cambiada.franja().horaInicio());
        assertEquals(original.creadaPorId(), cambiada.franja().creadaPorId());
        assertEquals(original.creadaEn(), cambiada.franja().creadaEn());
        assertEquals(1, franjas.franjas.size());
    }

    @Test
    void alCambiarTambienSeVigilanLosCrucesConLasOtrasFranjasYConLasNoDisponibilidades() {
        crear(MANANA, 14, 18);
        FranjaTrabajo franja = crear(MANANA, 8, 12).franja();

        assertThrows(FranjaTrabajoConflictoException.class,
                () -> administrar.actualizar(ADMIN, 1L, franja.id(), MANANA, LocalTime.of(10, 0), LocalTime.of(15, 0)));

        when(bloques.buscarDelTecnicoEntre(1L, MANANA.atTime(8, 0), MANANA.atTime(13, 0))).thenReturn(List.of(
                new BloqueIndisponibilidad(5L, 1L, MANANA.atTime(12, 0), MANANA.atTime(12, 30), null)));
        assertThrows(FranjaTrabajoConflictoException.class,
                () -> administrar.actualizar(ADMIN, 1L, franja.id(), MANANA, LocalTime.of(8, 0), LocalTime.of(13, 0)));
        assertEquals(LocalTime.of(12, 0), franjas.buscarDelTecnico(franja.id(), 1L).orElseThrow().horaFin());
    }

    @Test
    void cambiarConDatosInvalidosEs400() {
        FranjaTrabajo franja = crear(MANANA, 8, 12).franja();

        assertThrows(FranjaTrabajoInvalidaException.class,
                () -> administrar.actualizar(ADMIN, 1L, franja.id(), HOY.minusDays(1), LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    @Test
    void siLaFranjaYaTieneCitasNoSeCambiaNiSeBorraYDiceCuantas() {
        FranjaTrabajo franja = crear(MANANA, 8, 12).franja();
        when(consultaCitas.buscarPorEstadosEntre(anyLong(), anyCollection(), any(), any())).thenReturn(List.of(
                cita(1, MANANA.atTime(8, 0), MANANA.atTime(10, 0)), cita(2, MANANA.atTime(10, 0), MANANA.atTime(12, 0))));

        FranjaTrabajoConflictoException cambiar = assertThrows(FranjaTrabajoConflictoException.class,
                () -> administrar.actualizar(ADMIN, 1L, franja.id(), MANANA, LocalTime.of(8, 0), LocalTime.of(14, 0)));
        FranjaTrabajoConflictoException borrar = assertThrows(FranjaTrabajoConflictoException.class,
                () -> administrar.eliminar(ADMIN, 1L, franja.id()));

        assertEquals("No se puede cambiar la franja 2030-05-03 de 08:00 a 12:00: ya tiene 2 citas agendadas. "
                + "Cancélalas o reasígnalas antes", cambiar.getMessage());
        assertTrue(borrar.getMessage().startsWith("No se puede borrar la franja"));
        assertEquals(1, franjas.franjas.size());
        assertEquals(LocalTime.of(12, 0), franjas.franjas.get(0).horaFin());
    }

    @Test
    void conUnaSolaCitaElMensajeVaEnSingular() {
        FranjaTrabajo franja = crear(MANANA, 8, 12).franja();
        when(consultaCitas.buscarPorEstadosEntre(anyLong(), anyCollection(), any(), any())).thenReturn(List.of(
                cita(1, MANANA.atTime(8, 0), MANANA.atTime(10, 0))));

        FranjaTrabajoConflictoException ex = assertThrows(FranjaTrabajoConflictoException.class,
                () -> administrar.eliminar(ADMIN, 1L, franja.id()));

        assertTrue(ex.getMessage().contains("ya tiene 1 cita agendada."));
    }

    @Test
    void laFranjaDeOtroTecnicoOInexistenteEs404() {
        FranjaTrabajo franja = crear(MANANA, 8, 12).franja();
        Tecnico otro = new Tecnico();
        otro.setId(2L);
        when(tecnicos.buscarPorIdBloqueando(2L)).thenReturn(Optional.of(otro));

        assertThrows(FranjaTrabajoNoEncontradaException.class,
                () -> administrar.actualizar(ADMIN, 2L, franja.id(), MANANA, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        assertThrows(FranjaTrabajoNoEncontradaException.class, () -> administrar.eliminar(ADMIN, 1L, 999L));
        when(tecnicos.buscarPorIdBloqueando(99L)).thenReturn(Optional.empty());
        assertThrows(TecnicoNoEncontradoException.class, () -> administrar.eliminar(ADMIN, 99L, 1L));
    }

    // ---------- borrar ----------

    @Test
    void borraLaFranjaSinCitas() {
        FranjaTrabajo franja = crear(MANANA, 8, 12).franja();

        administrar.eliminar(ADMIN, 1L, franja.id());

        assertTrue(franjas.franjas.isEmpty());
    }

    // ---------- listar ----------

    @Test
    void listaOrdenadoPorFechaYHoraConBloquesYOcupadosPorCitas() {
        crear(MANANA.plusDays(1), 8, 12);
        crear(MANANA, 14, 18);
        crear(MANANA, 8, 12);
        when(consultaCitas.buscarPorEstadosEntre(anyLong(), anyCollection(), any(), any())).thenReturn(List.of(
                cita(1, MANANA.atTime(10, 0), MANANA.atTime(12, 0)),
                cita(2, MANANA.atTime(14, 0), MANANA.atTime(16, 0)),
                cita(3, MANANA.atTime(16, 0), MANANA.atTime(18, 0))));

        List<FranjaTrabajoConOcupacion> lista = consultar.listar(1L, null, null);

        assertEquals(3, lista.size());
        assertEquals(List.of(MANANA.atTime(8, 0), MANANA.atTime(14, 0), MANANA.plusDays(1).atTime(8, 0)),
                lista.stream().map(f -> f.franja().inicio()).toList());
        assertEquals(List.of(1, 2, 0), lista.stream().map(FranjaTrabajoConOcupacion::bloquesOcupados).toList());
        assertEquals(List.of(2, 2, 2), lista.stream().map(FranjaTrabajoConOcupacion::bloques).toList());
    }

    @Test
    void sinFechasUsaDesdeHoyYLos30DiasSiguientes() {
        crear(HOY.plusDays(30), 8, 12);
        crear(HOY.plusDays(31), 8, 12);

        List<FranjaTrabajoConOcupacion> lista = consultar.listar(1L, null, null);

        assertEquals(List.of(HOY.plusDays(30)), lista.stream().map(f -> f.franja().fecha()).toList());
        verify(consultaCitas).buscarPorEstadosEntre(1L, ReglasEstados.OCUPAN, HOY.atStartOfDay(), HOY.plusDays(31).atStartOfDay());
    }

    @Test
    void conRangoPropioFiltraPorEseRango() {
        crear(MANANA, 8, 12);
        crear(MANANA.plusDays(5), 8, 12);

        assertEquals(1, consultar.listar(1L, MANANA.plusDays(4), MANANA.plusDays(6)).size());
    }

    @Test
    void elRangoNoPuedeSerInvertidoNiSuperar90Dias() {
        assertThrows(FiltroCitasInvalidoException.class, () -> consultar.listar(1L, MANANA.plusDays(1), MANANA));
        assertThrows(FiltroCitasInvalidoException.class, () -> consultar.listar(1L, HOY, HOY.plusDays(91)));
        assertEquals(0, consultar.listar(1L, HOY, HOY.plusDays(90)).size());
    }

    @Test
    void listarConTecnicoInexistenteEs404() {
        when(tecnicos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(TecnicoNoEncontradoException.class, () -> consultar.listar(99L, null, null));
    }

    // ---------- radio de atención ----------

    @Test
    void cambiaElRadioDeAtencionDelTecnico() {
        Tecnico tecnico = cambiarRadio.cambiar(ADMIN, 1L, 30);

        assertEquals(30, tecnico.getRadioCoberturaKm());
        verify(tecnicos).guardar(tecnico);
        verify(log).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void unRadioFueraDeRangoEs400YNoGuarda() {
        assertThrows(RadioCoberturaInvalidoException.class, () -> cambiarRadio.cambiar(ADMIN, 1L, 0));
        assertThrows(RadioCoberturaInvalidoException.class, () -> cambiarRadio.cambiar(ADMIN, 1L, 101));
        assertThrows(RadioCoberturaInvalidoException.class, () -> cambiarRadio.cambiar(ADMIN, 1L, null));
        verify(tecnicos, org.mockito.Mockito.never()).guardar(any());
    }

    @Test
    void cambiarElRadioDeUnTecnicoInexistenteEs404() {
        when(tecnicos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(TecnicoNoEncontradoException.class, () -> cambiarRadio.cambiar(ADMIN, 99L, 20));
    }

    /** Los estados que ocupan agenda, tal como los consulta el caso de uso. */
    private static final class ReglasEstados {
        static final List<com.appointech.appointech_backend.domain.models.EstadoCita> OCUPAN = List.of(
                com.appointech.appointech_backend.domain.models.EstadoCita.CONFIRMADA,
                com.appointech.appointech_backend.domain.models.EstadoCita.EN_CAMINO,
                com.appointech.appointech_backend.domain.models.EstadoCita.EN_PROCESO);

        private ReglasEstados() {
        }
    }
}
