package com.appointech.appointech_backend.infrastructure.repositories;

import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.RolUsuarioEntity;

import java.math.BigDecimal;

/** Proyecciones de las consultas agregadas de las métricas (una fila por grupo; sin entidades completas). */
public final class MetricasFilas {

    private MetricasFilas() {
    }

    public record ConteoEstado(EstadoCitaEntity estado, Long cantidad) {
    }

    public record ConteoMotivo(String motivo, Long cantidad) {
    }

    public record IngresoMetodo(MetodoPago metodo, BigDecimal monto, Long cantidad) {
    }

    /** {@code monto} es null si no hay pagos por cobrar. */
    public record PorCobrar(Long cantidad, BigDecimal monto) {
    }

    public record EspecialidadConteo(Long especialidadId, String nombre, Long totalCitas, Long finalizadas) {
    }

    /** {@code promedio} es null si no hay calificaciones. */
    public record ResumenCalificaciones(Double promedio, Long total) {
    }

    public record CalificacionTecnico(Long tecnicoId, String nombre, String apellido, Double promedio, Long cantidad) {
    }

    public record ConteoRol(RolUsuarioEntity rol, Long cantidad) {
    }
}
