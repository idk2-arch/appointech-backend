package com.appointech.appointech_backend.domain.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Métricas generales para el administrador. Los mapas traen siempre todas las claves del enum (las que no tienen
 * datos, en cero). {@code solicitudesEspeciales}, {@code porCobrar} y {@code usuariosActivos} son una foto del estado
 * actual y no dependen del rango.
 */
public record MetricasGenerales(
        RangoMetricas rango,
        Citas citas,
        SolicitudesEspeciales solicitudesEspeciales,
        Ingresos ingresos,
        List<CitasDeEspecialidad> especialidades,
        Calificaciones calificaciones,
        UsuariosActivos usuariosActivos
) {

    /** Citas creadas en el rango; la tasa de cancelación es un porcentaje con 2 decimales (0.0 si no hay citas). */
    public record Citas(long total, Map<EstadoCita, Long> porEstado, double tasaCancelacion) {
    }

    /** Solicitudes especiales que siguen PENDIENTES ahora. */
    public record SolicitudesEspeciales(long total, Map<MotivoSolicitudEspecial, Long> porMotivo) {
    }

    /** Pagos cobrados en el rango por método, y los pagos pendientes con costo final registrado (por cobrar). */
    public record Ingresos(BigDecimal total, long cantidadPagos, Map<MetodoPago, IngresoDeMetodo> porMetodo,
                           PagosPorCobrar porCobrar) {
    }

    public record IngresoDeMetodo(BigDecimal monto, long cantidad) {
    }

    public record PagosPorCobrar(long cantidad, BigDecimal monto) {
    }

    /** Todas las especialidades, también las que no tuvieron citas en el rango. */
    public record CitasDeEspecialidad(Long especialidadId, String nombre, long totalCitas, long finalizadas) {
    }

    /** Calificaciones dadas en el rango; el promedio es null si no hay ninguna. */
    public record Calificaciones(Double promedio, long total, List<CalificacionesDeTecnico> porTecnico) {
    }

    /** Promedio (sin redondear, null si no hay) y cantidad de calificaciones, tal como salen de la base. */
    public record ResumenCalificaciones(Double promedio, long total) {
    }

    /** Solo los técnicos con al menos una calificación. */
    public record CalificacionesDeTecnico(Long tecnicoId, String nombre, Double promedio, long cantidad) {
    }

    public record UsuariosActivos(long total, Map<RolUsuario, Long> porRol) {
    }
}
