package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record FranjaDisponibleResponse(
        Long tecnicoId,
        LocalDateTime inicio,
        LocalDateTime fin,
        double distanciaKm
) {
}
