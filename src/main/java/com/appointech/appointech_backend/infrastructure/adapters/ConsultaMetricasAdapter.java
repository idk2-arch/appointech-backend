package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetricasGenerales;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.MotivoSolicitudEspecial;
import com.appointech.appointech_backend.domain.models.RangoMetricas;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.ports.out.ConsultaMetricasPort;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.MetricasFilas;
import com.appointech.appointech_backend.infrastructure.repositories.MetricasJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Transactional(readOnly = true)
public class ConsultaMetricasAdapter implements ConsultaMetricasPort {

    private final MetricasJpaRepository metricasJpaRepository;

    public ConsultaMetricasAdapter(MetricasJpaRepository metricasJpaRepository) {
        this.metricasJpaRepository = metricasJpaRepository;
    }

    @Override
    public Map<EstadoCita, Long> citasPorEstado(RangoMetricas rango) {
        Map<EstadoCita, Long> resultado = new EnumMap<>(EstadoCita.class);
        for (MetricasFilas.ConteoEstado fila : metricasJpaRepository.citasPorEstado(inicio(rango), finExclusivo(rango))) {
            resultado.put(EstadoCita.valueOf(fila.estado().name()), fila.cantidad());
        }
        return resultado;
    }

    @Override
    public Map<MotivoSolicitudEspecial, Long> solicitudesEspecialesPendientes() {
        Map<MotivoSolicitudEspecial, Long> resultado = new EnumMap<>(MotivoSolicitudEspecial.class);
        for (MetricasFilas.ConteoMotivo fila : metricasJpaRepository.solicitudesEspecialesPorMotivo(EstadoCitaEntity.PENDIENTE)) {
            resultado.put(MotivoSolicitudEspecial.valueOf(fila.motivo()), fila.cantidad());
        }
        return resultado;
    }

    @Override
    public Map<MetodoPago, MetricasGenerales.IngresoDeMetodo> ingresosPorMetodo(RangoMetricas rango) {
        Map<MetodoPago, MetricasGenerales.IngresoDeMetodo> resultado = new EnumMap<>(MetodoPago.class);
        for (MetricasFilas.IngresoMetodo fila : metricasJpaRepository.ingresosPorMetodo(
                EstadoPago.PAGADO, inicio(rango), finExclusivo(rango))) {
            resultado.put(fila.metodo(), new MetricasGenerales.IngresoDeMetodo(fila.monto(), fila.cantidad()));
        }
        return resultado;
    }

    @Override
    public MetricasGenerales.PagosPorCobrar pagosPorCobrar() {
        MetricasFilas.PorCobrar fila = metricasJpaRepository.pagosPorCobrar(EstadoPago.PENDIENTE);
        return new MetricasGenerales.PagosPorCobrar(fila.cantidad(), fila.monto() == null ? BigDecimal.ZERO : fila.monto());
    }

    @Override
    public List<MetricasGenerales.CitasDeEspecialidad> citasPorEspecialidad(RangoMetricas rango) {
        return metricasJpaRepository.citasPorEspecialidad(EstadoCitaEntity.FINALIZADA, inicio(rango), finExclusivo(rango))
                .stream()
                .map(f -> new MetricasGenerales.CitasDeEspecialidad(f.especialidadId(), f.nombre(), f.totalCitas(),
                        f.finalizadas() == null ? 0 : f.finalizadas()))
                .toList();
    }

    @Override
    public MetricasGenerales.ResumenCalificaciones calificacionesDelRango(RangoMetricas rango) {
        MetricasFilas.ResumenCalificaciones fila = metricasJpaRepository.calificacionesDelRango(inicio(rango), finExclusivo(rango));
        return new MetricasGenerales.ResumenCalificaciones(fila.promedio(), fila.total());
    }

    @Override
    public List<MetricasGenerales.CalificacionesDeTecnico> calificacionesPorTecnico(RangoMetricas rango) {
        return metricasJpaRepository.calificacionesPorTecnico(inicio(rango), finExclusivo(rango)).stream()
                .map(f -> new MetricasGenerales.CalificacionesDeTecnico(f.tecnicoId(), f.nombre() + " " + f.apellido(),
                        f.promedio(), f.cantidad()))
                .toList();
    }

    @Override
    public Map<RolUsuario, Long> usuariosActivosPorRol() {
        Map<RolUsuario, Long> resultado = new EnumMap<>(RolUsuario.class);
        for (MetricasFilas.ConteoRol fila : metricasJpaRepository.usuariosActivosPorRol()) {
            resultado.put(RolUsuario.valueOf(fila.rol().name()), fila.cantidad());
        }
        return resultado;
    }

    private static LocalDateTime inicio(RangoMetricas rango) {
        return rango.desde().atStartOfDay();
    }

    /** El último día entra completo: el límite es el inicio del día siguiente, sin incluirlo. */
    private static LocalDateTime finExclusivo(RangoMetricas rango) {
        return rango.hasta().plusDays(1).atStartOfDay();
    }
}
