package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CrearBloqueRequest(
        @NotNull(message = "El inicio es obligatorio")
        LocalDateTime inicio,

        @NotNull(message = "El fin es obligatorio")
        LocalDateTime fin,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo
) {
}
