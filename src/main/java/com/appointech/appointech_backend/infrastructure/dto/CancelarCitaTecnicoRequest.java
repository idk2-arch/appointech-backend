package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cancelación por el técnico: el motivo es obligatorio. */
public record CancelarCitaTecnicoRequest(
        @NotBlank(message = "El motivo de la cancelación es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
