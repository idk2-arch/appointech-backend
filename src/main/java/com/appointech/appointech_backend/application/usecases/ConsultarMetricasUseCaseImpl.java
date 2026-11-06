package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.CriteriosMetricas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;
import com.appointech.appointech_backend.domain.models.MetricasGenerales.CalificacionesDeTecnico;
import com.appointech.appointech_backend.domain.models.MetricasGenerales.IngresoDeMetodo;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.RangoMetricas;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.ports.in.ConsultarMetricasUseCase;
import com.appointech.appointech_backend.domain.ports.out.ConsultaMetricasPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso (RF16): reúne las métricas generales del administrador. Las consultas agregadas las hace la base;
 * aquí se valida el rango, se completan con cero las claves sin datos y se calculan los porcentajes y redondeos.
 */
@Service
public class ConsultarMetricasUseCaseImpl implements ConsultarMetricasUseCase {

    private static final int DECIMALES = 2;

    private final ConsultaMetricasPort consultaMetricasPort;
    private final Clock reloj;

    public ConsultarMetricasUseCaseImpl(ConsultaMetricasPort consultaMetricasPort, Clock reloj) {
        this.consultaMetricasPort = consultaMetricasPort;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public MetricasGenerales consultar(CriteriosMetricas criterios) {
        RangoMetricas rango = RangoMetricas.de(criterios.desde(), criterios.hasta(), LocalDate.now(reloj));
        return new MetricasGenerales(
                rango,
                citas(rango),
                solicitudesEspeciales(),
                ingresos(rango),
                consultaMetricasPort.citasPorEspecialidad(rango),
                calificaciones(rango),
                usuariosActivos());
    }

    private MetricasGenerales.Citas citas(RangoMetricas rango) {
        Map<EstadoCita, Long> porEstado = conCeros(EstadoCita.class, consultaMetricasPort.citasPorEstado(rango));
        long total = suma(porEstado);
        double tasa = total == 0 ? 0.0
                : BigDecimal.valueOf(porEstado.get(EstadoCita.CANCELADA) * 100L)
                .divide(BigDecimal.valueOf(total), DECIMALES, RoundingMode.HALF_UP).doubleValue();
        return new MetricasGenerales.Citas(total, porEstado, tasa);
    }

    private MetricasGenerales.SolicitudesEspeciales solicitudesEspeciales() {
        Map<MotivoSolicitudEspecial, Long> porMotivo =
                conCeros(MotivoSolicitudEspecial.class, consultaMetricasPort.solicitudesEspecialesPendientes());
        return new MetricasGenerales.SolicitudesEspeciales(suma(porMotivo), porMotivo);
    }

    private MetricasGenerales.Ingresos ingresos(RangoMetricas rango) {
        Map<MetodoPago, IngresoDeMetodo> porMetodo = new EnumMap<>(MetodoPago.class);
        for (MetodoPago metodo : MetodoPago.values()) {
            porMetodo.put(metodo, new IngresoDeMetodo(BigDecimal.ZERO, 0));
        }
        porMetodo.putAll(consultaMetricasPort.ingresosPorMetodo(rango));
        BigDecimal total = porMetodo.values().stream().map(IngresoDeMetodo::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        long cantidad = porMetodo.values().stream().mapToLong(IngresoDeMetodo::cantidad).sum();
        return new MetricasGenerales.Ingresos(total, cantidad, porMetodo, consultaMetricasPort.pagosPorCobrar());
    }

    private MetricasGenerales.Calificaciones calificaciones(RangoMetricas rango) {
        MetricasGenerales.ResumenCalificaciones resumen = consultaMetricasPort.calificacionesDelRango(rango);
        List<CalificacionesDeTecnico> porTecnico = consultaMetricasPort.calificacionesPorTecnico(rango).stream()
                .map(t -> new CalificacionesDeTecnico(t.tecnicoId(), t.nombre(), redondear(t.promedio()), t.cantidad()))
                .toList();
        Double promedio = resumen.promedio() == null ? null : redondear(resumen.promedio());
        return new MetricasGenerales.Calificaciones(promedio, resumen.total(), porTecnico);
    }

    private MetricasGenerales.UsuariosActivos usuariosActivos() {
        Map<RolUsuario, Long> porRol = conCeros(RolUsuario.class, consultaMetricasPort.usuariosActivosPorRol());
        return new MetricasGenerales.UsuariosActivos(suma(porRol), porRol);
    }

    /** Un mapa con todas las claves del enum: las que la base no devolvió quedan en cero. */
    private static <E extends Enum<E>> Map<E, Long> conCeros(Class<E> tipo, Map<E, Long> conDatos) {
        Map<E, Long> completo = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            completo.put(valor, 0L);
        }
        completo.putAll(conDatos);
        return completo;
    }

    private static long suma(Map<?, Long> valores) {
        return valores.values().stream().mapToLong(Long::longValue).sum();
    }

    private static double redondear(double valor) {
        return redondear(BigDecimal.valueOf(valor));
    }

    private static double redondear(BigDecimal valor) {
        return valor.setScale(DECIMALES, RoundingMode.HALF_UP).doubleValue();
    }
}
