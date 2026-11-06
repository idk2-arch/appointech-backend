package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.RangoMetricas;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.MetricasFilas;
import com.appointech.appointech_backend.infrastructure.repositories.MetricasJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaMetricasAdapterTest {

    private static final RangoMetricas RANGO = new RangoMetricas(LocalDate.of(2030, 5, 10), LocalDate.of(2030, 5, 12));
    /** Inicio del primer día y, sin incluirlo, inicio del día siguiente al último: así ambos extremos son inclusivos. */
    private static final LocalDateTime DESDE = LocalDateTime.of(2030, 5, 10, 0, 0);
    private static final LocalDateTime HASTA_EXCLUSIVO = LocalDateTime.of(2030, 5, 13, 0, 0);

    @Mock
    private MetricasJpaRepository jpa;
    @InjectMocks
    private ConsultaMetricasAdapter adapter;

    @Test
    void citasPorEstadoConvierteElEstadoYUsaElRangoInclusivo() {
        when(jpa.citasPorEstado(DESDE, HASTA_EXCLUSIVO)).thenReturn(List.of(
                new MetricasFilas.ConteoEstado(EstadoCitaEntity.FINALIZADA, 4L),
                new MetricasFilas.ConteoEstado(EstadoCitaEntity.CANCELADA, 1L)));

        assertEquals(Map.of(EstadoCita.FINALIZADA, 4L, EstadoCita.CANCELADA, 1L), adapter.citasPorEstado(RANGO));
    }

    @Test
    void solicitudesEspecialesPendientesConvierteElMotivoYPideSoloPendientes() {
        when(jpa.solicitudesEspecialesPorMotivo(EstadoCitaEntity.PENDIENTE)).thenReturn(List.of(
                new MetricasFilas.ConteoMotivo("SIN_TECNICOS_ESPECIALIDAD", 2L),
                new MetricasFilas.ConteoMotivo("FUERA_DE_COBERTURA", 5L)));

        assertEquals(Map.of(MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD, 2L, MotivoSolicitudEspecial.FUERA_DE_COBERTURA, 5L),
                adapter.solicitudesEspecialesPendientes());
    }

    @Test
    void ingresosPorMetodoSoloDePagosPagadosEnElRango() {
        when(jpa.ingresosPorMetodo(EstadoPago.PAGADO, DESDE, HASTA_EXCLUSIVO)).thenReturn(List.of(
                new MetricasFilas.IngresoMetodo(MetodoPago.EFECTIVO, new BigDecimal("90000.50"), 3L)));

        assertEquals(Map.of(MetodoPago.EFECTIVO, new MetricasGenerales.IngresoDeMetodo(new BigDecimal("90000.50"), 3)),
                adapter.ingresosPorMetodo(RANGO));
    }

    @Test
    void pagosPorCobrarSoloDePagosPendientes() {
        when(jpa.pagosPorCobrar(EstadoPago.PENDIENTE)).thenReturn(new MetricasFilas.PorCobrar(2L, new BigDecimal("60000.00")));

        assertEquals(new MetricasGenerales.PagosPorCobrar(2, new BigDecimal("60000.00")), adapter.pagosPorCobrar());
    }

    @Test
    void sinPagosPorCobrarElMontoNuloSeConvierteEnCero() {
        when(jpa.pagosPorCobrar(EstadoPago.PENDIENTE)).thenReturn(new MetricasFilas.PorCobrar(0L, null));

        assertEquals(new MetricasGenerales.PagosPorCobrar(0, BigDecimal.ZERO), adapter.pagosPorCobrar());
    }

    @Test
    void citasPorEspecialidadConservaLaEspecialidadSinCitas() {
        when(jpa.citasPorEspecialidad(EstadoCitaEntity.FINALIZADA, DESDE, HASTA_EXCLUSIVO)).thenReturn(List.of(
                new MetricasFilas.EspecialidadConteo(1L, "Hornos", 0L, 0L),
                new MetricasFilas.EspecialidadConteo(2L, "Lavadoras", 5L, 3L),
                new MetricasFilas.EspecialidadConteo(3L, "Neveras", 2L, null)));

        assertEquals(List.of(
                        new MetricasGenerales.CitasDeEspecialidad(1L, "Hornos", 0, 0),
                        new MetricasGenerales.CitasDeEspecialidad(2L, "Lavadoras", 5, 3),
                        new MetricasGenerales.CitasDeEspecialidad(3L, "Neveras", 2, 0)),
                adapter.citasPorEspecialidad(RANGO));
    }

    @Test
    void calificacionesDelRangoConservaElPromedioNuloCuandoNoHay() {
        when(jpa.calificacionesDelRango(DESDE, HASTA_EXCLUSIVO)).thenReturn(new MetricasFilas.ResumenCalificaciones(null, 0L));

        assertEquals(new MetricasGenerales.ResumenCalificaciones(null, 0), adapter.calificacionesDelRango(RANGO));
    }

    @Test
    void calificacionesPorTecnicoArmaElNombreCompleto() {
        when(jpa.calificacionesPorTecnico(DESDE, HASTA_EXCLUSIVO)).thenReturn(List.of(
                new MetricasFilas.CalificacionTecnico(7L, "Luis", "Mora", 4.5, 2L)));

        assertEquals(List.of(new MetricasGenerales.CalificacionesDeTecnico(7L, "Luis Mora", 4.5, 2)),
                adapter.calificacionesPorTecnico(RANGO));
        verify(jpa).calificacionesPorTecnico(DESDE, HASTA_EXCLUSIVO);
    }

    @Test
    void usuariosActivosPorRolConvierteElRol() {
        when(jpa.usuariosActivosPorRol()).thenReturn(List.of(
                new MetricasFilas.ConteoRol(RolUsuarioEntity.CLIENTE, 9L),
                new MetricasFilas.ConteoRol(RolUsuarioEntity.ADMINISTRADOR, 1L)));

        assertEquals(Map.of(RolUsuario.CLIENTE, 9L, RolUsuario.ADMINISTRADOR, 1L), adapter.usuariosActivosPorRol());
    }
}
