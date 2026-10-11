package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ConfirmarCitaRequest(
        @NotNull(message = "La solicitud es obligatoria")
        Long solicitudId,

        @NotNull(message = "El técnico es obligatorio")
        Long tecnicoId,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalDateTime inicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalDateTime fin
) {
}
