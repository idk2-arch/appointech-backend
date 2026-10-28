package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosticoRequest(
        @NotBlank(message = "El diagnóstico es obligatorio")
        @Size(max = 2000, message = "El diagnóstico no puede superar 2000 caracteres")
        String diagnostico,

        @Size(max = 1000, message = "Las recomendaciones no pueden superar 1000 caracteres")
        String recomendaciones
) {
}
