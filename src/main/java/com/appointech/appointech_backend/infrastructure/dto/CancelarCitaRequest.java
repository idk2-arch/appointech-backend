package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.Size;

/** Cancelación por el cliente: el motivo es opcional. */
public record CancelarCitaRequest(
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
