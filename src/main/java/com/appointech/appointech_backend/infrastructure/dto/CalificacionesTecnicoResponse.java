package com.appointech.appointech_backend.infrastructure.dto;

import java.util.List;

/** {@code promedio} es null si el técnico aún no tiene calificaciones; {@code total} cuenta todas, no solo la página. */
public record CalificacionesTecnicoResponse(Double promedio, long total, List<CalificacionRecibidaResponse> calificaciones) {
}
