package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Reemplazo completo de las reglas. Los rangos (radio 1-100 km, bloque 15-480 min, inicio anterior al fin y al
 * menos un bloque dentro de la jornada) los valida el dominio. Horas en formato HH:mm o HH:mm:ss.
 */
public record ReglasFranjasRequest(
        @NotNull(message = "El radio máximo de cobertura es obligatorio")
        Integer radioMaximoKm,

        @NotNull(message = "La duración del bloque es obligatoria")
        Integer duracionBloqueMinutos,

        @NotNull(message = "La hora de inicio de la jornada es obligatoria")
        LocalTime horaInicioJornada,

        @NotNull(message = "La hora de fin de la jornada es obligatoria")
        LocalTime horaFinJornada
) {
}
