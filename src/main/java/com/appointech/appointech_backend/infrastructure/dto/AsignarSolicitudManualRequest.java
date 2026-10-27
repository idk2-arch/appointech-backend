package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AsignarSolicitudManualRequest(
        @NotNull(message = "El técnico es obligatorio")
        Long tecnicoId,

        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        LocalDateTime fechaHoraInicio,

        @NotNull(message = "La fecha y hora de fin son obligatorias")
        LocalDateTime fechaHoraFin
) {
}
