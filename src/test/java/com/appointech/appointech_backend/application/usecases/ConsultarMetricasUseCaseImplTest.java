package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.CriteriosMetricas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;
import com.appointech.appointech_backend.domain.models.MetricasGenerales.CalificacionesDeTecnico;
import com.appointech.appointech_backend.domain.models.MetricasGenerales.IngresoDeMetodo;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.RangoMetricas;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.ports.out.ConsultaMetricasPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarMetricasUseCaseImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    /** 2030-06-15 en Bogotá. */
    private static final Clock RELOJ = Clock.fixed(LocalDate.of(2030, 6, 15).atTime(10, 0).atZone(ZONA).toInstant(), ZONA);

    private ConsultaMetricasPort puerto;
    private ConsultarMetricasUseCaseImpl casoDeUso;

    @BeforeEach
    void preparar() {
        puerto = mock(ConsultaMetricasPort.class);
        casoDeUso = new ConsultarMetricasUseCaseImpl(puerto, RELOJ);
        // Sin datos: todos los puertos devuelven vacío.
        when(puerto.citasPorEstado(any())).thenReturn(Map.of());
        when(puerto.solicitudesEspecialesPendientes()).thenReturn(Map.of());
        when(puerto.ingresosPorMetodo(any())).thenReturn(Map.of());
        when(puerto.pagosPorCobrar()).thenReturn(new MetricasGenerales.PagosPorCobrar(0, BigDecimal.ZERO));
        when(puerto.citasPorEspecialidad(any())).thenReturn(List.of());
        when(puerto.calificacionesDelRango(any())).thenReturn(new MetricasGenerales.ResumenCalificaciones(null, 0));
        when(puerto.calificacionesPorTecnico(any())).thenReturn(List.of());
        when(puerto.usuariosActivosPorRol()).thenReturn(Map.of());
    }

    private static CriteriosMetricas sinRango() {
        return new CriteriosMetricas(null, null);
    }

    private RangoMetricas rangoUsado() {
        ArgumentCaptor<RangoMetricas> captor = ArgumentCaptor.forClass(RangoMetricas.class);
        verify(puerto).citasPorEstado(captor.capture());
        return captor.getValue();
    }

    // ---------- rango ----------

    @Test
    void sinParametrosUsaLosUltimos30DiasHastaHoySegunElReloj() {
        MetricasGenerales m = casoDeUso.consultar(sinRango());

        assertEquals(new RangoMetricas(LocalDate.of(2030, 5, 17), LocalDate.of(2030, 6, 15)), m.rango());
        assertEquals(30, m.rango().dias());
        assertEquals(m.rango(), rangoUsado());
    }

    @Test
    void conRangoExplicitoLoRespeta() {
        MetricasGenerales m = casoDeUso.consultar(new CriteriosMetricas(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)));

        assertEquals(new RangoMetricas(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)), m.rango());
        assertEquals(31, m.rango().dias());
    }

    @Test
    void sinHastaTerminaHoyYSinDesdeEmpiezaTreintaDiasAntesDeHasta() {
        assertEquals(new RangoMetricas(LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 15)),
                casoDeUso.consultar(new CriteriosMetricas(LocalDate.of(2030, 6, 1), null)).rango());
        assertEquals(new RangoMetricas(LocalDate.of(2030, 4, 2), LocalDate.of(2030, 5, 1)),
                casoDeUso.consultar(new CriteriosMetricas(null, LocalDate.of(2030, 5, 1))).rango());
    }

    @Test
    void unSoloDiaEsUnRangoValido() {
        LocalDate dia = LocalDate.of(2030, 6, 1);

        assertEquals(1, casoDeUso.consultar(new CriteriosMetricas(dia, dia)).rango().dias());
    }

    @Test
    void elRangoMaximoDe366DiasEsValidoYDe367No() {
        LocalDate hasta = LocalDate.of(2030, 12, 31);

        assertEquals(366, casoDeUso.consultar(new CriteriosMetricas(hasta.minusDays(365), hasta)).rango().dias());
        var e = assertThrows(FiltroCitasInvalidoException.class,
                () -> casoDeUso.consultar(new CriteriosMetricas(hasta.minusDays(366), hasta)));
        assertEquals("El rango no puede superar 366 días", e.getMessage());
    }

    @Test
    void desdePosteriorAHastaEs400SinConsultarNada() {
        CriteriosMetricas invertido = new CriteriosMetricas(LocalDate.of(2030, 6, 2), LocalDate.of(2030, 6, 1));

        var e = assertThrows(FiltroCitasInvalidoException.class, () -> casoDeUso.consultar(invertido));

        assertEquals("desde no puede ser posterior a hasta", e.getMessage());
        verify(puerto, never()).citasPorEstado(any());
    }

    @Test
    void unDesdeEnElFuturoSinHastaEsUnRangoInvertido() {
        assertThrows(FiltroCitasInvalidoException.class,
                () -> casoDeUso.consultar(new CriteriosMetricas(LocalDate.of(2030, 7, 1), null)));
    }

    // ---------- ceros completos ----------

    @Test
    void sinDatosTodasLasClavesVienenEnCero() {
        MetricasGenerales m = casoDeUso.consultar(sinRango());

        assertEquals(0, m.citas().total());
        assertEquals(EstadoCita.values().length, m.citas().porEstado().size());
        assertTrue(m.citas().porEstado().values().stream().allMatch(v -> v == 0L));
        assertEquals(0.0, m.citas().tasaCancelacion());
        assertEquals(0, m.solicitudesEspeciales().total());
        assertEquals(MotivoSolicitudEspecial.values().length, m.solicitudesEspeciales().porMotivo().size());
        assertEquals(0, m.ingresos().total().signum());
        assertEquals(0, m.ingresos().cantidadPagos());
        assertEquals(List.of(MetodoPago.values()), List.copyOf(m.ingresos().porMetodo().keySet()));
        assertTrue(m.ingresos().porMetodo().values().stream()
                .allMatch(i -> i.monto().signum() == 0 && i.cantidad() == 0));
        assertEquals(0, m.ingresos().porCobrar().cantidad());
        assertTrue(m.especialidades().isEmpty());
        assertNull(m.calificaciones().promedio());
        assertEquals(0, m.calificaciones().total());
        assertTrue(m.calificaciones().porTecnico().isEmpty());
        assertEquals(0, m.usuariosActivos().total());
        assertEquals(RolUsuario.values().length, m.usuariosActivos().porRol().size());
    }

    // ---------- citas y tasa de cancelación ----------

    @Test
    void completaLosEstadosFaltantesConCeroYSumaElTotal() {
        when(puerto.citasPorEstado(any())).thenReturn(Map.of(EstadoCita.FINALIZADA, 6L, EstadoCita.CANCELADA, 2L));

        MetricasGenerales.Citas c = casoDeUso.consultar(sinRango()).citas();

        assertEquals(8, c.total());
        assertEquals(6L, c.porEstado().get(EstadoCita.FINALIZADA));
        assertEquals(0L, c.porEstado().get(EstadoCita.PENDIENTE));
        assertEquals(List.of(EstadoCita.values()), List.copyOf(c.porEstado().keySet()));
    }

    @Test
    void tasaDeCancelacionEsCanceladasSobreTotalEnPorcentajeConDosDecimales() {
        when(puerto.citasPorEstado(any())).thenReturn(Map.of(EstadoCita.FINALIZADA, 2L, EstadoCita.CANCELADA, 1L));

        assertEquals(33.33, casoDeUso.consultar(sinRango()).citas().tasaCancelacion());
    }

    @Test
    void tasaDeCancelacionRedondeaHaciaArribaEnLaMitadYLlegaAl100() {
        when(puerto.citasPorEstado(any())).thenReturn(Map.of(EstadoCita.CANCELADA, 1L, EstadoCita.PENDIENTE, 7L));
        assertEquals(12.5, casoDeUso.consultar(sinRango()).citas().tasaCancelacion());

        when(puerto.citasPorEstado(any())).thenReturn(Map.of(EstadoCita.CANCELADA, 4L));
        assertEquals(100.0, casoDeUso.consultar(sinRango()).citas().tasaCancelacion());

        when(puerto.citasPorEstado(any())).thenReturn(Map.of(EstadoCita.CANCELADA, 1L, EstadoCita.FINALIZADA, 15999L));
        assertEquals(0.01, casoDeUso.consultar(sinRango()).citas().tasaCancelacion());
    }

    @Test
    void sinCancelacionesLaTasaEsCero() {
        when(puerto.citasPorEstado(any())).thenReturn(Map.of(EstadoCita.FINALIZADA, 5L));

        assertEquals(0.0, casoDeUso.consultar(sinRango()).citas().tasaCancelacion());
    }

    // ---------- solicitudes especiales ----------

    @Test
    void solicitudesEspecialesPorMotivoConElTotalYElMotivoFaltanteEnCero() {
        when(puerto.solicitudesEspecialesPendientes()).thenReturn(Map.of(MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD, 3L));

        MetricasGenerales.SolicitudesEspeciales s = casoDeUso.consultar(sinRango()).solicitudesEspeciales();

        assertEquals(3, s.total());
        assertEquals(0L, s.porMotivo().get(MotivoSolicitudEspecial.FUERA_DE_COBERTURA));
        assertEquals(3L, s.porMotivo().get(MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD));
    }

    // ---------- ingresos ----------

    @Test
    void sumaLosIngresosSinPerderPrecisionYDejaElMetodoSinPagosEnCero() {
        when(puerto.ingresosPorMetodo(any())).thenReturn(Map.of(
                MetodoPago.EFECTIVO, new IngresoDeMetodo(new BigDecimal("90000.50"), 1)));
        when(puerto.pagosPorCobrar()).thenReturn(new MetricasGenerales.PagosPorCobrar(2, new BigDecimal("60000.10")));

        MetricasGenerales.Ingresos i = casoDeUso.consultar(sinRango()).ingresos();

        assertEquals(new BigDecimal("90000.50"), i.total());
        assertEquals(1, i.cantidadPagos());
        assertEquals(0, i.porMetodo().get(MetodoPago.EN_LINEA).monto().signum());
        assertEquals(0, i.porMetodo().get(MetodoPago.EN_LINEA).cantidad());
        assertEquals(2, i.porCobrar().cantidad());
        assertEquals(new BigDecimal("60000.10"), i.porCobrar().monto());
    }

    @Test
    void ambosMetodosSumanAlTotalExacto() {
        when(puerto.ingresosPorMetodo(any())).thenReturn(Map.of(
                MetodoPago.EFECTIVO, new IngresoDeMetodo(new BigDecimal("0.10"), 1),
                MetodoPago.EN_LINEA, new IngresoDeMetodo(new BigDecimal("0.20"), 2)));

        MetricasGenerales.Ingresos i = casoDeUso.consultar(sinRango()).ingresos();

        assertEquals(new BigDecimal("0.30"), i.total());
        assertEquals(3, i.cantidadPagos());
    }

    // ---------- especialidades ----------

    @Test
    void entregaLasEspecialidadesTalComoLasDevuelveElPuertoConSusCeros() {
        List<MetricasGenerales.CitasDeEspecialidad> lista = List.of(
                new MetricasGenerales.CitasDeEspecialidad(1L, "Hornos", 0, 0),
                new MetricasGenerales.CitasDeEspecialidad(2L, "Lavadoras", 5, 3));
        when(puerto.citasPorEspecialidad(any())).thenReturn(lista);

        assertEquals(lista, casoDeUso.consultar(sinRango()).especialidades());
    }

    // ---------- calificaciones ----------

    @Test
    void redondeaLosPromediosADosDecimalesYConservaElOrdenDelPuerto() {
        when(puerto.calificacionesDelRango(any())).thenReturn(new MetricasGenerales.ResumenCalificaciones(4.3333333, 3));
        when(puerto.calificacionesPorTecnico(any())).thenReturn(List.of(
                new CalificacionesDeTecnico(7L, "Luis Mora", 4.666666, 3),
                new CalificacionesDeTecnico(3L, "Ana Ruiz", 4.125, 2)));

        MetricasGenerales.Calificaciones c = casoDeUso.consultar(sinRango()).calificaciones();

        assertEquals(4.33, c.promedio());
        assertEquals(3, c.total());
        assertEquals(List.of(7L, 3L), c.porTecnico().stream().map(CalificacionesDeTecnico::tecnicoId).toList());
        assertEquals(4.67, c.porTecnico().get(0).promedio());
        assertEquals(4.13, c.porTecnico().get(1).promedio());
        assertEquals(2, c.porTecnico().get(1).cantidad());
        assertEquals("Luis Mora", c.porTecnico().get(0).nombre());
    }

    // ---------- usuarios ----------

    @Test
    void usuariosActivosPorRolConElTotalYLosRolesSinUsuariosEnCero() {
        when(puerto.usuariosActivosPorRol()).thenReturn(Map.of(RolUsuario.CLIENTE, 10L, RolUsuario.TECNICO, 4L));

        MetricasGenerales.UsuariosActivos u = casoDeUso.consultar(sinRango()).usuariosActivos();

        assertEquals(14, u.total());
        assertEquals(0L, u.porRol().get(RolUsuario.ADMINISTRADOR));
        assertEquals(10L, u.porRol().get(RolUsuario.CLIENTE));
    }
}
