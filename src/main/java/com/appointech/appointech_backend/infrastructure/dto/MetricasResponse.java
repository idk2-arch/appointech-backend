package com.appointech.appointech_backend.infrastructure.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Métricas generales del administrador, agrupadas. Los mapas traen siempre todas las claves (en cero si no hay datos). */
public record MetricasResponse(
        RangoResponse rango,
        CitasResponse citas,
        SolicitudesEspecialesResponse solicitudesEspeciales,
        IngresosResponse ingresos,
        List<EspecialidadResponse> especialidades,
        CalificacionesResponse calificaciones,
        UsuariosActivosResponse usuariosActivos
) {

    /** Fechas yyyy-MM-dd, ambas inclusivas. */
    public record RangoResponse(String desde, String hasta, long dias) {
    }

    /** Citas creadas en el rango; {@code tasaCancelacion} es un porcentaje con 2 decimales. */
    public record CitasResponse(long total, Map<String, Long> porEstado, double tasaCancelacion) {
    }

    /** Foto de las solicitudes especiales PENDIENTES ahora (no depende del rango). */
    public record SolicitudesEspecialesResponse(long total, Map<String, Long> porMotivo) {
    }

    public record IngresosResponse(BigDecimal total, long cantidadPagos, Map<String, IngresoMetodoResponse> porMetodo,
                                   PorCobrarResponse porCobrar) {
    }

    public record IngresoMetodoResponse(BigDecimal monto, long cantidad) {
    }

    /** Pagos pendientes con costo final registrado, ahora (no depende del rango). */
    public record PorCobrarResponse(long cantidad, BigDecimal monto) {
    }

    public record EspecialidadResponse(Long especialidadId, String nombre, long totalCitas, long finalizadas) {
    }

    /** {@code promedio} es null si no hay calificaciones en el rango. */
    public record CalificacionesResponse(Double promedio, long total, List<CalificacionTecnicoResponse> porTecnico) {
    }

    public record CalificacionTecnicoResponse(Long tecnicoId, String nombre, Double promedio, long cantidad) {
    }

    /** Foto de los usuarios activos ahora (no depende del rango). */
    public record UsuariosActivosResponse(long total, Map<String, Long> porRol) {
    }
}
