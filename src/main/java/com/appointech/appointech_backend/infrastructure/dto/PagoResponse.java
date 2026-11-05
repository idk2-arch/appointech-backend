package com.appointech.appointech_backend.infrastructure.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        Long citaId,
        BigDecimal montoCotizado,
        BigDecimal montoFinal,
        String metodo,
        String estado,
        String estadoEtiqueta,
        String referenciaExterna,
        LocalDateTime pagadoEn
) {
}
