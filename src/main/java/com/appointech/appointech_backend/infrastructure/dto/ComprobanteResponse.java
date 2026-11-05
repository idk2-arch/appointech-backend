package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComprobanteResponse(
        String numero,
        LocalDateTime emitidoEn,
        Long citaId,
        String cliente,
        String tecnico,
        String especialidad,
        BigDecimal monto,
        String moneda,
        MetodoPago metodo,
        String referencia,
        LocalDateTime pagadoEn
) {
}
