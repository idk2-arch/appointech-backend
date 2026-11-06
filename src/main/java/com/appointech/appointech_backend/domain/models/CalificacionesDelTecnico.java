package com.appointech.appointech_backend.domain.models;

import java.util.List;

/**
 * Calificaciones recibidas por un técnico: promedio (2 decimales, null si no tiene), total de calificaciones
 * y la página pedida (todas si no se pagina), las más recientes primero.
 */
public record CalificacionesDelTecnico(Double promedio, long total, List<CalificacionRecibida> calificaciones) {
}
