package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoCitaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoCita estado
) {
}
