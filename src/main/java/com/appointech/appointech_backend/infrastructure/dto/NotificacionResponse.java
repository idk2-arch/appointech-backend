package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(Long id, String tipo, String titulo, String mensaje, Long citaId, boolean leida,
                                   LocalDateTime creadaEn) {
}
