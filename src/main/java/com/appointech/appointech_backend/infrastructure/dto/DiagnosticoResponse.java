package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record DiagnosticoResponse(
        String diagnostico,
        String recomendaciones,
        LocalDateTime fecha
) {
}
