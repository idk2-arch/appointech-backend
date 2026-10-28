package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record HistorialEstadoResponse(
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime fecha,
        String actor,
        String motivo
) {
}
