package com.appointech.appointech_backend.infrastructure.dto;

import java.math.BigDecimal;

public record CotizacionResponse(
        Long citaId,
        String especialidad,
        BigDecimal montoEstimado,
        String nota
) {
}
