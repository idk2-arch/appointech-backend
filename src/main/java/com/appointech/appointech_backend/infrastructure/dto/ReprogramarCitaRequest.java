package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramarCitaRequest(
        @NotNull(message = "La nueva hora de inicio es obligatoria")
        LocalDateTime inicio
) {
}
