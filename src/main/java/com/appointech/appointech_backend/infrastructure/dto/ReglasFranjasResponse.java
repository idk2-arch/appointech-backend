package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalTime;

/** {@code advertencia} solo viene (no nula) cuando el cambio invalida franjas que los clientes ya tenían en pantalla. */
public record ReglasFranjasResponse(
        int radioMaximoKm,
        int duracionBloqueMinutos,
        LocalTime horaInicioJornada,
        LocalTime horaFinJornada,
        boolean franjasOfrecidasInvalidadas,
        String advertencia
) {
}
