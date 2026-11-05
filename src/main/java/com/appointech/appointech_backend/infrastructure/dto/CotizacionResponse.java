package com.appointech.appointech_backend.infrastructure.dto;

import java.math.BigDecimal;

/** Valor estimado; sin montoEstimado cuando la especialidad no tiene valor de referencia (la nota lo explica). */
public record CotizacionResponse(
        Long especialidadId,
        String especialidad,
        BigDecimal montoEstimado,
        String moneda,
        boolean disponible,
        String nota
) {
}
