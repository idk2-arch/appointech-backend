package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record BloqueResponse(Long id, LocalDateTime inicio, LocalDateTime fin, String motivo) {
}
