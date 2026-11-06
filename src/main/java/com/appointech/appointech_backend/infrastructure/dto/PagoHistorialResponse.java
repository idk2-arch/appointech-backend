package com.appointech.appointech_backend.infrastructure.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una fila del historial de pagos del administrador. Sin referencias de la pasarela ni datos de tarjeta. */
public record PagoHistorialResponse(
        Long citaId,
        Long clienteId,
        String clienteNombre,
        Long tecnicoId,
        String tecnicoNombre,
        String especialidad,
        BigDecimal montoCotizado,
        BigDecimal montoFinal,
        String metodo,
        String estado,
        LocalDateTime pagadoEn,
        String numeroComprobante
) {
}
