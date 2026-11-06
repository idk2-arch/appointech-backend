package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record CalificacionResponse(Long citaId, int puntuacion, String comentario, LocalDateTime fecha) {
}
