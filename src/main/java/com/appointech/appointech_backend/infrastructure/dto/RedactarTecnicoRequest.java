package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedactarTecnicoRequest(
        @NotBlank(message = "La descripción del problema es obligatoria")
        @Size(max = 1000, message = "La descripción del problema no puede superar 1000 caracteres")
        String descripcionProblema,

        @Size(max = 100, message = "La especialidad no puede superar 100 caracteres")
        String especialidad
) {
}
