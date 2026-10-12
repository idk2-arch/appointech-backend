package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record LogResponse(
        Long id,
        String nivel,
        String tipo,
        String origen,
        String usuario,
        String mensaje,
        LocalDateTime creadoEn
) {
}
