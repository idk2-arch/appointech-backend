package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        String telefono
) {
}