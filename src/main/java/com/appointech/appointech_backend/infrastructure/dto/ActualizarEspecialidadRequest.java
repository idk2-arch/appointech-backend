package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarEspecialidadRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion
) {
}