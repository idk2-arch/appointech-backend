package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.CriteriosMetricas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.RangoMetricas;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.infrastructure.dto.MetricasResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricasMapperTest {

    private static <E extends Enum<E>> Map<E, Long> completo(Class<E> tipo, Map<E, Long> valores) {
        Map<E, Long> mapa = new EnumMap<>(tipo);
        for (E e : tipo.getEnumConstants()) {
            mapa.put(e, 0L);
        }
        mapa.putAll(valores);
        return mapa;
    }

    /** Un resultado completo y coherente, reutilizado por la prueba HTTP. */
    static MetricasGenerales ejemplo() {
        Map<MetodoPago, MetricasGenerales.IngresoDeMetodo> metodos = new EnumMap<>(MetodoPago.class);
        metodos.put(MetodoPago.EFECTIVO, new MetricasGenerales.IngresoDeMetodo(new BigDecimal("90000.50"), 1));
        metodos.put(MetodoPago.EN_LINEA, new MetricasGenerales.IngresoDeMetodo(new BigDecimal("50000.25"), 1));
        return new MetricasGenerales(
                new RangoMetricas(LocalDate.of(2030, 5, 10), LocalDate.of(2030, 5, 12)),
                new MetricasGenerales.Citas(8, completo(EstadoCita.class, Map.of(EstadoCita.FINALIZADA, 6L, EstadoCita.CANCELADA, 2L)), 25.0),
                new MetricasGenerales.SolicitudesEspeciales(3, completo(MotivoSolicitudEspecial.class,
                        Map.of(MotivoSolicitudEspecial.FUERA_DE_COBERTURA, 2L, MotivoSolicitudEspecial.SIN_TECNICOS_ESPECIALIDAD, 1L))),
                new MetricasGenerales.Ingresos(new BigDecimal("140000.75"), 2, metodos,
                        new MetricasGenerales.PagosPorCobrar(1, new BigDecimal("30000.00"))),
                List.of(new MetricasGenerales.CitasDeEspecialidad(1L, "Hornos", 0, 0),
                        new MetricasGenerales.CitasDeEspecialidad(2L, "Lavadoras", 5, 3)),
                new MetricasGenerales.Calificaciones(4.0, 3,
                        List.of(new MetricasGenerales.CalificacionesDeTecnico(7L, "Luis Mora", 4.5, 2))),
                new MetricasGenerales.UsuariosActivos(14, completo(RolUsuario.class,
                        Map.of(RolUsuario.CLIENTE, 10L, RolUsuario.TECNICO, 4L))));
    }

    @Test
    void sinFechasLosCriteriosVanVacios() {
        assertEquals(new CriteriosMetricas(null, null), MetricasMapper.aCriterios(null, " "));
    }

    @Test
    void conviertePasaLasFechas() {
        assertEquals(new CriteriosMetricas(LocalDate.of(2030, 5, 10), LocalDate.of(2030, 5, 12)),
                MetricasMapper.aCriterios("2030-05-10", " 2030-05-12 "));
    }

    @Test
    void unaFechaInvalidaEsUnFiltroInvalido() {
        assertThrows(com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException.class,
                () -> MetricasMapper.aCriterios("2030-13-01", null));
    }

    @Test
    void agrupaLaRespuestaConNombresClarosYTodasLasClavesEnElOrdenDelEnum() {
        MetricasResponse r = MetricasMapper.aRespuesta(ejemplo());

        assertEquals(new MetricasResponse.RangoResponse("2030-05-10", "2030-05-12", 3), r.rango());
        assertEquals(List.of("PENDIENTE", "CONFIRMADA", "EN_CAMINO", "EN_PROCESO", "FINALIZADA", "CANCELADA"),
                List.copyOf(r.citas().porEstado().keySet()));
        assertEquals(6L, r.citas().porEstado().get("FINALIZADA"));
        assertEquals(0L, r.citas().porEstado().get("EN_CAMINO"));
        assertEquals(8, r.citas().total());
        assertEquals(25.0, r.citas().tasaCancelacion());
        assertEquals(List.of("SIN_TECNICOS_ESPECIALIDAD", "FUERA_DE_COBERTURA"),
                List.copyOf(r.solicitudesEspeciales().porMotivo().keySet()));
        assertEquals(3, r.solicitudesEspeciales().total());
        assertEquals(List.of("CLIENTE", "TECNICO", "ADMINISTRADOR"), List.copyOf(r.usuariosActivos().porRol().keySet()));
        assertEquals(14, r.usuariosActivos().total());
    }

    @Test
    void losIngresosConservanElBigDecimalYElDetallePorMetodo() {
        MetricasResponse.IngresosResponse i = MetricasMapper.aRespuesta(ejemplo()).ingresos();

        assertEquals(new BigDecimal("140000.75"), i.total());
        assertEquals(2, i.cantidadPagos());
        assertEquals(List.of("EFECTIVO", "EN_LINEA"), List.copyOf(i.porMetodo().keySet()));
        assertEquals(new MetricasResponse.IngresoMetodoResponse(new BigDecimal("90000.50"), 1), i.porMetodo().get("EFECTIVO"));
        assertEquals(new MetricasResponse.PorCobrarResponse(1, new BigDecimal("30000.00")), i.porCobrar());
    }

    @Test
    void especialidadesYCalificacionesPorTecnicoSeCopianEnOrden() {
        MetricasResponse r = MetricasMapper.aRespuesta(ejemplo());

        assertEquals(List.of(new MetricasResponse.EspecialidadResponse(1L, "Hornos", 0, 0),
                new MetricasResponse.EspecialidadResponse(2L, "Lavadoras", 5, 3)), r.especialidades());
        assertEquals(4.0, r.calificaciones().promedio());
        assertEquals(3, r.calificaciones().total());
        assertEquals(List.of(new MetricasResponse.CalificacionTecnicoResponse(7L, "Luis Mora", 4.5, 2)),
                r.calificaciones().porTecnico());
    }

    @Test
    void sinCalificacionesElPromedioQuedaNulo() {
        MetricasGenerales base = ejemplo();
        MetricasGenerales sinCalificaciones = new MetricasGenerales(base.rango(), base.citas(), base.solicitudesEspeciales(),
                base.ingresos(), base.especialidades(), new MetricasGenerales.Calificaciones(null, 0, List.of()),
                base.usuariosActivos());

        MetricasResponse.CalificacionesResponse c = MetricasMapper.aRespuesta(sinCalificaciones).calificaciones();

        assertNull(c.promedio());
        assertEquals(0, c.total());
        assertEquals(List.of(), c.porTecnico());
    }
}
