package com.appointech.appointech_backend.infrastructure.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComprobanteResponse(
        String numero,
        LocalDateTime emitidoEn,
        Long citaId,
        String especialidad,
        String cliente,
        String tecnico,
        BigDecimal monto,
        String metodo,
        String referenciaExterna,
        LocalDateTime pagadoEn
) {
}
